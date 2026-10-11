package com.rinconcito.controller;

import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.UsuarioRepository;
import com.rinconcito.service.EvidenciaStorageService;
import com.rinconcito.service.PagoPedidoService;
import com.rinconcito.service.PagoReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.util.List;

@Controller
@RequestMapping("/caja")
public class CajaPagoReservaController {

    @Autowired
    private PagoReservaService pagoReservaService;

    @Autowired
    private PagoPedidoService pagoPedidoService;

    @Autowired
    private EvidenciaStorageService evidenciaStorageService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/reservas-pendientes")
    public String listarPendientes(Model model) {
        model.addAttribute("titulo", "Reservas · Pagos por validar");
        List<Pago> pendientes = pagoReservaService.listarPendientesValidacionReservas();
        // Inicializar asociaciones lazy
        pendientes.forEach(p -> {
            if (p.getReserva() != null) {
                p.getReserva().getCodigoReserva();
                if (p.getReserva().getEvento() != null) {
                    p.getReserva().getEvento().getTitulo();
                }
                if (p.getReserva().getCliente() != null) {
                    p.getReserva().getCliente().getNombres();
                    p.getReserva().getCliente().getApellidos();
                }
                if (p.getReserva().getPisoSolicitado() != null) {
                    p.getReserva().getPisoSolicitado().getNombre();
                }
            }
        });
        model.addAttribute("pendientes", pendientes);
        return "paginas/caja/reservas-pendientes";
    }

    @PostMapping("/reservas-pendientes/{idPago}/aprobar")
    public String aprobar(@PathVariable Integer idPago,
                          Authentication authentication,
                          RedirectAttributes ra) {
        try {
            Usuario validador = usuarioActual(authentication);
            pagoReservaService.aprobar(idPago, validador);
            ra.addFlashAttribute("exito", "Pago aprobado y reserva confirmada con mesa asignada.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/caja/reservas-pendientes";
    }

    @PostMapping("/reservas-pendientes/{idPago}/rechazar")
    public String rechazar(@PathVariable Integer idPago,
                           @RequestParam("motivo") String motivo,
                           Authentication authentication,
                           RedirectAttributes ra) {
        try {
            Usuario validador = usuarioActual(authentication);
            pagoReservaService.rechazar(idPago, validador, motivo);
            ra.addFlashAttribute("exito", "Pago rechazado. La reserva volvió a pendiente de pago.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/caja/reservas-pendientes";
    }

    /**
     * Sirve capturas privadas solo para CAJA / ADMINISTRADOR (ruta bajo /caja/**).
     */
    @GetMapping("/evidencias/{idPago}")
    public ResponseEntity<Resource> verEvidencia(@PathVariable Integer idPago) {
        try {
            Pago pago;
            try {
                pago = pagoReservaService.obtenerPagoReserva(idPago);
            } catch (IllegalStateException ex) {
                pago = pagoPedidoService.obtenerPagoPedido(idPago);
            }
            Path archivo = evidenciaStorageService.resolverArchivo(pago.getEvidenciaPago());
            Resource resource = new UrlResource(archivo.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = evidenciaStorageService.contentTypeDe(archivo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + archivo.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Usuario usuarioActual(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Debes iniciar sesión.");
        }
        return usuarioRepository.findByCorreo(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario de sesión no encontrado."));
    }
}
