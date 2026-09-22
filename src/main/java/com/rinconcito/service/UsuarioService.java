package com.rinconcito.service;

import com.rinconcito.entity.Rol;
import com.rinconcito.entity.Usuario;
import com.rinconcito.entity.Usuario.EstadoUsuario;
import com.rinconcito.repository.RolRepository;
import com.rinconcito.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Usuario buscarOCrearInvitado(String dni, String nombres, String apellidos, String telefono) {
        return usuarioRepository.findByDni(dni)
                .orElseGet(() -> crearInvitado(dni, nombres, apellidos, telefono));
    }

    private Usuario crearInvitado(String dni, String nombres, String apellidos, String telefono) {
        Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new IllegalStateException("No existe el rol CLIENTE en la base de datos"));

        Usuario usuario = new Usuario();
        usuario.setRol(rolCliente);
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setDni(dni);
        usuario.setTelefono(telefono);
        usuario.setCorreo(dni + "@invitado.rinconcito.pe");
        usuario.setContrasena(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setEstado(EstadoUsuario.ACTIVO);

        return usuarioRepository.save(usuario);
    }
}