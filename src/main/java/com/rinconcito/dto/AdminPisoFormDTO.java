package com.rinconcito.dto;

public class AdminPisoFormDTO {

    private Integer idPiso;
    private String nombre;
    private String descripcion;
    private String estado = "ACTIVO";

    public AdminPisoFormDTO() {
    }

    public boolean esNuevo() {
        return idPiso == null;
    }

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

    public void setDescripcion(
            String descripcion
    ) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}