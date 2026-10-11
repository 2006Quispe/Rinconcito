package com.rinconcito.service;

import com.rinconcito.dto.AdminMesaFormDTO;
import com.rinconcito.dto.AdminPisoFormDTO;
import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Mesa.EstadoMesa;
import com.rinconcito.entity.Piso;
import com.rinconcito.entity.Piso.EstadoPiso;
import com.rinconcito.repository.MesaRepository;
import com.rinconcito.repository.PisoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AdminPisoMesaService {

    private final PisoRepository pisoRepository;
    private final MesaRepository mesaRepository;

    public AdminPisoMesaService(
            PisoRepository pisoRepository,
            MesaRepository mesaRepository
    ) {
        this.pisoRepository = pisoRepository;
        this.mesaRepository = mesaRepository;
    }

    @Transactional(readOnly = true)
    public List<Piso> listarPisos() {
        return pisoRepository
                .findAllByOrderByIdPisoAsc();
    }

    @Transactional(readOnly = true)
    public List<Piso> listarPisosActivos() {
        return pisoRepository
                .findByEstadoOrderByIdPisoAsc(
                        EstadoPiso.ACTIVO
                );
    }

    @Transactional(readOnly = true)
    public List<Mesa> listarMesas(
            Integer idPiso
    ) {
        if (idPiso == null) {
            return mesaRepository
                    .findAllByOrderByPisoIdPisoAscNumeroAsc();
        }

        Piso piso = obtenerPiso(idPiso);

        List<Mesa> mesas =
                mesaRepository
                        .findByPisoOrderByNumeroAsc(piso);

        mesas.forEach(mesa ->
                mesa.getPiso().getNombre()
        );

        return mesas;
    }

    @Transactional(readOnly = true)
    public Piso obtenerPiso(
            Integer idPiso
    ) {
        if (idPiso == null) {
            throw new IllegalStateException(
                    "El piso no es válido."
            );
        }

        return pisoRepository.findById(idPiso)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Piso no encontrado."
                        )
                );
    }

    @Transactional(readOnly = true)
    public Mesa obtenerMesa(
            Integer idMesa
    ) {
        if (idMesa == null) {
            throw new IllegalStateException(
                    "La mesa no es válida."
            );
        }

        Mesa mesa = mesaRepository.findById(idMesa)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Mesa no encontrada."
                        )
                );

        mesa.getPiso().getNombre();

        return mesa;
    }

    public AdminPisoFormDTO nuevoFormularioPiso() {
        AdminPisoFormDTO formulario =
                new AdminPisoFormDTO();

        formulario.setEstado(
                EstadoPiso.ACTIVO.name()
        );

        return formulario;
    }

    @Transactional(readOnly = true)
    public AdminPisoFormDTO obtenerFormularioPiso(
            Integer idPiso
    ) {
        Piso piso = obtenerPiso(idPiso);

        AdminPisoFormDTO formulario =
                new AdminPisoFormDTO();

        formulario.setIdPiso(
                piso.getIdPiso()
        );

        formulario.setNombre(
                piso.getNombre()
        );

        formulario.setDescripcion(
                piso.getDescripcion()
        );

        formulario.setEstado(
                piso.getEstado().name()
        );

        return formulario;
    }

    @Transactional
    public Piso guardarPiso(
            AdminPisoFormDTO formulario
    ) {
        if (formulario == null) {
            throw new IllegalStateException(
                    "No se recibieron los datos del piso."
            );
        }

        normalizarPiso(formulario);
        validarFormularioPiso(formulario);

        EstadoPiso estado =
                convertirEstadoPiso(
                        formulario.getEstado()
                );

        Piso piso;

        if (formulario.esNuevo()) {
            validarNombrePisoDuplicado(
                    formulario.getNombre(),
                    null
            );

            piso = new Piso();

        } else {
            piso = obtenerPiso(
                    formulario.getIdPiso()
            );

            validarNombrePisoDuplicado(
                    formulario.getNombre(),
                    piso.getIdPiso()
            );

            if (estado == EstadoPiso.INACTIVO) {
                validarPisoPuedeDesactivarse(piso);
            }
        }

        piso.setNombre(
                formulario.getNombre()
        );

        piso.setDescripcion(
                formulario.getDescripcion()
        );

        piso.setEstado(estado);

        return pisoRepository.save(piso);
    }

    @Transactional
    public Piso cambiarEstadoPiso(
            Integer idPiso
    ) {
        Piso piso = obtenerPiso(idPiso);

        if (piso.getEstado() == EstadoPiso.ACTIVO) {
            validarPisoPuedeDesactivarse(piso);

            piso.setEstado(
                    EstadoPiso.INACTIVO
            );

        } else {
            piso.setEstado(
                    EstadoPiso.ACTIVO
            );
        }

        return pisoRepository.save(piso);
    }

    public AdminMesaFormDTO nuevoFormularioMesa(
            Integer idPiso
    ) {
        AdminMesaFormDTO formulario =
                new AdminMesaFormDTO();

        formulario.setIdPiso(idPiso);
        formulario.setEstado(
                EstadoMesa.DISPONIBLE.name()
        );

        return formulario;
    }

    @Transactional(readOnly = true)
    public AdminMesaFormDTO obtenerFormularioMesa(
            Integer idMesa
    ) {
        Mesa mesa = obtenerMesa(idMesa);

        AdminMesaFormDTO formulario =
                new AdminMesaFormDTO();

        formulario.setIdMesa(
                mesa.getIdMesa()
        );

        formulario.setIdPiso(
                mesa.getPiso().getIdPiso()
        );

        formulario.setNumero(
                mesa.getNumero()
        );

        formulario.setCapacidad(
                mesa.getCapacidad()
        );

        formulario.setEstado(
                mesa.getEstado().name()
        );

        return formulario;
    }

    @Transactional
    public Mesa guardarMesa(
            AdminMesaFormDTO formulario
    ) {
        if (formulario == null) {
            throw new IllegalStateException(
                    "No se recibieron los datos de la mesa."
            );
        }

        validarFormularioMesa(formulario);

        Piso piso = obtenerPiso(
                formulario.getIdPiso()
        );

        if (piso.getEstado() != EstadoPiso.ACTIVO) {
            throw new IllegalStateException(
                    "No se pueden registrar o modificar mesas "
                    + "en un piso inactivo."
            );
        }

        EstadoMesa estado =
                convertirEstadoMesaAdministrativo(
                        formulario.getEstado()
                );

        Mesa mesa;

        if (formulario.esNueva()) {
            validarNumeroMesaDuplicado(
                    piso,
                    formulario.getNumero(),
                    null
            );

            mesa = new Mesa();

        } else {
            mesa = obtenerMesa(
                    formulario.getIdMesa()
            );

            validarMesaPuedeEditar(
                    mesa
            );

            validarNumeroMesaDuplicado(
                    piso,
                    formulario.getNumero(),
                    mesa.getIdMesa()
            );
        }

        mesa.setPiso(piso);
        mesa.setNumero(
                formulario.getNumero()
        );
        mesa.setCapacidad(
                formulario.getCapacidad()
        );
        mesa.setEstado(estado);

        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa cambiarEstadoMesa(
            Integer idMesa
    ) {
        Mesa mesa = obtenerMesa(idMesa);

        if (mesa.getEstado()
                == EstadoMesa.DISPONIBLE) {

            mesa.setEstado(
                    EstadoMesa.INACTIVA
            );

        } else if (mesa.getEstado()
                == EstadoMesa.INACTIVA) {

            if (mesa.getPiso().getEstado()
                    != EstadoPiso.ACTIVO) {

                throw new IllegalStateException(
                        "No puedes activar una mesa "
                        + "que pertenece a un piso inactivo."
                );
            }

            mesa.setEstado(
                    EstadoMesa.DISPONIBLE
            );

        } else {
            throw new IllegalStateException(
                    "No se puede desactivar una mesa "
                    + "que está ocupada o reservada."
            );
        }

        return mesaRepository.save(mesa);
    }

    @Transactional(readOnly = true)
    public long contarMesasPorPiso(
            Piso piso
    ) {
        return mesaRepository.countByPiso(piso);
    }

    @Transactional(readOnly = true)
    public long contarMesasDisponibles(
            Piso piso
    ) {
        return mesaRepository.countByPisoAndEstado(
                piso,
                EstadoMesa.DISPONIBLE
        );
    }

    @Transactional(readOnly = true)
    public long contarMesasOcupadas(
            Piso piso
    ) {
        return mesaRepository.countByPisoAndEstado(
                piso,
                EstadoMesa.OCUPADA
        );
    }

    @Transactional(readOnly = true)
    public long contarMesasReservadas(
            Piso piso
    ) {
        return mesaRepository.countByPisoAndEstado(
                piso,
                EstadoMesa.RESERVADA
        );
    }

    private void validarFormularioPiso(
            AdminPisoFormDTO formulario
    ) {
        String nombre = formulario.getNombre();

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalStateException(
                    "El nombre del piso es obligatorio."
            );
        }

        if (nombre.length() < 2
                || nombre.length() > 20) {

            throw new IllegalStateException(
                    "El nombre del piso debe contener "
                    + "entre 2 y 20 caracteres."
            );
        }

        if (!nombre.matches(
                "^[\\p{L}\\d]+(?:[ .'-][\\p{L}\\d]+)*$"
        )) {
            throw new IllegalStateException(
                    "El nombre del piso contiene "
                    + "caracteres no permitidos."
            );
        }

        if (formulario.getDescripcion() != null
                && formulario.getDescripcion().length() > 200) {

            throw new IllegalStateException(
                    "La descripción no puede superar "
                    + "200 caracteres."
            );
        }
    }

    private void validarFormularioMesa(
            AdminMesaFormDTO formulario
    ) {
        if (formulario.getIdPiso() == null) {
            throw new IllegalStateException(
                    "Debes seleccionar un piso."
            );
        }

        if (formulario.getNumero() == null
                || formulario.getNumero() < 1
                || formulario.getNumero() > 999) {

            throw new IllegalStateException(
                    "El número de mesa debe estar "
                    + "entre 1 y 999."
            );
        }

        if (formulario.getCapacidad() == null
                || formulario.getCapacidad() < 1
                || formulario.getCapacidad() > 30) {

            throw new IllegalStateException(
                    "La capacidad debe estar "
                    + "entre 1 y 30 personas."
            );
        }
    }

    private void validarNombrePisoDuplicado(
            String nombre,
            Integer idPiso
    ) {
        boolean existe =
                idPiso == null
                        ? pisoRepository
                        .existsByNombreIgnoreCase(nombre)
                        : pisoRepository
                        .existsByNombreIgnoreCaseAndIdPisoNot(
                                nombre,
                                idPiso
                        );

        if (existe) {
            throw new IllegalStateException(
                    "Ya existe un piso con ese nombre."
            );
        }
    }

    private void validarNumeroMesaDuplicado(
            Piso piso,
            Integer numero,
            Integer idMesa
    ) {
        boolean existe =
                idMesa == null
                        ? mesaRepository
                        .existsByPisoAndNumero(
                                piso,
                                numero
                        )
                        : mesaRepository
                        .existsByPisoAndNumeroAndIdMesaNot(
                                piso,
                                numero,
                                idMesa
                        );

        if (existe) {
            throw new IllegalStateException(
                    "La mesa número "
                    + numero
                    + " ya existe en "
                    + piso.getNombre()
                    + "."
            );
        }
    }

    private void validarPisoPuedeDesactivarse(
            Piso piso
    ) {
        long ocupadas =
                mesaRepository.countByPisoAndEstado(
                        piso,
                        EstadoMesa.OCUPADA
                );

        long reservadas =
                mesaRepository.countByPisoAndEstado(
                        piso,
                        EstadoMesa.RESERVADA
                );

        if (ocupadas > 0 || reservadas > 0) {
            throw new IllegalStateException(
                    "No puedes desactivar el piso porque "
                    + "tiene mesas ocupadas o reservadas."
            );
        }
    }

    private void validarMesaPuedeEditar(
            Mesa mesa
    ) {
        if (mesa.getEstado() == EstadoMesa.OCUPADA
                || mesa.getEstado() == EstadoMesa.RESERVADA) {

            throw new IllegalStateException(
                    "No puedes modificar una mesa "
                    + "que está ocupada o reservada."
            );
        }
    }

    private EstadoPiso convertirEstadoPiso(
            String estado
    ) {
        if (estado == null || estado.isBlank()) {
            return EstadoPiso.ACTIVO;
        }

        try {
            return EstadoPiso.valueOf(
                    estado.trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "El estado del piso no es válido."
            );
        }
    }

    private EstadoMesa convertirEstadoMesaAdministrativo(
            String estado
    ) {
        if (estado == null || estado.isBlank()) {
            return EstadoMesa.DISPONIBLE;
        }

        EstadoMesa estadoMesa;

        try {
            estadoMesa = EstadoMesa.valueOf(
                    estado.trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "El estado de la mesa no es válido."
            );
        }

        if (estadoMesa != EstadoMesa.DISPONIBLE
                && estadoMesa != EstadoMesa.INACTIVA) {

            throw new IllegalStateException(
                    "Desde Administración una mesa solo "
                    + "puede quedar disponible o inactiva."
            );
        }

        return estadoMesa;
    }

    private void normalizarPiso(
            AdminPisoFormDTO formulario
    ) {
        formulario.setNombre(
                limpiarEspacios(
                        formulario.getNombre()
                )
        );

        formulario.setDescripcion(
                limpiarEspacios(
                        formulario.getDescripcion()
                )
        );

        formulario.setEstado(
                limpiarOpcional(
                        formulario.getEstado()
                )
        );
    }

    private String limpiarEspacios(
            String texto
    ) {
        if (texto == null) {
            return null;
        }

        String limpio = texto.trim()
                .replaceAll("\\s+", " ");

        return limpio.isBlank()
                ? null
                : limpio;
    }

    private String limpiarOpcional(
            String texto
    ) {
        if (texto == null) {
            return null;
        }

        String limpio = texto.trim();

        return limpio.isBlank()
                ? null
                : limpio;
    }
}