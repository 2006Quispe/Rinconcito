package com.rinconcito.dto;

import java.util.ArrayList;
import java.util.List;

public class PedidoFormDTO {

    private String tipoEntrega = "DELIVERY";

    private String correo;
    private String dni;
    private String nombres;
    private String apellidos;
    private String telefono;

    private String direccionTexto;
    private String distrito;
    private String referencia;

    private String observaciones;

    private List<ItemPedidoDTO> items =
            new ArrayList<>();

    public PedidoFormDTO() {
    }

    public String getTipoEntrega() {
        return tipoEntrega;
    }

    public void setTipoEntrega(String tipoEntrega) {
        this.tipoEntrega = tipoEntrega;
    }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccionTexto() {
        return direccionTexto;
    }

    public void setDireccionTexto(
            String direccionTexto
    ) {
        this.direccionTexto = direccionTexto;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(
            String referencia
    ) {
        this.referencia = referencia;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(
            String observaciones
    ) {
        this.observaciones = observaciones;
    }

    public List<ItemPedidoDTO> getItems() {
        return items;
    }

    public void setItems(
            List<ItemPedidoDTO> items
    ) {
        this.items = items != null
                ? items
                : new ArrayList<>();
    }

    public boolean esDelivery() {
        return "DELIVERY".equalsIgnoreCase(
                tipoEntrega
        );
    }

    public boolean esRecojoLocal() {
        return "RECOJO_LOCAL".equalsIgnoreCase(
                tipoEntrega
        );
    }
}