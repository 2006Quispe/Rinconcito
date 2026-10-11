package com.rinconcito.service;

import com.rinconcito.dto.CarritoItemDTO;
import com.rinconcito.entity.Producto;
import com.rinconcito.entity.Producto.EstadoProducto;
import com.rinconcito.repository.ProductoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class CarritoService {

    public static final String SESSION_KEY = "CARRITO_ITEMS";

    private static final int CANTIDAD_MAXIMA = 30;

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Obtiene el carrito almacenado en la sesión.
     * Si todavía no existe, crea uno vacío.
     */
    @SuppressWarnings("unchecked")
    public List<CarritoItemDTO> obtenerItems(HttpSession session) {

        Object carritoSesion = session.getAttribute(SESSION_KEY);

        if (carritoSesion instanceof List<?> lista) {
            return (List<CarritoItemDTO>) lista;
        }

        List<CarritoItemDTO> carritoNuevo = new ArrayList<>();
        session.setAttribute(SESSION_KEY, carritoNuevo);

        return carritoNuevo;
    }

    /**
     * Devuelve la cantidad total de unidades del carrito.
     */
    public int contarUnidades(HttpSession session) {

        return obtenerItems(session)
                .stream()
                .mapToInt(item ->
                        item.getCantidad() == null
                                ? 0
                                : item.getCantidad())
                .sum();
    }

    /**
     * Calcula el subtotal general del carrito.
     */
    public int subtotal(HttpSession session) {

        return obtenerItems(session)
                .stream()
                .mapToInt(CarritoItemDTO::getSubtotal)
                .sum();
    }

    /**
     * Agrega un producto al carrito.
     */
    public void agregar(
            HttpSession session,
            Integer idProducto,
            Integer cantidad) {

        if (idProducto == null) {
            throw new IllegalStateException("Producto no indicado.");
        }

        int cantidadSolicitada = cantidad == null ? 1 : cantidad;

        if (cantidadSolicitada <= 0) {
            throw new IllegalStateException(
                    "La cantidad debe ser mayor que cero.");
        }

        Producto producto = buscarProductoValido(idProducto);

        List<CarritoItemDTO> items = obtenerItems(session);

        for (CarritoItemDTO item : items) {

            if (idProducto.equals(item.getIdProducto())) {

                int cantidadActual =
                        item.getCantidad() == null
                                ? 0
                                : item.getCantidad();

                int nuevaCantidad =
                        cantidadActual + cantidadSolicitada;

                validarCantidadMaxima(nuevaCantidad);

                item.setCantidad(nuevaCantidad);
                actualizarDatosDelProducto(item, producto);

                return;
            }
        }

        validarCantidadMaxima(cantidadSolicitada);

        CarritoItemDTO nuevoItem = new CarritoItemDTO(
                producto.getIdProducto(),
                producto.getNombre(),
                producto.getImagen(),
                producto.getPrecio(),
                cantidadSolicitada
        );

        items.add(nuevoItem);
    }

    /**
     * Actualiza la cantidad de un producto.
     *
     * Si la cantidad es cero o negativa, elimina el producto.
     */
    public void actualizarCantidad(
            HttpSession session,
            Integer idProducto,
            Integer cantidad) {

        if (idProducto == null) {
            throw new IllegalStateException("Producto no indicado.");
        }

        if (cantidad == null || cantidad <= 0) {
            eliminar(session, idProducto);
            return;
        }

        validarCantidadMaxima(cantidad);

        Producto producto = buscarProductoValido(idProducto);

        List<CarritoItemDTO> items = obtenerItems(session);

        for (CarritoItemDTO item : items) {

            if (idProducto.equals(item.getIdProducto())) {

                item.setCantidad(cantidad);
                actualizarDatosDelProducto(item, producto);

                return;
            }
        }

        throw new IllegalStateException(
                "Ese producto no está en el carrito.");
    }

    /**
     * Elimina un producto del carrito.
     */
    public void eliminar(
            HttpSession session,
            Integer idProducto) {

        if (idProducto == null) {
            return;
        }

        List<CarritoItemDTO> items = obtenerItems(session);

        Iterator<CarritoItemDTO> iterator = items.iterator();

        while (iterator.hasNext()) {

            CarritoItemDTO item = iterator.next();

            if (idProducto.equals(item.getIdProducto())) {
                iterator.remove();
                return;
            }
        }
    }

    /**
     * Elimina todos los productos del carrito.
     */
    public void vaciar(HttpSession session) {

        session.setAttribute(
                SESSION_KEY,
                new ArrayList<CarritoItemDTO>()
        );
    }

    /**
     * Indica si el carrito está vacío.
     */
    public boolean estaVacio(HttpSession session) {

        return obtenerItems(session).isEmpty();
    }

    /**
     * Consulta el producto directamente desde MySQL
     * y comprueba que pueda venderse.
     */
    private Producto buscarProductoValido(Integer idProducto) {

        Producto producto = productoRepository
                .findById(idProducto)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "El producto no existe."));

        validarProductoParaCarrito(producto);

        return producto;
    }

    /**
     * Comprueba que el producto esté activo,
     * disponible y tenga un precio válido.
     */
    private void validarProductoParaCarrito(
            Producto producto) {

        if (producto.getEstado() != EstadoProducto.ACTIVO) {
            throw new IllegalStateException(
                    "El producto no está activo.");
        }

        if (!producto.isDisponible()) {
            throw new IllegalStateException(
                    "El producto está agotado.");
        }

        if (producto.getPrecio() == null
                || producto.getPrecio() <= 0) {

            throw new IllegalStateException(
                    "El producto no tiene un precio válido.");
        }
    }

    /**
     * Actualiza los datos visuales utilizando
     * la información actual del producto en MySQL.
     */
    private void actualizarDatosDelProducto(
            CarritoItemDTO item,
            Producto producto) {

        item.setNombre(producto.getNombre());
        item.setImagen(producto.getImagen());
        item.setPrecioUnitario(producto.getPrecio());
    }

    /**
     * Limita las compras accidentales o cantidades exageradas.
     */
    private void validarCantidadMaxima(int cantidad) {

        if (cantidad > CANTIDAD_MAXIMA) {
            throw new IllegalStateException(
                    "Solo puedes agregar hasta "
                            + CANTIDAD_MAXIMA
                            + " unidades por producto.");
        }
    }
}