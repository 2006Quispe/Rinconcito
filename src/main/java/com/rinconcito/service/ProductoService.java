package com.rinconcito.service;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.entity.Producto.EstadoProducto;
import com.rinconcito.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public List<Producto> listarPorCategoria(Categoria categoria) {
        return productoRepository.findByCategoriaAndEstado(categoria, EstadoProducto.ACTIVO);
    }
}