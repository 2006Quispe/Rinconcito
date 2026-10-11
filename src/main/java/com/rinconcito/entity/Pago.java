package com.rinconcito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    /*
     * Un pago puede pertenecer a un pedido.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido")
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva")
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false)
    private MetodoPago metodoPago;
    
    @Column(name = "monto", nullable = false)
    private Integer monto;

    @Column(
        name = "numero_operacion",
        unique = true,
        length = 50
    )
    private String numeroOperacion;

    /*
     * Guarda el nombre o ruta privada de la captura.
     * La imagen no se almacena directamente en MySQL.
     */
    @Column(name = "evidencia_pago", length = 255)
    private String evidenciaPago;

    /*
     * Estado de revisión del pago.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPago estado =
            EstadoPago.PENDIENTE_VALIDACION;

    /*
     * Usuario de Caja que aprueba o rechaza el pago.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_validador")
    private Usuario usuarioValidador;

    @Column(
        name = "fecha_pago",
        nullable = false,
        updatable = false
    )
    private LocalDateTime fechaPago;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    /*
     * Motivo escrito por Caja cuando rechaza
     * una evidencia de pago.
     */
    @Column(
        name = "observacion_validacion",
        length = 300
    )
    private String observacionValidacion;

    /*
     * Métodos de pago disponibles.
     *
     * EFECTIVO se conserva para los pedidos presenciales
     * registrados por el personal del restaurante.
     */
    public enum MetodoPago {
        YAPE,
        PLIN,
        TRANSFERENCIA,
        EFECTIVO
    }

    /*
     * Estados posibles de un pago.
     */
    public enum EstadoPago {
        PENDIENTE_VALIDACION,
        APROBADO,
        RECHAZADO
    }

    public Pago() {
    }

    /*
     * Registra automáticamente la fecha del pago antes de guardar por primera vez.
     */
    @PrePersist
    protected void onCreate() {

        if (fechaPago == null) {
            fechaPago = LocalDateTime.now();
        }

        if (estado == null) {
            estado = EstadoPago.PENDIENTE_VALIDACION;
        }
    }

    public Integer getIdPago() {
        return idPago;
    }

    public void setIdPago(Integer idPago) {
        this.idPago = idPago;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(
            MetodoPago metodoPago
    ) {
        this.metodoPago = metodoPago;
    }

    public Integer getMonto() {
        return monto;
    }

    public void setMonto(Integer monto) {
        this.monto = monto;
    }

    public String getNumeroOperacion() {
        return numeroOperacion;
    }

    public void setNumeroOperacion(
            String numeroOperacion
    ) {
        this.numeroOperacion = numeroOperacion;
    }

    public String getEvidenciaPago() {
        return evidenciaPago;
    }

    public void setEvidenciaPago(
            String evidenciaPago
    ) {
        this.evidenciaPago = evidenciaPago;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(
            EstadoPago estado
    ) {
        this.estado = estado;
    }

    public Usuario getUsuarioValidador() {
        return usuarioValidador;
    }

    public void setUsuarioValidador(
            Usuario usuarioValidador
    ) {
        this.usuarioValidador =
                usuarioValidador;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(
            LocalDateTime fechaPago
    ) {
        this.fechaPago = fechaPago;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    public void setFechaValidacion(
            LocalDateTime fechaValidacion
    ) {
        this.fechaValidacion =
                fechaValidacion;
    }

    public String getObservacionValidacion() {
        return observacionValidacion;
    }

    public void setObservacionValidacion(
            String observacionValidacion
    ) {
        this.observacionValidacion =
                observacionValidacion;
    }
}