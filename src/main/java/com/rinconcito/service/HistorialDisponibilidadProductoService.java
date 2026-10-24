package com.rinconcito.service;

import com.rinconcito.entity.HistorialDisponibilidadProducto;
import com.rinconcito.repository.HistorialDisponibilidadProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistorialDisponibilidadProductoService {

    private final HistorialDisponibilidadProductoRepository
            historialRepository;

    public HistorialDisponibilidadProductoService(
            HistorialDisponibilidadProductoRepository
                    historialRepository
    ) {
        this.historialRepository = historialRepository;
    }

    @Transactional(readOnly = true)
    public List<HistorialDisponibilidadProducto>
    listarRecientes() {
        return historialRepository
                .findTop200ByOrderByFechaCambioDesc();
    }
}
