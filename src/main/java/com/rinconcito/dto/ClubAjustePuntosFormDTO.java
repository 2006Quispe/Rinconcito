package com.rinconcito.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClubAjustePuntosFormDTO {

    @NotBlank
    @Pattern(regexp = "\\d{8}")
    private String dni;

    @NotNull
    @Min(-1000000)
    @Max(1000000)
    private Integer puntos;

    @NotBlank
    @Size(min = 8, max = 200)
    private String motivo;

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public Integer getPuntos() { return puntos; }
    public void setPuntos(Integer puntos) { this.puntos = puntos; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
