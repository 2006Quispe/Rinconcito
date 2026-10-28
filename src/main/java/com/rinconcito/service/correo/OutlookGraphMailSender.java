package com.rinconcito.service.correo;

import jakarta.mail.internet.MimeMessage;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/** Reutiliza el mensaje y sus adjuntos, enviandolos por Microsoft Graph. */
public class OutlookGraphMailSender extends JavaMailSenderImpl {
    static final String SEND_ENDPOINT = "https://graph.microsoft.com/v1.0/me/sendMail";
    private final OutlookOAuthTokenService tokens;
    private final RestClient cliente;

    public OutlookGraphMailSender(OutlookOAuthTokenService tokens, RestClient cliente) {
        this.tokens = tokens;
        this.cliente = cliente;
        setDefaultEncoding("UTF-8");
    }

    @Override
    protected void doSend(MimeMessage[] mensajes, Object[] originales) throws MailException {
        for (MimeMessage mensaje : mensajes) {
            try {
                mensaje.saveChanges();
                var salida = new ByteArrayOutputStream();
                mensaje.writeTo(salida);
                byte[] contenido = Base64.getEncoder().encode(salida.toByteArray());
                String token = tokens.obtenerToken();
                var respuesta = cliente.post().uri(SEND_ENDPOINT)
                        .headers(headers -> headers.setBearerAuth(token))
                        .contentType(MediaType.TEXT_PLAIN).contentLength(contenido.length)
                        // No utilizar conversores que puedan imprimir datos del correo en DEBUG.
                        .body(stream -> stream.write(contenido)).retrieve().toBodilessEntity();
                if (respuesta.getStatusCode().value() != 202) {
                    throw new MailSendException("Microsoft Graph no confirmo la aceptacion del correo.");
                }
            } catch (RestClientResponseException error) {
                if (error.getStatusCode().value() == 401) tokens.invalidar();
                // No incluir cuerpos, cabeceras ni tokens de la respuesta en logs.
                throw new MailSendException("Microsoft Graph rechazo el envio (HTTP "
                        + error.getStatusCode().value() + "). Revisa la autorizacion Mail.Send.");
            } catch (MailException error) {
                throw error;
            } catch (Exception error) {
                throw new MailSendException("No se pudo preparar o entregar el correo a Microsoft Graph.");
            }
        }
    }
}
