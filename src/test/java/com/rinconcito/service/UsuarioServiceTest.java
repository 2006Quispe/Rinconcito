package com.rinconcito.service;

import com.rinconcito.dto.RegistroFormDTO;
import com.rinconcito.entity.Rol;
import com.rinconcito.entity.Usuario;
import com.rinconcito.entity.Usuario.EstadoUsuario;
import com.rinconcito.entity.Usuario.TipoRegistro;
import com.rinconcito.repository.RolRepository;
import com.rinconcito.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UsuarioService usuarioService;

    @ParameterizedTest
    @CsvSource({
            "CLIENTE, CUENTA, ACTIVO",
            "CLIENTE, CUENTA, INACTIVO",
            "MESERO, CUENTA, ACTIVO",
            "CAJA, CUENTA, ACTIVO",
            "ADMINISTRADOR, CUENTA, ACTIVO",
            "ADMINISTRADOR, CUENTA, INACTIVO",
            "CLIENTE, INVITADO, ACTIVO",
            "CLIENTE, INVITADO, INACTIVO"
    })
    void rechazaDniExistenteSinModificarTitular(
            String nombreRol, TipoRegistro tipo, EstadoUsuario estado) {
        Rol rol = new Rol();
        rol.setNombre(nombreRol);
        Usuario titular = new Usuario();
        titular.setRol(rol);
        titular.setTipoRegistro(tipo);
        titular.setEstado(estado);
        titular.setDni("12345678");
        titular.setCorreo("titular@example.com");
        titular.setContrasena("hash-original");
        titular.setNombres("Titular");
        titular.setApellidos("Original");
        titular.setTelefono("999111222");
        when(usuarioRepository.findByDni("12345678"))
                .thenReturn(Optional.of(titular));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> usuarioService.registrar(formulario()));

        assertTrue(error.getMessage().contains("DNI ya está registrado"));
        assertAll(
                () -> assertSame(rol, titular.getRol()),
                () -> assertEquals(tipo, titular.getTipoRegistro()),
                () -> assertEquals(estado, titular.getEstado()),
                () -> assertEquals("12345678", titular.getDni()),
                () -> assertEquals("titular@example.com", titular.getCorreo()),
                () -> assertEquals("hash-original", titular.getContrasena()),
                () -> assertEquals("Titular", titular.getNombres()),
                () -> assertEquals("Original", titular.getApellidos()),
                () -> assertEquals("999111222", titular.getTelefono())
        );
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(rolRepository, passwordEncoder);
    }

    @Test
    void registraDniNuevoComoClienteConContrasenaCodificada() {
        Rol cliente = new Rol();
        cliente.setNombre("CLIENTE");
        when(usuarioRepository.findByDni("12345678")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.encode("clave-nueva")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario creado = usuarioService.registrar(formulario());

        assertAll(
                () -> assertSame(cliente, creado.getRol()),
                () -> assertEquals(TipoRegistro.CUENTA, creado.getTipoRegistro()),
                () -> assertEquals(EstadoUsuario.ACTIVO, creado.getEstado()),
                () -> assertEquals("12345678", creado.getDni()),
                () -> assertEquals("nuevo@example.com", creado.getCorreo()),
                () -> assertEquals("hash-nuevo", creado.getContrasena())
        );
        verify(usuarioRepository).save(creado);
    }

    private RegistroFormDTO formulario() {
        RegistroFormDTO dto = new RegistroFormDTO();
        dto.setDni(" 12345678 ");
        dto.setNombres("Nuevo");
        dto.setApellidos("Usuario");
        dto.setTelefono("999333444");
        dto.setCorreo("nuevo@example.com");
        dto.setContrasena("clave-nueva");
        return dto;
    }
}
