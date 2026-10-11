package com.rinconcito.service;

import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pago.EstadoPago;
import com.rinconcito.entity.Pago.MetodoPago;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.PagoRepository;
import com.rinconcito.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PagoReservaService {

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final MesaService mesaService;
    private final EvidenciaStorageService evidenciaStorageService;
    private final NotaVentaService notaVentaService;

    public PagoReservaService(
            ReservaRepository reservaRepository,
            PagoRepository pagoRepository,
            MesaService mesaService,
            EvidenciaStorageService evidenciaStorageService,
            NotaVentaService notaVentaService
    ) {
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.mesaService = mesaService;
        this.evidenciaStorageService =
                evidenciaStorageService;
        this.notaVentaService = notaVentaService;
    }

    @Transactional(readOnly = true)
    public Optional<Reserva> buscarPorCodigo(
            String codigo
    ) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }

        return reservaRepository.findByCodigoReserva(
                codigo.trim()
        );
    }

    @Transactional(readOnly = true)
    public Optional<Pago> ultimoPagoDe(
            Reserva reserva
    ) {
        return pagoRepository
                .findFirstByReservaOrderByFechaPagoDesc(
                        reserva
                );
    }

    @Transactional(readOnly = true)
    public List<Pago> listarPendientesValidacionReservas() {
        return pagoRepository
                .findByEstadoAndReservaIsNotNullOrderByFechaPagoAsc(
                        EstadoPago.PENDIENTE_VALIDACION
                );
    }

    @Transactional(readOnly = true)
    public Pago obtenerPagoReserva(
            Integer idPago
    ) {
        Pago pago = pagoRepository
                .findById(idPago)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Pago no encontrado."
                        )
                );

        if (pago.getReserva() == null) {
            throw new IllegalStateException(
                    "Este pago no corresponde a una reserva."
            );
        }

        Reserva reserva = pago.getReserva();

        reserva.getCodigoReserva();

        if (reserva.getEvento() != null) {
            reserva.getEvento().getTitulo();
        }

        if (reserva.getCliente() != null) {
            reserva.getCliente().getNombres();
            reserva.getCliente().getApellidos();
            reserva.getCliente().getDni();
        }

        if (reserva.getPisoSolicitado() != null) {
            reserva.getPisoSolicitado().getNombre();
        }

        if (reserva.getMesa() != null) {
            reserva.getMesa().getNumero();
        }

        return pago;
    }

    @Transactional
    public Pago registrarEvidencia(
            String codigoReserva,
            MetodoPago metodo,
            String numeroOperacion,
            MultipartFile captura
    ) {
        if (
                metodo != MetodoPago.YAPE
                        && metodo != MetodoPago.PLIN
                        && metodo != MetodoPago.TRANSFERENCIA
        ) {
            throw new IllegalStateException(
                    "Solo se acepta Yape, Plin o transferencia."
            );
        }

        if (
                codigoReserva == null
                        || codigoReserva.isBlank()
        ) {
            throw new IllegalStateException(
                    "El código de reserva es obligatorio."
            );
        }

        Reserva reserva = reservaRepository
                .findByCodigoReserva(
                        codigoReserva.trim()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encontró la reserva con ese código."
                        )
                );

        if (
                reserva.getEstado()
                        != EstadoReserva.PENDIENTE_PAGO
        ) {
            throw new IllegalStateException(
                    "Esta reserva no admite envío de evidencia "
                            + "en su estado actual ("
                            + reserva.getEstado()
                            + ")."
            );
        }

        if (
                !reserva.isRequierePago()
                        || reserva.getMontoReserva() == null
                        || reserva.getMontoReserva() <= 0
        ) {
            throw new IllegalStateException(
                    "Esta reserva no requiere pago."
            );
        }

        Optional<Pago> pagoPendiente =
                pagoRepository
                        .findFirstByReservaAndEstadoOrderByFechaPagoDesc(
                                reserva,
                                EstadoPago.PENDIENTE_VALIDACION
                        );

        if (pagoPendiente.isPresent()) {
            throw new IllegalStateException(
                    "Ya hay un pago en revisión para esta reserva. "
                            + "Espera la validación."
            );
        }

        String rutaEvidencia =
                evidenciaStorageService.guardar(
                        captura,
                        reserva.getCodigoReserva()
                );

        Pago pago = new Pago();

        pago.setReserva(reserva);
        pago.setPedido(null);
        pago.setMetodoPago(metodo);
        pago.setMonto(reserva.getMontoReserva());
        pago.setNumeroOperacion(
                blankToNull(numeroOperacion)
        );
        pago.setEvidenciaPago(rutaEvidencia);
        pago.setEstado(
                EstadoPago.PENDIENTE_VALIDACION
        );

        pago = pagoRepository.save(pago);

        reserva.setEstado(
                EstadoReserva.PENDIENTE_VALIDACION
        );
        reserva.setMesa(null);

        reservaRepository.save(reserva);

        return pago;
    }

    @Transactional
    public Reserva aprobar(
            Integer idPago,
            Usuario validador
    ) {
        if (validador == null) {
            throw new IllegalStateException(
                    "El usuario de Caja es obligatorio."
            );
        }

        Pago pago = obtenerPagoReserva(idPago);

        if (
                pago.getEstado()
                        != EstadoPago.PENDIENTE_VALIDACION
        ) {
            throw new IllegalStateException(
                    "Este pago ya fue procesado."
            );
        }

        Reserva reserva = pago.getReserva();

        if (
                reserva.getEstado()
                        != EstadoReserva.PENDIENTE_VALIDACION
        ) {
            throw new IllegalStateException(
                    "La reserva no está pendiente de validación."
            );
        }

        int montoEsperado =
                recalcularMonto(reserva);

        if (
                pago.getMonto() == null
                        || !pago.getMonto().equals(montoEsperado)
                        || !pago.getMonto().equals(
                                reserva.getMontoReserva()
                        )
        ) {
            throw new IllegalStateException(
                    "El monto del pago no coincide con el "
                            + "de la reserva (esperado S/ "
                            + montoEsperado
                            + "). No se puede aprobar."
            );
        }

        Optional<Mesa> mesaDisponible =
                mesaService.buscarMesaDisponible(
                        reserva.getPisoSolicitado(),
                        reserva.getFechaReserva(),
                        reserva.getHoraInicio(),
                        reserva.getHoraFin(),
                        reserva.getNumeroPersonas()
                );

        if (mesaDisponible.isEmpty()) {
            throw new IllegalStateException(
                    "No hay mesa disponible en "
                            + reserva.getPisoSolicitado().getNombre()
                            + " para ese horario y número de personas. "
                            + "Coordina con el cliente otro piso "
                            + "u horario."
            );
        }

        reserva.setMesa(
                mesaDisponible.get()
        );
        reserva.setEstado(
                EstadoReserva.CONFIRMADA
        );
        reserva.setMontoReserva(
                montoEsperado
        );

        reservaRepository.save(reserva);

        pago.setEstado(
                EstadoPago.APROBADO
        );
        pago.setUsuarioValidador(
                validador
        );
        pago.setFechaValidacion(
                LocalDateTime.now()
        );

        pago = pagoRepository.save(pago);

        /*
         * Genera automáticamente la nota de venta
         * correspondiente a la reserva pagada.
         */
        notaVentaService.emitir(
                pago,
                validador
        );

        reserva.getMesa().getNumero();

        return reserva;
    }

    @Transactional
    public void rechazar(
            Integer idPago,
            Usuario validador,
            String motivo
    ) {
        if (
                motivo == null
                        || motivo.isBlank()
        ) {
            throw new IllegalStateException(
                    "Debes indicar el motivo del rechazo."
            );
        }

        if (motivo.trim().length() > 300) {
            throw new IllegalStateException(
                    "El motivo no puede superar 300 caracteres."
            );
        }

        Pago pago = obtenerPagoReserva(idPago);

        if (
                pago.getEstado()
                        != EstadoPago.PENDIENTE_VALIDACION
        ) {
            throw new IllegalStateException(
                    "Este pago ya fue procesado."
            );
        }

        Reserva reserva = pago.getReserva();

        pago.setEstado(
                EstadoPago.RECHAZADO
        );
        pago.setObservacionValidacion(
                motivo.trim()
        );
        pago.setUsuarioValidador(
                validador
        );
        pago.setFechaValidacion(
                LocalDateTime.now()
        );

        pagoRepository.save(pago);

        reserva.setEstado(
                EstadoReserva.PENDIENTE_PAGO
        );
        reserva.setMesa(null);

        reservaRepository.save(reserva);
    }

    private int recalcularMonto(
            Reserva reserva
    ) {
        if (
                reserva.getEvento() != null
                        && reserva.getEvento().isRequierePago()
                        && reserva.getEvento().getPrecioEntrada() != null
                        && reserva.getEvento().getPrecioEntrada() > 0
        ) {
            return reserva.getEvento().getPrecioEntrada()
                    * reserva.getNumeroPersonas();
        }

        return reserva.getMontoReserva() != null
                ? reserva.getMontoReserva()
                : 0;
    }

    private String blankToNull(
            String valor
    ) {
        if (
                valor == null
                        || valor.isBlank()
        ) {
            return null;
        }

        return valor.trim();
    }
}