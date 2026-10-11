package com.rinconcito.dto;

import java.io.Serializable;

/**
 * Producto almacenado temporalmente en el carrito de la sesión.
 *
 * El nombre, la imagen y el precio se utilizan para mostrar el producto.
 * Cuando se registra el pedido, el precio se consulta nuevamente en MySQL.
 */
public class CarritoItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer idProducto;
    private String nombre;
    private String imagen;
    private Integer precioUnitario;
    private Integer cantidad;

    public CarritoItemDTO() {
    }

    public CarritoItemDTO(
            Integer idProducto,
            String nombre,
            String imagen,
            Integer precioUnitario,
            Integer cantidad) {

        this.idProducto = idProducto;
        this.nombre = nombre;
        this.imagen = imagen;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
    }

    /**
     * Calcula el subtotal del producto.
     */
    public Integer getSubtotal() {
        if (precioUnitario == null || cantidad == null) {
            return 0;
        }

        return precioUnitario * cantidad;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Integer getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Integer precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}