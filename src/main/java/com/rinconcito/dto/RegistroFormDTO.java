package com.rinconcito.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistroFormDTO {

    @NotBlank(message = "Los nombres son obligatorios.")
    @Size(
            min = 3,
            max = 25,
            message = "Los nombres deben tener entre 3 y 25 caracteres."
    )
    @Pattern(
            regexp = "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$",
            message = "Los nombres solo pueden contener letras, espacios, apóstrofos y guiones."
    )
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios.")
    @Size(
            min = 3,
            max = 25,
            message = "Los apellidos deben tener entre 3 y 25 caracteres."
    )
    @Pattern(
            regexp = "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$",
            message = "Los apellidos solo pueden contener letras, espacios, apóstrofos y guiones."
    )
    private String apellidos;

    @NotBlank(message = "El DNI es obligatorio.")
    @Pattern(
            regexp = "^[0-9]{8}$",
            message = "El DNI debe contener exactamente 8 números."
    )
    private String dni;

    @NotBlank(message = "El teléfono es obligatorio.")
    @Pattern(
            regexp = "^9[0-9]{8}$",
            message = "El teléfono debe comenzar con 9 y contener exactamente 9 números."
    )
    private String telefono;

    @NotBlank(message = "El correo es obligatorio.")
    @Size(
            min = 6,
            max = 120,
            message = "El correo debe tener entre 6 y 120 caracteres."
    )
    @Email(message = "Ingresa un correo electrónico válido.")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(
            min = 8,
            max = 20,
            message = "La contraseña debe tener entre 8 y 20 caracteres."
    )
    private String contrasena;

    @NotBlank(message = "Debes confirmar la contraseña.")
    @Size(
            min = 8,
            max = 20,
            message = "La confirmación debe tener entre 8 y 20 caracteres."
    )
    private String confirmarContrasena;

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getConfirmarContrasena() {
        return confirmarContrasena;
    }

    public void setConfirmarContrasena(
            String confirmarContrasena
    ) {
        this.confirmarContrasena = confirmarContrasena;
    }
}
