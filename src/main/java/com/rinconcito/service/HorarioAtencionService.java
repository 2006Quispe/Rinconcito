package com.rinconcito.service;

import com.rinconcito.dto.HorarioAtencionFormDTO;
import com.rinconcito.dto.HorarioConfiguracionFormDTO;
import com.rinconcito.dto.HorarioResumenDTO;
import com.rinconcito.entity.HorarioAtencion;
import com.rinconcito.entity.HorarioAtencion.DiaSemana;
import com.rinconcito.repository.HorarioAtencionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HorarioAtencionService {

    private static final LocalTime APERTURA_PREDETERMINADA =
            LocalTime.of(11, 0);

    private static final LocalTime CIERRE_NORMAL =
            LocalTime.of(18, 0);

    private static final LocalTime CIERRE_DOMINGO =
            LocalTime.of(22, 0);

    private final HorarioAtencionRepository
            horarioAtencionRepository;

    public HorarioAtencionService(
            HorarioAtencionRepository
                    horarioAtencionRepository
    ) {
        this.horarioAtencionRepository =
                horarioAtencionRepository;
    }

    /*
     * Lista los horarios ordenados de lunes a domingo.
     */
    @Transactional(readOnly = true)
    public List<HorarioAtencion> listar() {

        List<HorarioAtencion> horarios =
                horarioAtencionRepository.findAll();

        horarios.sort(
                Comparator.comparingInt(
                        horario ->
                                horario.getDiaSemana()
                                        .ordinal()
                )
        );

        return horarios;
    }

    /*
     * Prepara los siete días para el formulario
     * del administrador.
     */
    @Transactional(readOnly = true)
    public HorarioConfiguracionFormDTO
    obtenerFormularioConfiguracion() {

        Map<DiaSemana, HorarioAtencion> horariosGuardados =
                new EnumMap<>(DiaSemana.class);

        horarioAtencionRepository
                .findAll()
                .forEach(horario ->
                        horariosGuardados.put(
                                horario.getDiaSemana(),
                                horario
                        )
                );

        HorarioConfiguracionFormDTO formulario =
                new HorarioConfiguracionFormDTO();

        List<HorarioAtencionFormDTO> horariosFormulario =
                new ArrayList<>();

        for (DiaSemana dia : DiaSemana.values()) {

            HorarioAtencion horarioGuardado =
                    horariosGuardados.get(dia);

            HorarioAtencionFormDTO horarioForm =
                    new HorarioAtencionFormDTO();

            horarioForm.setDiaSemana(dia);

            if (horarioGuardado != null) {

                horarioForm.setIdHorario(
                        horarioGuardado.getIdHorario()
                );

                horarioForm.setAbierto(
                        horarioGuardado.isAbierto()
                );

                horarioForm.setHoraApertura(
                        horarioGuardado.getHoraApertura()
                );

                horarioForm.setHoraCierre(
                        horarioGuardado.getHoraCierre()
                );

            } else {
                aplicarHorarioPredeterminado(
                        horarioForm
                );
            }

            horariosFormulario.add(
                    horarioForm
            );
        }

        formulario.setHorarios(
                horariosFormulario
        );

        return formulario;
    }

    /*
     * Guarda los siete horarios enviados
     * desde el panel del administrador.
     */
    @Transactional
    public void guardarConfiguracion(
            HorarioConfiguracionFormDTO formulario
    ) {
        validarFormulario(formulario);

        for (
                HorarioAtencionFormDTO horarioForm
                : formulario.getHorarios()
        ) {
            HorarioAtencion horario =
                    horarioAtencionRepository
                            .findByDiaSemana(
                                    horarioForm.getDiaSemana()
                            )
                            .orElseGet(
                                    HorarioAtencion::new
                            );

            horario.setDiaSemana(
                    horarioForm.getDiaSemana()
            );

            horario.setAbierto(
                    horarioForm.isAbierto()
            );

            if (horarioForm.isAbierto()) {

                horario.setHoraApertura(
                        horarioForm.getHoraApertura()
                );

                horario.setHoraCierre(
                        horarioForm.getHoraCierre()
                );

            } else {

                horario.setHoraApertura(null);
                horario.setHoraCierre(null);
            }

            horarioAtencionRepository.save(
                    horario
            );
        }
    }

    /*
     * Agrupa días consecutivos que poseen
     * el mismo horario para mostrarlos en el footer.
     */
    @Transactional(readOnly = true)
    public List<HorarioResumenDTO> resumir() {

        List<HorarioAtencion> horarios =
                listar();

        List<HorarioResumenDTO> resumen =
                new ArrayList<>();

        if (horarios.isEmpty()) {
            return resumen;
        }

        HorarioAtencion inicioGrupo =
                horarios.get(0);

        HorarioAtencion finGrupo =
                horarios.get(0);

        for (int i = 1; i < horarios.size(); i++) {

            HorarioAtencion actual =
                    horarios.get(i);

            if (
                    mismoHorario(
                            finGrupo,
                            actual
                    )
                            && consecutivos(
                                    finGrupo.getDiaSemana(),
                                    actual.getDiaSemana()
                            )
            ) {
                finGrupo = actual;

            } else {

                resumen.add(
                        crearResumen(
                                inicioGrupo,
                                finGrupo
                        )
                );

                inicioGrupo = actual;
                finGrupo = actual;
            }
        }

        resumen.add(
                crearResumen(
                        inicioGrupo,
                        finGrupo
                )
        );

        return resumen;
    }

    /*
     * Valida que la reserva esté dentro
     * del horario de atención configurado.
     */
    @Transactional(readOnly = true)
    public void validarHorarioParaReserva(
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
        if (
                fecha == null
                        || horaInicio == null
                        || horaFin == null
        ) {
            throw new IllegalStateException(
                    "La fecha y el horario de la reserva "
                            + "son obligatorios."
            );
        }

        DiaSemana dia =
                mapearDia(
                        fecha.getDayOfWeek()
                );

        HorarioAtencion horario =
                horarioAtencionRepository
                        .findByDiaSemana(dia)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No hay horario de atención "
                                                + "configurado para "
                                                + nombreDia(
                                                        dia,
                                                        false
                                                )
                                                + "."
                                )
                        );

        if (
                !horario.isAbierto()
                        || horario.getHoraApertura() == null
                        || horario.getHoraCierre() == null
        ) {
            throw new IllegalStateException(
                    "El restaurante está cerrado los "
                            + nombreDia(dia, false)
                            + "."
            );
        }

        LocalTime apertura =
                horario.getHoraApertura();

        LocalTime cierre =
                horario.getHoraCierre();

        if (
                horaInicio.isBefore(apertura)
                        || !horaInicio.isBefore(cierre)
        ) {
            throw new IllegalStateException(
                    "La hora de inicio debe estar entre "
                            + formatearHora(apertura)
                            + " y "
                            + formatearHora(cierre)
                            + "."
            );
        }

        if (
                !horaFin.isAfter(horaInicio)
        ) {
            throw new IllegalStateException(
                    "La hora de finalización debe ser "
                            + "posterior a la hora de inicio."
            );
        }

        if (horaFin.isAfter(cierre)) {
            throw new IllegalStateException(
                    "La reserva supera la hora de cierre ("
                            + formatearHora(cierre)
                            + "). Elige una hora más temprana."
            );
        }
    }

    public static DiaSemana mapearDia(
            DayOfWeek dayOfWeek
    ) {
        return switch (dayOfWeek) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }

    private void validarFormulario(
            HorarioConfiguracionFormDTO formulario
    ) {
        if (
                formulario == null
                        || formulario.getHorarios() == null
                        || formulario.getHorarios().isEmpty()
        ) {
            throw new IllegalStateException(
                    "Debes configurar los horarios de atención."
            );
        }

        if (
                formulario.getHorarios().size()
                        != DiaSemana.values().length
        ) {
            throw new IllegalStateException(
                    "Debes enviar la configuración "
                            + "de los siete días."
            );
        }

        Set<DiaSemana> diasRecibidos =
                new HashSet<>();

        for (
                HorarioAtencionFormDTO horario
                : formulario.getHorarios()
        ) {
            if (
                    horario == null
                            || horario.getDiaSemana() == null
            ) {
                throw new IllegalStateException(
                        "Uno de los días enviados no es válido."
                );
            }

            if (
                    !diasRecibidos.add(
                            horario.getDiaSemana()
                    )
            ) {
                throw new IllegalStateException(
                        "El día "
                                + nombreDia(
                                        horario.getDiaSemana(),
                                        true
                                )
                                + " está repetido."
                );
            }

            validarHorarioDia(
                    horario
            );
        }

        for (DiaSemana dia : DiaSemana.values()) {

            if (!diasRecibidos.contains(dia)) {

                throw new IllegalStateException(
                        "Falta configurar el día "
                                + nombreDia(dia, true)
                                + "."
                );
            }
        }
    }

    private void validarHorarioDia(
            HorarioAtencionFormDTO horario
    ) {
        if (!horario.isAbierto()) {
            return;
        }

        if (
                horario.getHoraApertura() == null
                        || horario.getHoraCierre() == null
        ) {
            throw new IllegalStateException(
                    "Debes ingresar la hora de apertura "
                            + "y cierre para "
                            + nombreDia(
                                    horario.getDiaSemana(),
                                    false
                            )
                            + "."
            );
        }

        if (
                !horario.getHoraCierre()
                        .isAfter(
                                horario.getHoraApertura()
                        )
        ) {
            throw new IllegalStateException(
                    "La hora de cierre de "
                            + nombreDia(
                                    horario.getDiaSemana(),
                                    false
                            )
                            + " debe ser posterior "
                            + "a la hora de apertura."
            );
        }
    }

    private void aplicarHorarioPredeterminado(
            HorarioAtencionFormDTO horario
    ) {
        DiaSemana dia =
                horario.getDiaSemana();

        if (dia == DiaSemana.LUNES) {

            horario.setAbierto(false);
            horario.setHoraApertura(null);
            horario.setHoraCierre(null);

            return;
        }

        horario.setAbierto(true);
        horario.setHoraApertura(
                APERTURA_PREDETERMINADA
        );

        if (dia == DiaSemana.DOMINGO) {

            horario.setHoraCierre(
                    CIERRE_DOMINGO
            );

        } else {

            horario.setHoraCierre(
                    CIERRE_NORMAL
            );
        }
    }

    private boolean mismoHorario(
            HorarioAtencion primero,
            HorarioAtencion segundo
    ) {
        return primero.isAbierto()
                == segundo.isAbierto()
                && iguales(
                        primero.getHoraApertura(),
                        segundo.getHoraApertura()
                )
                && iguales(
                        primero.getHoraCierre(),
                        segundo.getHoraCierre()
                );
    }

    private boolean iguales(
            LocalTime primero,
            LocalTime segundo
    ) {
        if (
                primero == null
                        && segundo == null
        ) {
            return true;
        }

        if (
                primero == null
                        || segundo == null
        ) {
            return false;
        }

        return primero.equals(segundo);
    }

    private boolean consecutivos(
            DiaSemana anterior,
            DiaSemana actual
    ) {
        return actual.ordinal()
                == anterior.ordinal() + 1;
    }

    private HorarioResumenDTO crearResumen(
            HorarioAtencion inicio,
            HorarioAtencion fin
    ) {
        String dias;

        if (
                inicio.getDiaSemana()
                        == fin.getDiaSemana()
        ) {
            dias = nombreDia(
                    inicio.getDiaSemana(),
                    true
            );

        } else {

            dias =
                    nombreDia(
                            inicio.getDiaSemana(),
                            true
                    )
                            + " a "
                            + nombreDia(
                                    fin.getDiaSemana(),
                                    false
                            );
        }

        String detalle;

        if (
                !inicio.isAbierto()
                        || inicio.getHoraApertura() == null
                        || inicio.getHoraCierre() == null
        ) {
            detalle = "Cerrado";

        } else {

            detalle =
                    formatearHora(
                            inicio.getHoraApertura()
                    )
                            + " a "
                            + formatearHora(
                                    inicio.getHoraCierre()
                            );
        }

        return new HorarioResumenDTO(
                dias,
                detalle
        );
    }

    private String nombreDia(
            DiaSemana dia,
            boolean capitalizar
    ) {
        String nombre = switch (dia) {
            case LUNES -> "lunes";
            case MARTES -> "martes";
            case MIERCOLES -> "miércoles";
            case JUEVES -> "jueves";
            case VIERNES -> "viernes";
            case SABADO -> "sábado";
            case DOMINGO -> "domingo";
        };

        if (!capitalizar) {
            return nombre;
        }

        return Character.toUpperCase(
                nombre.charAt(0)
        ) + nombre.substring(1);
    }

    private String formatearHora(
            LocalTime hora
    ) {
        int hour =
                hora.getHour();

        int minute =
                hora.getMinute();

        String periodo =
                hour < 12
                        ? "a. m."
                        : "p. m.";

        int hora12 =
                hour % 12;

        if (hora12 == 0) {
            hora12 = 12;
        }

        return hora12
                + ":"
                + String.format(
                        "%02d",
                        minute
                )
                + " "
                + periodo;
    }
}