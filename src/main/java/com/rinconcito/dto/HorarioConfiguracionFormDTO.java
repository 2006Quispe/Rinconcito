package com.rinconcito.dto;

import java.util.ArrayList;
import java.util.List;

public class HorarioConfiguracionFormDTO {

    private List<HorarioAtencionFormDTO> horarios =
            new ArrayList<>();

    public HorarioConfiguracionFormDTO() {
    }

    public List<HorarioAtencionFormDTO> getHorarios() {
        return horarios;
    }

    public void setHorarios(
            List<HorarioAtencionFormDTO> horarios
    ) {
        this.horarios = horarios != null
                ? horarios
                : new ArrayList<>();
    }
}