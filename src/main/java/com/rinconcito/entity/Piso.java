package com.rinconcito.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pisos")
public class Piso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_piso")
    private Integer idPiso;

    @Column(name = "nombre", nullable = false, length = 10)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPiso estado = EstadoPiso.ACTIVO;

    public enum EstadoPiso {
        ACTIVO, INACTIVO
    }

    public Piso() {
    }

    // Getters y setters

    public Integer getIdPiso() {
        return idPiso;
    }

    public void setIdPiso(Integer idPiso) {
        this.idPiso = idPiso;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoPiso getEstado() {
        return estado;
    }

    public void setEstado(EstadoPiso estado) {
        this.estado = estado;
    }
}