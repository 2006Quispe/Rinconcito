package com.rinconcito.service;

import com.rinconcito.dto.AdminUsuarioFormDTO;
import com.rinconcito.entity.Rol;
import com.rinconcito.entity.Usuario;
import com.rinconcito.entity.Usuario.EstadoUsuario;
import com.rinconcito.entity.Usuario.TipoRegistro;
import com.rinconcito.repository.RolRepository;
import com.rinconcito.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminUsuarioService {

    private static final String PATRON_NOMBRE =
            "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$";

    private static final String PATRON_CORREO =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> buscar(
            String buscar,
            Integer idRol,
            String estado,
            String tipoRegistro
    ) {
        String textoBusqueda =
                limpiarOpcional(buscar);

        EstadoUsuario estadoUsuario =
                convertirEstadoOpcional(estado);

        TipoRegistro tipo =
                convertirTipoRegistroOpcional(
                        tipoRegistro
                );

        return usuarioRepository.buscarParaAdministracion(
                textoBusqueda,
                idRol,
                estadoUsuario,
                tipo
        );
    }

    @Transactional(readOnly = true)
    public List<Rol> listarRoles() {

        return rolRepository.findAll()
                .stream()
                .filter(this::esRolDePersonal)
                .sorted(
                        Comparator.comparing(
                                Rol::getNombre
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUsuarioFormDTO obtenerFormulario(
            Integer idUsuario
    ) {
        Usuario usuario =
                obtenerUsuario(idUsuario);

        AdminUsuarioFormDTO formulario =
                new AdminUsuarioFormDTO();

        formulario.setIdUsuario(
                usuario.getIdUsuario()
        );

        formulario.setIdRol(
                usuario.getRol().getIdRol()
        );

        formulario.setNombres(
                usuario.getNombres()
        );

        formulario.setApellidos(
                usuario.getApellidos()
        );

        formulario.setDni(
                usuario.getDni()
        );

        formulario.setTelefono(
                usuario.getTelefono()
        );

        formulario.setCorreo(
                usuario.getCorreo()
        );

        formulario.setEstado(
                usuario.getEstado().name()
        );

        formulario.setContrasena(null);

        return formulario;
    }

    @Transactional
    public Usuario guardar(
            AdminUsuarioFormDTO formulario,
            String correoAdministrador
    ) {
        if (formulario == null) {
            throw new IllegalStateException(
                    "No se recibieron los datos del usuario."
            );
        }

        normalizar(formulario);
        validarFormulario(formulario);

        Rol rolSeleccionado =
                rolRepository
                        .findById(formulario.getIdRol())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "El rol seleccionado no existe."
                                )
                        );

        validarRolPermitido(
                rolSeleccionado
        );

        EstadoUsuario estadoSeleccionado =
                convertirEstadoObligatorio(
                        formulario.getEstado()
                );

        if (formulario.esNuevo()) {

            return crearUsuario(
                    formulario,
                    rolSeleccionado,
                    estadoSeleccionado
            );
        }

        return actualizarUsuario(
                formulario,
                rolSeleccionado,
                estadoSeleccionado,
                correoAdministrador
        );
    }

    @Transactional
    public Usuario cambiarEstado(
            Integer idUsuario,
            String correoAdministrador
    ) {
        Usuario usuario =
                obtenerUsuario(idUsuario);

        if (esUsuarioActual(
                usuario,
                correoAdministrador
        )) {
            throw new IllegalStateException(
                    "No puedes desactivar tu propia cuenta."
            );
        }

        if (usuario.getEstado()
                == EstadoUsuario.ACTIVO) {

            validarNoDesactivarUltimoAdministrador(
                    usuario
            );

            usuario.setEstado(
                    EstadoUsuario.INACTIVO
            );

        } else {

            usuario.setEstado(
                    EstadoUsuario.ACTIVO
            );
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void restablecerContrasena(
            Integer idUsuario,
            String nuevaContrasena
    ) {
        Usuario usuario =
                obtenerUsuario(idUsuario);

        String contrasena =
                limpiarOpcional(
                        nuevaContrasena
                );

        validarContrasena(
                contrasena,
                true
        );

        usuario.setContrasena(
                passwordEncoder.encode(
                        contrasena
                )
        );

        usuario.setTipoRegistro(
                TipoRegistro.CUENTA
        );

        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario obtenerUsuario(
            Integer idUsuario
    ) {
        if (idUsuario == null) {
            throw new IllegalStateException(
                    "El usuario no es válido."
            );
        }

        return usuarioRepository
                .findWithRolByIdUsuario(idUsuario)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Usuario no encontrado."
                        )
                );
    }

    @Transactional(readOnly = true)
    public long contarUsuarios() {
        return usuarioRepository.count();
    }

    @Transactional(readOnly = true)
    public long contarActivos() {
        return usuarioRepository.countByEstado(
                EstadoUsuario.ACTIVO
        );
    }

    @Transactional(readOnly = true)
    public long contarInactivos() {
        return usuarioRepository.countByEstado(
                EstadoUsuario.INACTIVO
        );
    }

    @Transactional(readOnly = true)
    public long contarInvitados() {
        return usuarioRepository.countByTipoRegistro(
                TipoRegistro.INVITADO
        );
    }

    private Usuario crearUsuario(
            AdminUsuarioFormDTO formulario,
            Rol rol,
            EstadoUsuario estado
    ) {
        validarCorreoDuplicado(
                formulario.getCorreo(),
                null
        );

        validarDniDuplicado(
                formulario.getDni(),
                null
        );

        validarContrasena(
                formulario.getContrasena(),
                true
        );

        Usuario usuario =
                new Usuario();

        usuario.setRol(rol);
        usuario.setNombres(
                formulario.getNombres()
        );
        usuario.setApellidos(
                formulario.getApellidos()
        );
        usuario.setDni(
                formulario.getDni()
        );
        usuario.setTelefono(
                formulario.getTelefono()
        );
        usuario.setCorreo(
                formulario.getCorreo()
        );
        usuario.setContrasena(
                passwordEncoder.encode(
                        formulario.getContrasena()
                )
        );
        usuario.setEstado(estado);
        usuario.setTipoRegistro(
                TipoRegistro.CUENTA
        );

        return usuarioRepository.save(usuario);
    }

    private Usuario actualizarUsuario(
            AdminUsuarioFormDTO formulario,
            Rol rolSeleccionado,
            EstadoUsuario estadoSeleccionado,
            String correoAdministrador
    ) {
        Usuario usuario =
                obtenerUsuario(
                        formulario.getIdUsuario()
                );

        boolean esUsuarioActual =
                esUsuarioActual(
                        usuario,
                        correoAdministrador
                );

        if (esUsuarioActual) {

            if (!usuario.getRol()
                    .getIdRol()
                    .equals(
                            rolSeleccionado.getIdRol()
                    )) {

                throw new IllegalStateException(
                        "No puedes cambiar el rol "
                        + "de tu propia cuenta."
                );
            }

            if (estadoSeleccionado
                    == EstadoUsuario.INACTIVO) {

                throw new IllegalStateException(
                        "No puedes desactivar "
                        + "tu propia cuenta."
                );
            }
        }

        boolean dejaDeSerAdministrador =
                esAdministrador(usuario)
                && (
                !"ADMINISTRADOR".equalsIgnoreCase(
                        rolSeleccionado.getNombre()
                )
                || estadoSeleccionado
                == EstadoUsuario.INACTIVO
        );

        if (dejaDeSerAdministrador) {
            validarNoDesactivarUltimoAdministrador(
                    usuario
            );
        }

        validarCorreoDuplicado(
                formulario.getCorreo(),
                usuario.getIdUsuario()
        );

        validarDniDuplicado(
                formulario.getDni(),
                usuario.getIdUsuario()
        );

        usuario.setRol(rolSeleccionado);
        usuario.setNombres(
                formulario.getNombres()
        );
        usuario.setApellidos(
                formulario.getApellidos()
        );
        usuario.setDni(
                formulario.getDni()
        );
        usuario.setTelefono(
                formulario.getTelefono()
        );
        usuario.setCorreo(
                formulario.getCorreo()
        );
        usuario.setEstado(
                estadoSeleccionado
        );

        String nuevaContrasena =
                limpiarOpcional(
                        formulario.getContrasena()
                );

        if (nuevaContrasena != null) {

            validarContrasena(
                    nuevaContrasena,
                    true
            );

            usuario.setContrasena(
                    passwordEncoder.encode(
                            nuevaContrasena
                    )
            );

            usuario.setTipoRegistro(
                    TipoRegistro.CUENTA
            );
        }

        return usuarioRepository.save(usuario);
    }

    private void validarFormulario(
            AdminUsuarioFormDTO formulario
    ) {
        if (formulario.getIdRol() == null) {
            throw new IllegalStateException(
                    "Debes seleccionar un rol."
            );
        }

        validarNombre(
                formulario.getNombres(),
                "Los nombres"
        );

        validarNombre(
                formulario.getApellidos(),
                "Los apellidos"
        );

        if (formulario.getDni() == null
                || !formulario.getDni()
                .matches("\\d{8}")) {

            throw new IllegalStateException(
                    "El DNI debe contener "
                    + "exactamente 8 números."
            );
        }

        if (formulario.getTelefono() == null
                || !formulario.getTelefono()
                .matches("9\\d{8}")) {

            throw new IllegalStateException(
                    "El teléfono debe contener 9 números "
                    + "y comenzar con 9."
            );
        }

        if (formulario.getCorreo() == null
                || !formulario.getCorreo()
                .matches(PATRON_CORREO)) {

            throw new IllegalStateException(
                    "Ingresa un correo electrónico válido."
            );
        }

        if (formulario.getCorreo().length() > 120) {

            throw new IllegalStateException(
                    "El correo no puede superar "
                    + "120 caracteres."
            );
        }
    }

    private void validarNombre(
            String texto,
            String campo
    ) {
        if (texto == null || texto.isBlank()) {

            throw new IllegalStateException(
                    campo + " son obligatorios."
            );
        }

        if (texto.length() < 2
                || texto.length() > 50) {

            throw new IllegalStateException(
                    campo
                    + " deben contener entre "
                    + "2 y 50 caracteres."
            );
        }

        if (!texto.matches(PATRON_NOMBRE)) {

            throw new IllegalStateException(
                    campo
                    + " solo pueden contener letras, "
                    + "espacios, tildes, apóstrofes "
                    + "y guiones."
            );
        }
    }

    private void validarContrasena(
            String contrasena,
            boolean obligatoria
    ) {
        if (contrasena == null
                || contrasena.isBlank()) {

            if (obligatoria) {
                throw new IllegalStateException(
                        "La contraseña es obligatoria."
                );
            }

            return;
        }

        if (contrasena.length() < 8
                || contrasena.length() > 72) {

            throw new IllegalStateException(
                    "La contraseña debe contener "
                    + "entre 8 y 72 caracteres."
            );
        }

        if (!contrasena.matches(".*[A-Za-z].*")
                || !contrasena.matches(".*\\d.*")) {

            throw new IllegalStateException(
                    "La contraseña debe incluir "
                    + "al menos una letra y un número."
            );
        }
    }

    private void validarCorreoDuplicado(
            String correo,
            Integer idUsuario
    ) {
        boolean existe =
                idUsuario == null
                        ? usuarioRepository
                        .existsByCorreoIgnoreCase(correo)
                        : usuarioRepository
                        .existsByCorreoIgnoreCaseAndIdUsuarioNot(
                                correo,
                                idUsuario
                        );

        if (existe) {
            throw new IllegalStateException(
                    "El correo ya está registrado."
            );
        }
    }

    private void validarDniDuplicado(
            String dni,
            Integer idUsuario
    ) {
        boolean existe =
                idUsuario == null
                        ? usuarioRepository
                        .existsByDni(dni)
                        : usuarioRepository
                        .existsByDniAndIdUsuarioNot(
                                dni,
                                idUsuario
                        );

        if (existe) {
            throw new IllegalStateException(
                    "El DNI ya está registrado."
            );
        }
    }

    private void validarRolPermitido(
            Rol rol
    ) {
        if (!esRolDePersonal(rol)) {
            throw new IllegalStateException(
                    "Desde Administración solamente se pueden "
                    + "registrar usuarios con rol ADMINISTRADOR, "
                    + "CAJA o MESERO."
            );
        }
    }

    private boolean esRolDePersonal(
            Rol rol
    ) {
        if (rol == null || rol.getNombre() == null) {
            return false;
        }

        String nombre = rol.getNombre()
                .trim()
                .toUpperCase(Locale.ROOT);

        return nombre.equals("ADMINISTRADOR")
                || nombre.equals("CAJA")
                || nombre.equals("MESERO");
    }

    private void validarNoDesactivarUltimoAdministrador(
            Usuario usuario
    ) {
        if (!esAdministrador(usuario)
                || usuario.getEstado()
                != EstadoUsuario.ACTIVO) {

            return;
        }

        Rol rolAdministrador =
                rolRepository
                        .findByNombre("ADMINISTRADOR")
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el rol ADMINISTRADOR."
                                )
                        );

        long administradoresActivos =
                usuarioRepository
                        .buscarParaAdministracion(
                                null,
                                rolAdministrador.getIdRol(),
                                EstadoUsuario.ACTIVO,
                                TipoRegistro.CUENTA
                        )
                        .size();

        if (administradoresActivos <= 1) {

            throw new IllegalStateException(
                    "No puedes desactivar o cambiar "
                    + "el rol del último administrador activo."
            );
        }
    }

    private boolean esAdministrador(
            Usuario usuario
    ) {
        return usuario.getRol() != null
                && "ADMINISTRADOR".equalsIgnoreCase(
                        usuario.getRol().getNombre()
                );
    }

    private boolean esUsuarioActual(
            Usuario usuario,
            String correoAdministrador
    ) {
        return correoAdministrador != null
                && usuario.getCorreo() != null
                && usuario.getCorreo()
                .equalsIgnoreCase(
                        correoAdministrador.trim()
                );
    }

    private EstadoUsuario convertirEstadoObligatorio(
            String estado
    ) {
        if (estado == null || estado.isBlank()) {
            return EstadoUsuario.ACTIVO;
        }

        try {
            return EstadoUsuario.valueOf(
                    estado.trim().toUpperCase()
            );

        } catch (IllegalArgumentException e) {

            throw new IllegalStateException(
                    "El estado seleccionado no es válido."
            );
        }
    }

    private EstadoUsuario convertirEstadoOpcional(
            String estado
    ) {
        if (estado == null || estado.isBlank()) {
            return null;
        }

        try {
            return EstadoUsuario.valueOf(
                    estado.trim().toUpperCase()
            );

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TipoRegistro convertirTipoRegistroOpcional(
            String tipoRegistro
    ) {
        if (tipoRegistro == null
                || tipoRegistro.isBlank()) {

            return null;
        }

        try {
            return TipoRegistro.valueOf(
                    tipoRegistro.trim().toUpperCase()
            );

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void normalizar(
            AdminUsuarioFormDTO formulario
    ) {
        formulario.setNombres(
                limpiarEspacios(
                        formulario.getNombres()
                )
        );

        formulario.setApellidos(
                limpiarEspacios(
                        formulario.getApellidos()
                )
        );

        formulario.setDni(
                limpiarOpcional(
                        formulario.getDni()
                )
        );

        formulario.setTelefono(
                limpiarOpcional(
                        formulario.getTelefono()
                )
        );

        String correo =
                limpiarOpcional(
                        formulario.getCorreo()
                );

        formulario.setCorreo(
                correo != null
                        ? correo.toLowerCase(Locale.ROOT)
                        : null
        );

        formulario.setContrasena(
                limpiarOpcional(
                        formulario.getContrasena()
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

        String limpio =
                texto.trim()
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

        String limpio =
                texto.trim();

        return limpio.isBlank()
                ? null
                : limpio;
    }
}