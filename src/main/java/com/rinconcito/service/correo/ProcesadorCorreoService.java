package com.rinconcito.service.correo;

import com.rinconcito.service.NotaVentaPdfService;
import com.rinconcito.service.NotaVentaService;
import com.rinconcito.entity.NotaVenta;
import com.rinconcito.entity.Pago;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(name="app.correo.habilitado", havingValue="true")
public class ProcesadorCorreoService {
    private static final Logger log = LoggerFactory.getLogger(ProcesadorCorreoService.class);
    private final JdbcTemplate jdbc;
    private final JavaMailSender sender;
    private final NotaVentaPdfService pdf;
    private final NotaVentaService notas;
    private final String remitente;
    public ProcesadorCorreoService(JdbcTemplate jdbc, JavaMailSender sender, NotaVentaPdfService pdf,
                                   NotaVentaService notas, @Value("${app.correo.remitente}") String remitente) {
        this.jdbc=jdbc; this.sender=sender; this.pdf=pdf; this.notas=notas; this.remitente=remitente;
    }

    // Otra conexion solo puede ver las filas una vez confirmada la transaccion de negocio.
    @Scheduled(fixedDelayString="${app.correo.intervalo-ms:5000}", initialDelay=5000)
    public synchronized void procesar() {
        try {
            var pendientes = jdbc.queryForList("""
                    SELECT * FROM correo_salida WHERE estado IN ('PENDIENTE','ERROR','ENVIANDO')
                    AND intentos < 3 AND proximo_intento <= CURRENT_TIMESTAMP ORDER BY id LIMIT 10
                    """);
            for (var fila : pendientes) {
                long id=((Number)fila.get("id")).longValue();
                int tomado=jdbc.update("""
                        UPDATE correo_salida SET estado='ENVIANDO',intentos=intentos+1,
                        proximo_intento=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 5 MINUTE)
                        WHERE id=? AND estado IN ('PENDIENTE','ERROR','ENVIANDO')
                        AND intentos < 3 AND proximo_intento <= CURRENT_TIMESTAMP
                        """, id);
                if (tomado == 0) continue;
                try {
                    var mensaje=sender.createMimeMessage();
                    var helper=new MimeMessageHelper(mensaje,true,StandardCharsets.UTF_8.name());
                    helper.setFrom(remitente);
                    helper.setTo((String)fila.get("destinatario"));
                    helper.setSubject((String)fila.get("asunto"));
                    helper.setText((String)fila.get("cuerpo"),false);
                    if ("NOTA".equals(fila.get("tipo"))) {
                        int idNota=((Number)fila.get("id_referencia")).intValue();
                        var nota=notas.obtenerParaPdf(idNota);
                        if (nota.getEstado()!=NotaVenta.EstadoNotaVenta.EMITIDA
                                || nota.getPago().getEstado()!=Pago.EstadoPago.APROBADO) {
                            jdbc.update("UPDATE correo_salida SET estado='OMITIDO',error=? WHERE id=?",
                                    "La nota ya no esta emitida o su pago no esta aprobado.",id);
                            continue;
                        }
                        helper.addAttachment("Nota-"+nota.getSerie()+"-"+String.format("%06d",nota.getNumero())+".pdf",
                                new ByteArrayResource(pdf.generar(idNota)),"application/pdf");
                    }
                    sender.send(mensaje);
                    jdbc.update("UPDATE correo_salida SET estado='ENVIADO',error=NULL,enviado=CURRENT_TIMESTAMP WHERE id=?",id);
                    log.info("Correo de la cola {} aceptado por el proveedor de correo",id);
                } catch (Exception error) {
                    jdbc.update("""
                            UPDATE correo_salida SET estado='ERROR',error=?,
                            proximo_intento=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1 MINUTE) WHERE id=?
                            ""","No se pudo generar o entregar el correo. Comprueba la autorizacion del proveedor y el documento.",id);
                    log.warn("Fallo el correo de la cola {} ({})",id,error.getClass().getSimpleName());
                }
            }
        } catch (Exception error) {
            log.error("No se pudo consultar la cola de correo ({})",error.getClass().getSimpleName());
        }
    }
}
