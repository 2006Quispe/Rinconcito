package com.rinconcito.service;

import com.rinconcito.service.correo.ColaCorreoService;

import com.rinconcito.dto.ReservaFormDTO;
import com.rinconcito.entity.Evento;
import com.rinconcito.entity.Evento.EstadoEvento;
import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Piso;
import com.rinconcito.entity.Piso.EstadoPiso;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.EventoRepository;
import com.rinconcito.repository.PisoRepository;
import com.rinconcito.repository.ReservaRepository;
import com.rinconcito.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservaService {

    @org.springframework.beans.factory.annotation.Autowired
    private ColaCorreoService colaCorreoService;

    private static final int DURACION_HORAS = 2;
    private static final int MAX_RESERVAS_POR_DIA = 1;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MesaService mesaService;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private PisoRepository pisoRepository;

    @Autowired
    private HorarioAtencionService horarioAtencionService;

    @Transactional
    public Reserva crearReserva(ReservaFormDTO dto) {
        return crearReserva(dto, null);
    }

    @Transactional
    public Reserva crearReserva(ReservaFormDTO dto, Usuario clienteLogueado) {
        validarCamposBasicos(dto);

        LocalDate hoy = LocalDate.now();
        if (dto.getFecha().isBefore(hoy)) {
            throw new IllegalStateException("No puedes reservar para una fecha que ya pasó.");
        }
        if (dto.getFecha().isEqual(hoy) && dto.getHoraInicio().isBefore(LocalTime.now())) {
            throw new IllegalStateException("No puedes reservar para una hora que ya pasó hoy.");
        }

        LocalTime horaFin = dto.getHoraInicio().plusHours(DURACION_HORAS);
        horarioAtencionService.validarHorarioParaReserva(dto.getFecha(), dto.getHoraInicio(), horaFin);

        Piso piso = pisoRepository.findById(dto.getIdPiso())
                .orElseThrow(() -> new IllegalStateException("El piso seleccionado no existe."));
        if (piso.getEstado() != EstadoPiso.ACTIVO) {
            throw new IllegalStateException("El piso seleccionado no está disponible.");
        }

        Usuario cliente = resolverCliente(dto, clienteLogueado);

        long reservasActivas = reservaRepository
                .findByClienteAndFechaReserva(cliente, dto.getFecha())
                .stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA
                        && r.getEstado() != EstadoReserva.RECHAZADA
                        && r.getEstado() != EstadoReserva.FINALIZADA)
                .count();
        if (reservasActivas >= MAX_RESERVAS_POR_DIA) {
            throw new IllegalStateException(
                    "Ya tienes una reserva registrada para ese día. Si necesitas cambiarla, cancela la anterior primero.");
        }

        Evento evento = null;
        List<Evento> eventosDelDia = eventoRepository.findByFechaAndEstado(dto.getFecha(), EstadoEvento.ACTIVO);
        if (!eventosDelDia.isEmpty()) {
            evento = eventosDelDia.get(0);
        }

        boolean requierePago = evento != null
                && evento.isRequierePago()
                && evento.getPrecioEntrada() != null
                && evento.getPrecioEntrada() > 0;
        int monto = requierePago ? evento.getPrecioEntrada() * dto.getNumeroPersonas() : 0;

        Reserva reserva = new Reserva();
        reserva.setCodigoReserva(generarCodigoReserva());
        reserva.setCliente(cliente);
        reserva.setPisoSolicitado(piso);
        reserva.setEvento(evento);
        reserva.setFechaReserva(dto.getFecha());
        reserva.setHoraInicio(dto.getHoraInicio());
        reserva.setHoraFin(horaFin);
        reserva.setNumeroPersonas(dto.getNumeroPersonas());
        reserva.setOcasionEspecial(blankToNull(dto.getOcasionEspecial()));
        reserva.setComentarios(blankToNull(dto.getComentarios()));
        reserva.setRequierePago(requierePago);
        reserva.setMontoReserva(monto);

        if (requierePago) {
            // No asignar ni bloquear mesa hasta validar pago (módulo aparte).
            reserva.setMesa(null);
            reserva.setEstado(EstadoReserva.PENDIENTE_PAGO);
        } else {
            Optional<Mesa> mesaDisponible = mesaService.buscarMesaDisponible(
                    piso, dto.getFecha(), dto.getHoraInicio(), horaFin, dto.getNumeroPersonas());
            if (mesaDisponible.isEmpty()) {
                throw new IllegalStateException(
                        "No hay mesas disponibles en " + piso.getNombre()
                                + " para " + dto.getNumeroPersonas()
                                + " personas en ese horario. Intenta con otro piso, fecha u hora.");
            }
            reserva.setMesa(mesaDisponible.get());
            reserva.setEstado(EstadoReserva.CONFIRMADA);
        }

        reserva = reservaRepository.save(reserva);
        // Inicializar asociaciones para la vista
        reserva.getPisoSolicitado().getNombre();
        reserva.getCliente().getNombres();
        if (reserva.getMesa() != null) {
            reserva.getMesa().getNumero();
        }
        if (reserva.getEvento() != null) {
            reserva.getEvento().getTitulo();
        }
        colaCorreoService.reserva(reserva,
                clienteLogueado != null && clienteLogueado.getRol() != null
                        && "CLIENTE".equals(clienteLogueado.getRol().getNombre())
                        ? clienteLogueado.getCorreo() : dto.getCorreo());
        return reserva;
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarConFiltros(LocalDate fecha, Integer idPiso, EstadoReserva estado) {
        Piso piso = null;
        if (idPiso != null) {
            piso = pisoRepository.findById(idPiso).orElse(null);
        }
        return reservaRepository.buscarConFiltros(fecha, piso, estado);
    }

    private void validarCamposBasicos(ReservaFormDTO dto) {
        if (dto.getDni() == null || !dto.getDni().matches("\\d{8}")) {
            throw new IllegalStateException("El DNI debe tener 8 dígitos.");
        }
        if (StringUtils.isBlank(dto.getNombres())) {
        throw new IllegalStateException("Los nombres son obligatorios.");
        }
        if (StringUtils.isBlank(dto.getApellidos())) {
        throw new IllegalStateException("Los apellidos son obligatorios.");
        }
        if (dto.getTelefono() == null || !dto.getTelefono().matches("\\d{9}")) {
            throw new IllegalStateException("El teléfono debe tener 9 dígitos.");
        }
        if (dto.getFecha() == null) {
            throw new IllegalStateException("La fecha es obligatoria.");
        }
        if (dto.getHoraInicio() == null) {
            throw new IllegalStateException("La hora es obligatoria.");
        }
        if (dto.getNumeroPersonas() == null || dto.getNumeroPersonas() < 1) {
            throw new IllegalStateException("El número de personas debe ser al menos 1.");
        }
        if (dto.getIdPiso() == null) {
            throw new IllegalStateException("Debes elegir un piso.");
        }
        if (dto.getCorreo() != null && !dto.getCorreo().isBlank()
                && !dto.getCorreo().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalStateException("El correo no tiene un formato válido.");
        }
    }

    private Usuario resolverCliente(ReservaFormDTO dto, Usuario clienteLogueado) {
        if (clienteLogueado != null
                && clienteLogueado.getRol() != null
                && "CLIENTE".equalsIgnoreCase(clienteLogueado.getRol().getNombre())) {
            if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
                clienteLogueado.setTelefono(dto.getTelefono());
            }
            if (dto.getCorreo() != null && !dto.getCorreo().isBlank()
                    && (clienteLogueado.getCorreo() == null
                    || clienteLogueado.getCorreo().endsWith("@invitado.rinconcito.pe"))) {
                // no forzar cambio de correo de cuenta real
            }
            return usuarioRepository.save(clienteLogueado);
        }

        return usuarioService.buscarOCrearInvitado(
                dto.getDni(),
                dto.getNombres().trim(),
                dto.getApellidos().trim(),
                dto.getTelefono(),
                blankToNull(dto.getCorreo()));
    }

    private String generarCodigoReserva() {
        int anio = LocalDate.now().getYear();
        String prefijo = "RES-" + anio + "-";
        Optional<Reserva> ultima = reservaRepository
                .findTopByCodigoReservaStartingWithOrderByCodigoReservaDesc(prefijo);
        int siguiente = 1;
        if (ultima.isPresent()) {
            String codigo = ultima.get().getCodigoReserva();
            String sufijo = codigo.substring(prefijo.length());
            try {
                siguiente = Integer.parseInt(sufijo) + 1;
            } catch (NumberFormatException ignored) {
                siguiente = 1;
            }
        }
        return prefijo + String.format("%06d", siguiente);
    }

    private String blankToNull(String value) {
        return StringUtils.trimToNull(value);
    }
}
