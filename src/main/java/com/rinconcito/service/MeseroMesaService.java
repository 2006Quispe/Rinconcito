package com.rinconcito.service;

import com.rinconcito.dto.MeseroMesaDTO;
import com.rinconcito.dto.MeseroPisoDTO;
import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Mesa.EstadoMesa;
import com.rinconcito.entity.Piso;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.repository.MesaRepository;
import com.rinconcito.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MeseroMesaService {

    private static final ZoneId ZONA_PERU =
            ZoneId.of("America/Lima");

    private static final int MINUTOS_ANTES_RESERVA = 30;

    private static final Set<EstadoReserva>
            ESTADOS_OPERATIVOS = EnumSet.of(
                    EstadoReserva.CONFIRMADA,
                    EstadoReserva.CLIENTE_LLEGO,
                    EstadoReserva.EN_ATENCION
            );

    private final MesaRepository mesaRepository;
    private final ReservaRepository reservaRepository;

    public MeseroMesaService(
            MesaRepository mesaRepository,
            ReservaRepository reservaRepository
    ) {
        this.mesaRepository = mesaRepository;
        this.reservaRepository = reservaRepository;
    }

/*Construye el panel de mesas del mesero.Las mesas se agrupan por piso y se relacionan con las reservas operativas del día.*/
    @Transactional(readOnly = true)
    public List<MeseroPisoDTO> listarPisosConMesasHoy() {

        LocalDate hoy = LocalDate.now(ZONA_PERU);
        LocalTime ahora = LocalTime.now(ZONA_PERU);

        List<Mesa> mesas =
                mesaRepository
                        .findAllByOrderByPisoIdPisoAscNumeroAsc();

        List<Reserva> reservas =
                reservaRepository
                        .listarReservasOperativasDelDia(
                                hoy,
                                new ArrayList<>(ESTADOS_OPERATIVOS)
                        );

        Map<Integer, List<Reserva>> reservasPorMesa =
                agruparReservasPorMesa(reservas);

        Map<Integer, MeseroPisoDTO> pisosAgrupados =
                new LinkedHashMap<>();

        for (Mesa mesa : mesas) {

            Piso piso = mesa.getPiso();

            MeseroPisoDTO pisoDTO =
                    pisosAgrupados.computeIfAbsent(
                            piso.getIdPiso(),
                            id -> new MeseroPisoDTO(piso)
                    );

            Reserva reserva =
                    seleccionarReservaVisible(
                            reservasPorMesa.get(
                                    mesa.getIdMesa()
                            ),
                            ahora
                    );

            String estadoVisual =
                    determinarEstadoVisual(
                            mesa,
                            reserva,
                            ahora
                    );

            pisoDTO.agregarMesa(
                    new MeseroMesaDTO(
                            mesa,
                            reserva,
                            estadoVisual
                    )
            );
        }

        return new ArrayList<>(
                pisosAgrupados.values()
        );
    }

    /*
     * Marca manualmente una mesa como ocupada.
     * No permite ocupar mesas inactivas ni mesas
     * bloqueadas actualmente por una reserva.
     */
    @Transactional
    public void ocuparMesa(Integer idMesa) {

        Mesa mesa = buscarMesa(idMesa);

        if (mesa.getEstado() == EstadoMesa.INACTIVA) {
            throw new IllegalStateException(
                    "No se puede ocupar una mesa inactiva."
            );
        }

        if (mesa.getEstado() == EstadoMesa.OCUPADA) {
            throw new IllegalStateException(
                    "La mesa ya se encuentra ocupada."
            );
        }

        Reserva reservaActual =
                buscarReservaActualDeMesa(mesa);

        if (reservaActual != null) {
            throw new IllegalStateException(
                    "La mesa está vinculada con la reserva "
                            + reservaActual.getCodigoReserva()
                            + " y no puede ocuparse manualmente."
            );
        }

        mesa.setEstado(EstadoMesa.OCUPADA);
        mesaRepository.save(mesa);
    }

    /*
     * Libera una mesa ocupada manualmente.
     * Si existe una reserva en atención, primero
     * debe finalizarse desde el panel.
     */
    @Transactional
    public void liberarMesa(Integer idMesa) {

        Mesa mesa = buscarMesa(idMesa);

        if (mesa.getEstado() == EstadoMesa.INACTIVA) {
            throw new IllegalStateException(
                    "No se puede liberar una mesa inactiva."
            );
        }

        Reserva reservaEnAtencion =
                buscarReservaEnAtencionDeMesa(mesa);

        if (reservaEnAtencion != null) {
            throw new IllegalStateException(
                    "La mesa pertenece a la reserva "
                            + reservaEnAtencion.getCodigoReserva()
                            + ". Debes finalizar primero la atención."
            );
        }

        mesa.setEstado(EstadoMesa.DISPONIBLE);
        mesaRepository.save(mesa);
    }

    /*
     * Registra que el cliente de una reserva
     * confirmada llegó al restaurante.
     */
    @Transactional
    public void registrarLlegada(Integer idReserva) {

        Reserva reserva = buscarReserva(idReserva);

        validarReservaDeHoy(reserva);

        if (reserva.getEstado()
                != EstadoReserva.CONFIRMADA) {

            throw new IllegalStateException(
                    "Solo se puede registrar la llegada "
                            + "de una reserva confirmada."
            );
        }

        Mesa mesa = obtenerMesaAsignada(reserva);

        if (mesa.getEstado() == EstadoMesa.INACTIVA) {
            throw new IllegalStateException(
                    "La mesa asignada está inactiva."
            );
        }

        if (mesa.getEstado() == EstadoMesa.OCUPADA) {
            throw new IllegalStateException(
                    "La mesa asignada ya está ocupada."
            );
        }

        reserva.setEstado(
                EstadoReserva.CLIENTE_LLEGO
        );

        mesa.setEstado(
                EstadoMesa.OCUPADA
        );

        reservaRepository.save(reserva);
        mesaRepository.save(mesa);
    }

    /*
     * Cambia la reserva desde CLIENTE_LLEGO
     * hacia EN_ATENCION.
     */
    @Transactional
    public void iniciarAtencion(Integer idReserva) {

        Reserva reserva = buscarReserva(idReserva);

        validarReservaDeHoy(reserva);

        if (reserva.getEstado()
                != EstadoReserva.CLIENTE_LLEGO) {

            throw new IllegalStateException(
                    "Primero debes registrar la llegada "
                            + "del cliente."
            );
        }

        Mesa mesa = obtenerMesaAsignada(reserva);

        reserva.setEstado(
                EstadoReserva.EN_ATENCION
        );

        mesa.setEstado(
                EstadoMesa.OCUPADA
        );

        reservaRepository.save(reserva);
        mesaRepository.save(mesa);
    }

    /*
     * Finaliza la reserva y vuelve a dejar
     * disponible la mesa.
     */
    @Transactional
    public void finalizarReserva(Integer idReserva) {

        Reserva reserva = buscarReserva(idReserva);

        validarReservaDeHoy(reserva);

        if (
                reserva.getEstado()
                        != EstadoReserva.CLIENTE_LLEGO
                        && reserva.getEstado()
                        != EstadoReserva.EN_ATENCION
        ) {
            throw new IllegalStateException(
                    "La reserva no se encuentra en atención."
            );
        }

        Mesa mesa = obtenerMesaAsignada(reserva);

        reserva.setEstado(
                EstadoReserva.FINALIZADA
        );

        mesa.setEstado(
                EstadoMesa.DISPONIBLE
        );

        reservaRepository.save(reserva);
        mesaRepository.save(mesa);
    }

    private Map<Integer, List<Reserva>>
    agruparReservasPorMesa(
            List<Reserva> reservas
    ) {
        Map<Integer, List<Reserva>> resultado =
                new LinkedHashMap<>();

        if (reservas == null) {
            return resultado;
        }

        for (Reserva reserva : reservas) {

            if (
                    reserva.getMesa() == null
                            || reserva.getMesa()
                            .getIdMesa() == null
            ) {
                continue;
            }

            resultado.computeIfAbsent(
                    reserva.getMesa().getIdMesa(),
                    id -> new ArrayList<>()
            ).add(reserva);
        }

        return resultado;
    }

    /*
     * Selecciona la reserva más importante para mostrar:
     *
     * 1. Reserva en atención.
     * 2. Cliente que ya llegó.
     * 3. Próxima reserva confirmada.
     * 4. Reserva confirmada anterior.
     */
    private Reserva seleccionarReservaVisible(
            List<Reserva> reservas,
            LocalTime ahora
    ) {
        if (reservas == null || reservas.isEmpty()) {
            return null;
        }

        Comparator<Reserva> comparador =
                Comparator
                        .comparingInt(
                                (Reserva reserva) ->
                                        prioridadReserva(
                                                reserva,
                                                ahora
                                        )
                        )
                        .thenComparing(
                                (Reserva reserva) ->
                                        reserva.getHoraInicio(),
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        );

        return reservas.stream()
                .sorted(comparador)
                .findFirst()
                .orElse(null);
    }
    private int prioridadReserva(
            Reserva reserva,
            LocalTime ahora
    ) {
        if (
                reserva.getEstado()
                        == EstadoReserva.EN_ATENCION
        ) {
            return 1;
        }

        if (
                reserva.getEstado()
                        == EstadoReserva.CLIENTE_LLEGO
        ) {
            return 2;
        }

        if (
                reserva.getEstado()
                        == EstadoReserva.CONFIRMADA
                        && reserva.getHoraFin() != null
                        && reserva.getHoraFin().isAfter(ahora)
        ) {
            return 3;
        }

        return 4;
    }

    private String determinarEstadoVisual(
            Mesa mesa,
            Reserva reserva,
            LocalTime ahora
    ) {
        if (mesa.getEstado() == EstadoMesa.INACTIVA) {
            return EstadoMesa.INACTIVA.name();
        }

        if (reserva != null) {

            if (
                    reserva.getEstado()
                            == EstadoReserva.CLIENTE_LLEGO
                            || reserva.getEstado()
                            == EstadoReserva.EN_ATENCION
            ) {
                return EstadoMesa.OCUPADA.name();
            }

            if (
                    reserva.getEstado()
                            == EstadoReserva.CONFIRMADA
                            && reservaBloqueaAhora(
                                    reserva,
                                    ahora
                            )
            ) {
                return EstadoMesa.RESERVADA.name();
            }
        }

        return mesa.getEstado().name();
    }

    /*
     * Una reserva confirmada bloquea la mesa
     * desde 30 minutos antes de su inicio
     * hasta la hora de finalización.
     */
    private boolean reservaBloqueaAhora(
            Reserva reserva,
            LocalTime ahora
    ) {
        if (
                reserva.getHoraInicio() == null
                        || reserva.getHoraFin() == null
        ) {
            return false;
        }

        LocalTime inicioBloqueo =
                reserva.getHoraInicio()
                        .minusMinutes(
                                MINUTOS_ANTES_RESERVA
                        );

        return !ahora.isBefore(inicioBloqueo)
                && ahora.isBefore(
                        reserva.getHoraFin()
                );
    }

    private Reserva buscarReservaActualDeMesa(
            Mesa mesa
    ) {
        LocalDate hoy = LocalDate.now(ZONA_PERU);
        LocalTime ahora = LocalTime.now(ZONA_PERU);

        List<Reserva> reservas =
                reservaRepository
                        .listarReservasOperativasDelDia(
                                hoy,
                                new ArrayList<>(
                                        ESTADOS_OPERATIVOS
                                )
                        );

        return reservas.stream()
                .filter(reserva ->
                        reserva.getMesa() != null
                                && reserva.getMesa()
                                .getIdMesa()
                                .equals(
                                        mesa.getIdMesa()
                                )
                )
                .filter(reserva ->
                        reserva.getEstado()
                                == EstadoReserva.CLIENTE_LLEGO
                                || reserva.getEstado()
                                == EstadoReserva.EN_ATENCION
                                || (
                                reserva.getEstado()
                                        == EstadoReserva.CONFIRMADA
                                        && reservaBloqueaAhora(
                                                reserva,
                                                ahora
                                        )
                        )
                )
                .findFirst()
                .orElse(null);
    }

    private Reserva buscarReservaEnAtencionDeMesa(
            Mesa mesa
    ) {
        LocalDate hoy = LocalDate.now(ZONA_PERU);

        List<Reserva> reservas =
                reservaRepository
                        .listarReservasOperativasDelDia(
                                hoy,
                                new ArrayList<>(
                                        ESTADOS_OPERATIVOS
                                )
                        );

        return reservas.stream()
                .filter(reserva ->
                        reserva.getMesa() != null
                                && reserva.getMesa()
                                .getIdMesa()
                                .equals(
                                        mesa.getIdMesa()
                                )
                )
                .filter(reserva ->
                        reserva.getEstado()
                                == EstadoReserva.CLIENTE_LLEGO
                                || reserva.getEstado()
                                == EstadoReserva.EN_ATENCION
                )
                .findFirst()
                .orElse(null);
    }

    private Mesa buscarMesa(Integer idMesa) {

        if (idMesa == null) {
            throw new IllegalStateException(
                    "El identificador de la mesa es obligatorio."
            );
        }

        return mesaRepository.findById(idMesa)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Mesa no encontrada."
                        )
                );
    }

    private Reserva buscarReserva(
            Integer idReserva
    ) {
        if (idReserva == null) {
            throw new IllegalStateException(
                    "El identificador de la reserva "
                            + "es obligatorio."
            );
        }

        return reservaRepository
                .buscarPorIdParaMesero(idReserva)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Reserva no encontrada."
                        )
                );
    }

    private Mesa obtenerMesaAsignada(
            Reserva reserva
    ) {
        if (reserva.getMesa() == null) {
            throw new IllegalStateException(
                    "La reserva no tiene una mesa asignada."
            );
        }

        return reserva.getMesa();
    }

    private void validarReservaDeHoy(
            Reserva reserva
    ) {
        LocalDate hoy = LocalDate.now(ZONA_PERU);

        if (
                reserva.getFechaReserva() == null
                        || !reserva.getFechaReserva()
                        .equals(hoy)
        ) {
            throw new IllegalStateException(
                    "Esta operación solo está disponible "
                            + "para reservas de hoy."
            );
        }
    }
}