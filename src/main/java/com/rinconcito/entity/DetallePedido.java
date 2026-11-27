package com.rinconcito.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "detalle_pedido")
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private Integer precioUnitario;

    @Column(name = "subtotal", nullable = false)
    private Integer subtotal;

    @Column(name = "nombre_producto", length = 100)
    private String nombreProducto;

    @Column(name = "es_recompensa_club", nullable = false)
    private boolean recompensaClub;

    @Column(name = "descuento_club", nullable = false)
    private Integer descuentoClub = 0;

    @Column(name = "observaciones", length = 200)
    private String observaciones;

    public DetallePedido() {
    }

    // Getters y setters

    public Integer getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(Integer idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Integer getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Integer precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Integer getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Integer subtotal) {
        this.subtotal = subtotal;
    }

    public String getNombreMostrado() {
        return nombreProducto != null && !nombreProducto.isBlank()
                ? nombreProducto : producto == null ? "Producto" : producto.getNombre();
    }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public boolean isRecompensaClub() { return recompensaClub; }
    public void setRecompensaClub(boolean recompensaClub) { this.recompensaClub = recompensaClub; }
    public Integer getDescuentoClub() { return descuentoClub == null ? 0 : descuentoClub; }
    public void setDescuentoClub(Integer descuentoClub) { this.descuentoClub = descuentoClub; }
    public Integer getSubtotalNeto() { return Math.max(0, (subtotal == null ? 0 : subtotal) - getDescuentoClub()); }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
