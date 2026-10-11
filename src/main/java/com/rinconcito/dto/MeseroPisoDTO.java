package com.rinconcito.dto;

import com.rinconcito.entity.Piso;

import java.util.ArrayList;
import java.util.List;

public class MeseroPisoDTO {

    private final Piso piso;

    private final List<MeseroMesaDTO> mesas =
            new ArrayList<>();

    public MeseroPisoDTO(
            Piso piso
    ) {
        this.piso = piso;
    }

    public Piso getPiso() {
        return piso;
    }

    public List<MeseroMesaDTO> getMesas() {
        return mesas;
    }

    public void agregarMesa(
            MeseroMesaDTO mesa
    ) {
        mesas.add(mesa);
    }

    public long getDisponibles() {
        return mesas.stream()
                .filter(
                        MeseroMesaDTO::isDisponible
                )
                .count();
    }

    public long getOcupadas() {
        return mesas.stream()
                .filter(
                        MeseroMesaDTO::isOcupada
                )
                .count();
    }

    public long getReservadas() {
        return mesas.stream()
                .filter(
                        MeseroMesaDTO::isReservada
                )
                .count();
    }

    public long getInactivas() {
        return mesas.stream()
                .filter(
                        MeseroMesaDTO::isInactiva
                )
                .count();
    }
}