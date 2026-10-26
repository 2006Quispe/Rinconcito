package com.rinconcito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "historial_disponibilidad_producto",
        indexes = {
                @Index(
                        name = "idx_historial_disponibilidad_fecha",
                        columnList = "fecha_cambio"
                ),
                @Index(
                        name = "idx_historial_disponibilidad_producto",
                        columnList = "id_producto"
                ),
                @Index(
                        name = "idx_historial_disponibilidad_usuario",
                        columnList = "id_usuario"
                )
        }
)
public class HistorialDisponibilidadProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "disponible_anterior", nullable = false)
    private boolean disponibleAnterior;

    @Column(name = "disponible_nuevo", nullable = false)
    private boolean disponibleNuevo;

    @Column(
            name = "fecha_cambio",
            nullable = false,
            updatable = false
    )
    private LocalDateTime fechaCambio;

    public HistorialDisponibilidadProducto() {
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCambio == null) {
            fechaCambio = LocalDateTime.now();
        }
    }

    public Long getIdHistorial() {
        return idHistorial;
    }

    public void setIdHistorial(Long idHistorial) {
        this.idHistorial = idHistorial;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public boolean isDisponibleAnterior() {
        return disponibleAnterior;
    }

    public void setDisponibleAnterior(
            boolean disponibleAnterior
    ) {
        this.disponibleAnterior = disponibleAnterior;
    }

    public boolean isDisponibleNuevo() {
        return disponibleNuevo;
    }

    public void setDisponibleNuevo(
            boolean disponibleNuevo
    ) {
        this.disponibleNuevo = disponibleNuevo;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(
            LocalDateTime fechaCambio
    ) {
        this.fechaCambio = fechaCambio;
    }
}
