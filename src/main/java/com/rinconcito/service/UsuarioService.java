package com.rinconcito.service;

import com.rinconcito.dto.RegistroFormDTO;
import com.rinconcito.entity.Rol;
import com.rinconcito.entity.Usuario;
import com.rinconcito.entity.Usuario.EstadoUsuario;
import com.rinconcito.entity.Usuario.TipoRegistro;
import com.rinconcito.repository.RolRepository;
import com.rinconcito.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioService {

    private static final String CORREO_CONSUMIDOR_FINAL =
            "consumidor.final@rinconcito.local";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario buscarOCrearInvitado(
            String dni,
            String nombres,
            String apellidos,
            String telefono
    ) {
        return buscarOCrearInvitado(
                dni,
                nombres,
                apellidos,
                telefono,
                null
        );
    }

    @Transactional
    public Usuario buscarOCrearInvitado(
            String dni,
            String nombres,
            String apellidos,
            String telefono,
            String correo
    ) {
        if (dni == null || dni.isBlank()) {
            throw new IllegalStateException(
                    "El DNI del cliente es obligatorio."
            );
        }

        String dniLimpio = dni.trim();

        return usuarioRepository
                .findByDni(dniLimpio)
                .map(existente -> {

                    if (
                            existente.getTipoRegistro()
                                    == TipoRegistro.INVITADO
                    ) {
                        existente.setNombres(
                                limpiarNombre(nombres)
                        );

                        existente.setApellidos(
                                limpiarNombre(apellidos)
                        );

                        existente.setTelefono(
                                limpiarTexto(telefono)
                        );

                        String correoLimpio =
                                normalizarCorreo(correo);

                        if (
                                correoLimpio != null
                                        && !correoLimpio.equalsIgnoreCase(
                                                existente.getCorreo()
                                        )
                                        && !usuarioRepository
                                        .existsByCorreoIgnoreCase(
                                                correoLimpio
                                        )
                        ) {
                            existente.setCorreo(
                                    correoLimpio
                            );
                        }

                        return usuarioRepository.save(
                                existente
                        );
                    }

                    return existente;
                })
                .orElseGet(() ->
                        crearInvitado(
                                dniLimpio,
                                nombres,
                                apellidos,
                                telefono,
                                correo
                        )
                );
    }

    @Transactional
    public Usuario obtenerOCrearConsumidorFinal() {

        Optional<Usuario> existente =
                usuarioRepository.findByCorreo(
                        CORREO_CONSUMIDOR_FINAL
                );

        if (existente.isPresent()) {

            Usuario consumidorFinal =
                    existente.get();

            boolean modificado = false;

            if (
                    consumidorFinal.getEstado()
                            != EstadoUsuario.ACTIVO
            ) {
                consumidorFinal.setEstado(
                        EstadoUsuario.ACTIVO
                );

                modificado = true;
            }

            if (consumidorFinal.getRol() == null) {

                consumidorFinal.setRol(
                        obtenerRolCliente()
                );

                modificado = true;
            }

            if (modificado) {
                return usuarioRepository.save(
                        consumidorFinal
                );
            }

            return consumidorFinal;
        }

        Usuario consumidorFinal =
                new Usuario();

        consumidorFinal.setRol(
                obtenerRolCliente()
        );

        consumidorFinal.setNombres(
                "Consumidor"
        );

        consumidorFinal.setApellidos(
                "Final"
        );

        consumidorFinal.setDni(null);
        consumidorFinal.setTelefono(null);

        consumidorFinal.setCorreo(
                CORREO_CONSUMIDOR_FINAL
        );

        consumidorFinal.setContrasena(
                passwordEncoder.encode(
                        UUID.randomUUID().toString()
                )
        );

        consumidorFinal.setTipoRegistro(
                TipoRegistro.INVITADO
        );

        consumidorFinal.setEstado(
                EstadoUsuario.ACTIVO
        );

        return usuarioRepository.save(
                consumidorFinal
        );
    }

    @Transactional
    public Usuario registrar(
            RegistroFormDTO dto
    ) {
        if (dto == null) {
            throw new IllegalStateException(
                    "No se recibieron los datos del usuario."
            );
        }

        if (
                !Objects.equals(
                        dto.getContrasena(),
                        dto.getConfirmarContrasena()
                )
        ) {
            throw new IllegalStateException(
                    "Las contraseñas no coinciden."
            );
        }

        String correo =
                normalizarCorreo(dto.getCorreo());

        if (correo == null) {
            throw new IllegalStateException(
                    "El correo es obligatorio."
            );
        }

        if (
                usuarioRepository
                        .existsByCorreoIgnoreCase(correo)
        ) {
            throw new IllegalStateException(
                    "Ese correo ya está registrado."
            );
        }

        String dni =
                limpiarTexto(dto.getDni());

        if (dni == null) {
            throw new IllegalStateException(
                    "El DNI es obligatorio."
            );
        }

        Optional<Usuario> existente =
                usuarioRepository.findByDni(dni);

        if (existente.isPresent()) {
            throw new IllegalStateException(
                    "Ese DNI ya está registrado. Si te pertenece, "
                            + "contacta al restaurante para verificar tu identidad."
            );
        }

        Usuario usuario = new Usuario();

        usuario.setRol(
                obtenerRolCliente()
        );

        usuario.setNombres(
                limpiarNombre(dto.getNombres())
        );

        usuario.setApellidos(
                limpiarNombre(dto.getApellidos())
        );

        usuario.setDni(dni);

        usuario.setTelefono(
                limpiarTexto(dto.getTelefono())
        );

        usuario.setTipoRegistro(
                TipoRegistro.CUENTA
        );

        usuario.setEstado(
                EstadoUsuario.ACTIVO
        );

        usuario.setCorreo(correo);

        usuario.setContrasena(
                passwordEncoder.encode(
                        dto.getContrasena()
                )
        );

        try {
            return usuarioRepository.save(
                    usuario
            );

        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "El correo o el DNI ya está registrado."
            );
        }
    }

    private Usuario crearInvitado(
            String dni,
            String nombres,
            String apellidos,
            String telefono,
            String correo
    ) {
        String correoLimpio =
                normalizarCorreo(correo);

        String correoFinal;

        if (
                correoLimpio != null
                        && !usuarioRepository
                        .existsByCorreoIgnoreCase(
                                correoLimpio
                        )
        ) {
            correoFinal = correoLimpio;

        } else {
            correoFinal =
                    dni
                            + "@invitado.rinconcito.pe";
        }

        Usuario usuario =
                new Usuario();

        usuario.setRol(
                obtenerRolCliente()
        );

        usuario.setNombres(
                limpiarNombre(nombres)
        );

        usuario.setApellidos(
                limpiarNombre(apellidos)
        );

        usuario.setDni(dni);

        usuario.setTelefono(
                limpiarTexto(telefono)
        );

        usuario.setCorreo(
                correoFinal
        );

        usuario.setContrasena(
                passwordEncoder.encode(
                        UUID.randomUUID().toString()
                )
        );

        usuario.setTipoRegistro(
                TipoRegistro.INVITADO
        );

        usuario.setEstado(
                EstadoUsuario.ACTIVO
        );

        return usuarioRepository.save(
                usuario
        );
    }

    private Rol obtenerRolCliente() {
        return rolRepository
                .findByNombre("CLIENTE")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No existe el rol CLIENTE "
                                        + "en la base de datos."
                        )
                );
    }

    private String limpiarNombre(
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

    private String normalizarCorreo(
            String correo
    ) {
        String limpio =
                limpiarTexto(correo);

        return limpio == null
                ? null
                : limpio.toLowerCase(
                        Locale.ROOT
                );
    }

    private String limpiarTexto(
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
