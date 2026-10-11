package com.rinconcito.config;

import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Correo o contraseña incorrectos"));

        // INVITADO (sin cuenta real) no puede iniciar sesión
        if (usuario.getTipoRegistro() == Usuario.TipoRegistro.INVITADO) {
            throw new UsernameNotFoundException("Correo o contraseña incorrectos");
        }

        if (usuario.getContrasena() == null || usuario.getContrasena().isBlank()) {
            throw new UsernameNotFoundException("Correo o contraseña incorrectos");
        }

        if (usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            throw new UsernameNotFoundException("Correo o contraseña incorrectos");
        }

        boolean activo = usuario.getEstado() == Usuario.EstadoUsuario.ACTIVO;

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getContrasena())
                .disabled(!activo)
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre()))
                .build();
    }
}
