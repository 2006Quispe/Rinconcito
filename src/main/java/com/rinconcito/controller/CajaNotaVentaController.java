package com.rinconcito.controller;

import com.rinconcito.entity.NotaVenta;
import com.rinconcito.service.NotaVentaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/caja/notas-venta")
public class CajaNotaVentaController {
    @org.springframework.beans.factory.annotation.Value("${app.correo.habilitado:false}")
    private boolean correoHabilitado;

    private final NotaVentaService notaVentaService;

    public CajaNotaVentaController(
            NotaVentaService notaVentaService
    ) {
        this.notaVentaService =
                notaVentaService;
    }

    @GetMapping
    public String listarNotasVenta(
            @RequestParam(
                    name = "buscar",
                    required = false
            ) String buscar,
            Model model
    ) {
        List<NotaVenta> notas =
                notaVentaService.listarParaCaja();

        if (buscar != null && !buscar.isBlank()) {
            String texto =
                    buscar.trim()
                            .toLowerCase(Locale.ROOT);

            notas = notas.stream()
                    .filter(nota ->
                            coincideBusqueda(
                                    nota,
                                    texto
                            )
                    )
                    .toList();
        }

        Map<Integer, String> numerosNotas =
                new LinkedHashMap<>();

        for (NotaVenta nota : notas) {
            numerosNotas.put(
                    nota.getIdNota(),
                    notaVentaService.formarNumero(nota)
            );
        }

        int totalNotas = notas.size();

        int notasEmitidas = (int) notas.stream()
                .filter(nota ->
                        nota.getEstado()
                                == NotaVenta.EstadoNotaVenta.EMITIDA
                )
                .count();

        int notasAnuladas = (int) notas.stream()
                .filter(nota ->
                        nota.getEstado()
                                == NotaVenta.EstadoNotaVenta.ANULADA
                )
                .count();

        int totalVendido = notas.stream()
                .filter(nota ->
                        nota.getEstado()
                                == NotaVenta.EstadoNotaVenta.EMITIDA
                )
                .map(NotaVenta::getTotal)
                .filter(total -> total != null)
                .mapToInt(Integer::intValue)
                .sum();

        model.addAttribute(
                "titulo",
                "Notas de venta"
        );

        model.addAttribute(
                "notas",
                notas
        );

        model.addAttribute(
                "numerosNotas",
                numerosNotas
        );

        model.addAttribute(
                "buscar",
                buscar
        );

        model.addAttribute(
                "totalNotas",
                totalNotas
        );

        model.addAttribute(
                "notasEmitidas",
                notasEmitidas
        );

        model.addAttribute(
                "notasAnuladas",
                notasAnuladas
        );

        model.addAttribute(
                "totalVendido",
                totalVendido
        );

        model.addAttribute("correoHabilitado", correoHabilitado);
        return "paginas/caja/notas-venta";
    }

    @GetMapping("/{idNota}/ticket")
    public String ticketPresencial(
            @org.springframework.web.bind.annotation.PathVariable Integer idNota,
            Model model
    ) {
        NotaVenta nota;
        try {
            nota = notaVentaService.obtenerParaPdf(idNota);
        } catch (IllegalStateException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Ticket no encontrado", e);
        }
        var pedido = nota.getPago().getPedido();
        if (pedido == null || pedido.getTipoPedido() != com.rinconcito.entity.Pedido.TipoPedido.PRESENCIAL) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Ticket presencial no encontrado");
        }
        var total = java.math.BigDecimal.valueOf(nota.getTotal());
        var base = total.divide(new java.math.BigDecimal("1.18"), 2, java.math.RoundingMode.HALF_UP);
        model.addAttribute("nota", nota);
        model.addAttribute("codigo", notaVentaService.formarNumero(nota));
        model.addAttribute("detalles", notaVentaService.obtenerDetalles(nota));
        model.addAttribute("igv", total.subtract(base));
        return "paginas/caja/ticket-presencial";
    }

    private boolean coincideBusqueda(
            NotaVenta nota,
            String texto
    ) {
        String numeroNota =
                notaVentaService
                        .formarNumero(nota)
                        .toLowerCase(Locale.ROOT);

        String cliente =
                nota.getClienteNombre() != null
                        ? nota.getClienteNombre()
                        .toLowerCase(Locale.ROOT)
                        : "";

        String documento =
                nota.getClienteDocumento() != null
                        ? nota.getClienteDocumento()
                        .toLowerCase(Locale.ROOT)
                        : "";

        String codigoPedido = "";

        if (
                nota.getPago() != null
                        && nota.getPago().getPedido() != null
                        && nota.getPago()
                        .getPedido()
                        .getCodigoPedido() != null
        ) {
            codigoPedido =
                    nota.getPago()
                            .getPedido()
                            .getCodigoPedido()
                            .toLowerCase(Locale.ROOT);
        }

        return numeroNota.contains(texto)
                || cliente.contains(texto)
                || documento.contains(texto)
                || codigoPedido.contains(texto);
    }
}