package com.rinconcito.service;

import com.rinconcito.dto.ReservaFormDTO;
import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Piso;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.repository.PisoRepository;
import com.rinconcito.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReservaModuloIT {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private MesaService mesaService;

    @Autowired
    private PisoRepository pisoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    private LocalDate proximo(DayOfWeek dia) {
        LocalDate d = LocalDate.now();
        while (d.getDayOfWeek() != dia) {
            d = d.plusDays(1);
        }
        // si es hoy y ya pasó la hora de prueba, usar la siguiente semana
        if (d.equals(LocalDate.now()) && LocalTime.now().isAfter(LocalTime.of(14, 0))) {
            d = d.plusWeeks(1);
        }
        return d;
    }

    private ReservaFormDTO baseForm(LocalDate fecha, LocalTime hora, int personas, int idPiso, String dni) {
        ReservaFormDTO dto = new ReservaFormDTO();
        dto.setDni(dni);
        dto.setNombres("Test");
        dto.setApellidos("Reserva");
        dto.setTelefono("999888777");
        dto.setFecha(fecha);
        dto.setHoraInicio(hora);
        dto.setNumeroPersonas(personas);
        dto.setIdPiso(idPiso);
        dto.setOcasionEspecial("Prueba");
        return dto;
    }

    @Test
    void asignaMesaCapacidadMasCercanaEnPiso() {
        Piso piso1 = pisoRepository.findById(1).orElseThrow();
        LocalDate fecha = proximo(DayOfWeek.WEDNESDAY);
        LocalTime inicio = LocalTime.of(12, 0);
        LocalTime fin = inicio.plusHours(2);

        // 5 personas → mesa de capacidad 6 (más cercana), no la de 8
        Optional<Mesa> mesa = mesaService.buscarMesaDisponible(piso1, fecha, inicio, fin, 5);
        assertTrue(mesa.isPresent());
        assertEquals(1, mesa.get().getPiso().getIdPiso());
        assertEquals(6, mesa.get().getCapacidad());
    }

    @Test
    void filtroPisoNoAsignaMesaDeOtroPiso() {
        Piso piso2 = pisoRepository.findById(2).orElseThrow();
        LocalDate fecha = proximo(DayOfWeek.THURSDAY);
        Optional<Mesa> mesa = mesaService.buscarMesaDisponible(
                piso2, fecha, LocalTime.of(13, 0), LocalTime.of(15, 0), 2);
        assertTrue(mesa.isPresent());
        assertEquals(2, mesa.get().getPiso().getIdPiso());
    }

    @Test
    void cruceHorarioConConfirmadaBloqueaMismaMesa() {
        LocalDate fecha = proximo(DayOfWeek.FRIDAY);
        ReservaFormDTO dto = baseForm(fecha, LocalTime.of(12, 0), 4, 1, "20000001");
        Reserva r1 = reservaService.crearReserva(dto);
        assertEquals(EstadoReserva.CONFIRMADA, r1.getEstado());
        assertNotNull(r1.getMesa());
        assertTrue(r1.getCodigoReserva().matches("RES-\\d{4}-\\d{6}"));

        // Cruce parcial misma capacidad en mismo piso → debe usar otra mesa o fallar si no hay
        ReservaFormDTO dto2 = baseForm(fecha, LocalTime.of(13, 0), 4, 1, "20000002");
        Reserva r2 = reservaService.crearReserva(dto2);
        assertEquals(EstadoReserva.CONFIRMADA, r2.getEstado());
        assertNotNull(r2.getMesa());
        assertNotEquals(r1.getMesa().getIdMesa(), r2.getMesa().getIdMesa());
    }

    @Test
    void pendientePagoNoAsignaMesaNiBloquea() {
        // Domingo con evento Almuerzo Show (requiere pago)
        LocalDate domingo = proximo(DayOfWeek.SUNDAY);
        ReservaFormDTO dto = baseForm(domingo, LocalTime.of(14, 0), 3, 1, "20000003");
        Reserva r = reservaService.crearReserva(dto);

        assertEquals(EstadoReserva.PENDIENTE_PAGO, r.getEstado());
        assertNull(r.getMesa());
        assertTrue(r.isRequierePago());
        assertEquals(75, r.getMontoReserva()); // 25 * 3

        // Otra reserva CONFIRMADA no aplica a domingo con pago… en domingo el evento requiere pago
        // Verificamos que PENDIENTE_PAGO no ocupa: buscar mesa sigue disponible
        Optional<Mesa> libre = mesaService.buscarMesaDisponible(
                pisoRepository.findById(1).orElseThrow(),
                domingo, LocalTime.of(14, 0), LocalTime.of(16, 0), 3);
        assertTrue(libre.isPresent());
    }

    @Test
    void eventoSinPagoConfirmaConMesa() {
        LocalDate sabado = proximo(DayOfWeek.SATURDAY);
        ReservaFormDTO dto = baseForm(sabado, LocalTime.of(12, 0), 2, 2, "20000004");
        Reserva r = reservaService.crearReserva(dto);
        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
        assertNotNull(r.getMesa());
        assertEquals(2, r.getPisoSolicitado().getIdPiso());
        assertFalse(r.isRequierePago());
        assertEquals(0, r.getMontoReserva());
    }

    @Test
    void lunesCerradoRechaza() {
        LocalDate lunes = proximo(DayOfWeek.MONDAY);
        ReservaFormDTO dto = baseForm(lunes, LocalTime.of(12, 0), 2, 1, "20000005");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reservaService.crearReserva(dto));
        assertTrue(ex.getMessage().toLowerCase().contains("cerrado"));
    }

    @Test
    void horaFueraDeAtencionRechaza() {
        LocalDate martes = proximo(DayOfWeek.TUESDAY);
        ReservaFormDTO dto = baseForm(martes, LocalTime.of(19, 0), 2, 1, "20000006");
        assertThrows(IllegalStateException.class, () -> reservaService.crearReserva(dto));
    }

    @Test
    void filtrosAdminPorFechaPisoEstado() {
        LocalDate fecha = proximo(DayOfWeek.WEDNESDAY);
        Reserva r = reservaService.crearReserva(baseForm(fecha, LocalTime.of(11, 0), 2, 3, "20000007"));
        var lista = reservaService.listarConFiltros(fecha, 3, EstadoReserva.CONFIRMADA);
        assertTrue(lista.stream().anyMatch(x -> x.getIdReserva().equals(r.getIdReserva())));
    }
}