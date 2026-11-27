package com.rinconcito.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ClubConfiguracionFormDTO {

    @NotNull @Min(1) @Max(1000)
    private Integer puntosPorSol = 1;

    @NotNull @Min(1) @Max(3650)
    private Integer vigenciaPuntosDias = 365;

    @NotNull @Min(1) @Max(3650)
    private Integer vigenciaRecompensasDias = 90;

    public Integer getPuntosPorSol() { return puntosPorSol; }
    public void setPuntosPorSol(Integer puntosPorSol) { this.puntosPorSol = puntosPorSol; }
    public Integer getVigenciaPuntosDias() { return vigenciaPuntosDias; }
    public void setVigenciaPuntosDias(Integer vigenciaPuntosDias) { this.vigenciaPuntosDias = vigenciaPuntosDias; }
    public Integer getVigenciaRecompensasDias() { return vigenciaRecompensasDias; }
    public void setVigenciaRecompensasDias(Integer vigenciaRecompensasDias) { this.vigenciaRecompensasDias = vigenciaRecompensasDias; }
}
