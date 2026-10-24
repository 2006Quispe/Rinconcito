package com.rinconcito.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroFormDTOValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void prepararValidador() {
        validatorFactory =
                Validation.buildDefaultValidatorFactory();

        validator =
                validatorFactory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        validatorFactory.close();
    }

    @Test
    void aceptaFormularioValido() {
        Set<ConstraintViolation<RegistroFormDTO>>
                errores =
                validator.validate(
                        formularioValido()
                );

        assertTrue(errores.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Billy123",
            "María@",
            "Jo",
            "Nombre Demasiado Extenso Para Registro"
    })
    void rechazaNombresInvalidos(
            String nombres
    ) {
        RegistroFormDTO formulario =
                formularioValido();

        formulario.setNombres(nombres);

        assertTrue(
                tieneErrorEn(
                        formulario,
                        "nombres"
                )
        );
    }

    @Test
    void aceptaNombresConTildesApostrofoYGuion() {
        RegistroFormDTO formulario =
                formularioValido();

        formulario.setNombres(
                "José-Luis"
        );

        formulario.setApellidos(
                "D'Angelo"
        );

        Set<ConstraintViolation<RegistroFormDTO>>
                errores =
                validator.validate(formulario);

        assertFalse(
                errores.stream().anyMatch(
                        error ->
                                error.getPropertyPath()
                                        .toString()
                                        .equals("nombres")
                                        || error.getPropertyPath()
                                        .toString()
                                        .equals("apellidos")
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1234567",
            "123456789",
            "12A45678",
            "1234-678"
    })
    void rechazaDniInvalido(
            String dni
    ) {
        RegistroFormDTO formulario =
                formularioValido();

        formulario.setDni(dni);

        assertTrue(
                tieneErrorEn(
                        formulario,
                        "dni"
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "91234567",
            "812345678",
            "9123456789",
            "91234A678"
    })
    void rechazaTelefonoInvalido(
            String telefono
    ) {
        RegistroFormDTO formulario =
                formularioValido();

        formulario.setTelefono(
                telefono
        );

        assertTrue(
                tieneErrorEn(
                        formulario,
                        "telefono"
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "correo",
            "correo@",
            "@correo.pe",
            "correo.demasiado.largo@example.com"
    })
    void rechazaCorreoInvalido(
            String correo
    ) {
        RegistroFormDTO formulario =
                formularioValido();

        formulario.setCorreo(correo);

        assertTrue(
                tieneErrorEn(
                        formulario,
                        "correo"
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1234567",
            "123456789012345678901"
    })
    void rechazaContrasenaFueraDelLimite(
            String contrasena
    ) {
        RegistroFormDTO formulario =
                formularioValido();

        formulario.setContrasena(
                contrasena
        );

        assertTrue(
                tieneErrorEn(
                        formulario,
                        "contrasena"
                )
        );
    }

    private boolean tieneErrorEn(
            RegistroFormDTO formulario,
            String campo
    ) {
        return validator
                .validate(formulario)
                .stream()
                .anyMatch(error ->
                        error.getPropertyPath()
                                .toString()
                                .equals(campo)
                );
    }

    private RegistroFormDTO formularioValido() {
        RegistroFormDTO dto =
                new RegistroFormDTO();

        dto.setNombres("Billy Joel");
        dto.setApellidos("Quispe Torres");
        dto.setDni("12345678");
        dto.setTelefono("912345678");
        dto.setCorreo("billy@example.com");
        dto.setContrasena("Clave#2026");
        dto.setConfirmarContrasena(
                "Clave#2026"
        );

        return dto;
    }
}
