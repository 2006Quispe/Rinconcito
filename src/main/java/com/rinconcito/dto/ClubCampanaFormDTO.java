package com.rinconcito.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ClubCampanaFormDTO {

    private Long idCampana;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotNull
    @Min(1)
    @Max(500)
    private Integer porcentajeBonus = 50;

    @NotNull
    private LocalDate fechaInicio;
    @NotNull
    private LocalDate fechaFin;
    private boolean activa = true;

    public Long getIdCampana() { return idCampana; }
    public void setIdCampana(Long idCampana) { this.idCampana = idCampana; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getPorcentajeBonus() { return porcentajeBonus; }
    public void setPorcentajeBonus(Integer porcentajeBonus) { this.porcentajeBonus = porcentajeBonus; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
