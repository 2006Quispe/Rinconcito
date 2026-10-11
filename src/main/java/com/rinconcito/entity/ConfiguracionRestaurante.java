package com.rinconcito.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_restaurante")
public class ConfiguracionRestaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Integer idConfiguracion;

    @Column(name = "nombre", length = 150)
    private String nombre;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "referencia", length = 200)
    private String referencia;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "whatsapp", length = 20)
    private String whatsapp;

    @Column(name = "correo", length = 120)
    private String correo;

    @Column(name = "url_facebook", length = 255)
    private String urlFacebook;

    @Column(name = "url_instagram", length = 255)
    private String urlInstagram;

    @Column(name = "url_google_maps", length = 500)
    private String urlGoogleMaps;

    @Column(name = "numero_yape", length = 20)
    private String numeroYape;

    @Column(name = "titular_yape", length = 150)
    private String titularYape;

    @Column(name = "qr_yape", length = 255)
    private String qrYape;

    @Column(name = "numero_plin", length = 20)
    private String numeroPlin;

    @Column(name = "titular_plin", length = 150)
    private String titularPlin;

    @Column(name = "qr_plin", length = 255)
    private String qrPlin;

    @Column(name = "banco_transferencia", length = 100)
    private String bancoTransferencia;

    @Column(name = "titular_transferencia", length = 150)
    private String titularTransferencia;

    @Column(name = "numero_cuenta_transferencia", length = 50)
    private String numeroCuentaTransferencia;

    @Column(name = "cci_transferencia", length = 30)
    private String cciTransferencia;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public ConfiguracionRestaurante() {
    }

    @PrePersist
    protected void antesDeGuardar() {
        if (actualizadoEn == null) {
            actualizadoEn = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void antesDeActualizar() {
        actualizadoEn = LocalDateTime.now();
    }

    public Integer getIdConfiguracion() {
        return idConfiguracion;
    }

    public void setIdConfiguracion(Integer idConfiguracion) {
        this.idConfiguracion = idConfiguracion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getUrlFacebook() {
        return urlFacebook;
    }

    public void setUrlFacebook(String urlFacebook) {
        this.urlFacebook = urlFacebook;
    }

    public String getUrlInstagram() {
        return urlInstagram;
    }

    public void setUrlInstagram(String urlInstagram) {
        this.urlInstagram = urlInstagram;
    }

    public String getUrlGoogleMaps() {
        return urlGoogleMaps;
    }

    public void setUrlGoogleMaps(String urlGoogleMaps) {
        this.urlGoogleMaps = urlGoogleMaps;
    }

    public String getNumeroYape() {
        return numeroYape;
    }

    public void setNumeroYape(String numeroYape) {
        this.numeroYape = numeroYape;
    }

    public String getTitularYape() {
        return titularYape;
    }

    public void setTitularYape(String titularYape) {
        this.titularYape = titularYape;
    }

    public String getQrYape() {
        return qrYape;
    }

    public void setQrYape(String qrYape) {
        this.qrYape = qrYape;
    }

    public String getNumeroPlin() {
        return numeroPlin;
    }

    public void setNumeroPlin(String numeroPlin) {
        this.numeroPlin = numeroPlin;
    }

    public String getTitularPlin() {
        return titularPlin;
    }

    public void setTitularPlin(String titularPlin) {
        this.titularPlin = titularPlin;
    }

    public String getQrPlin() {
        return qrPlin;
    }

    public void setQrPlin(String qrPlin) {
        this.qrPlin = qrPlin;
    }

    public String getBancoTransferencia() {
        return bancoTransferencia;
    }

    public void setBancoTransferencia(String bancoTransferencia) {
        this.bancoTransferencia = bancoTransferencia;
    }

    public String getTitularTransferencia() {
        return titularTransferencia;
    }

    public void setTitularTransferencia(String titularTransferencia) {
        this.titularTransferencia = titularTransferencia;
    }

    public String getNumeroCuentaTransferencia() {
        return numeroCuentaTransferencia;
    }

    public void setNumeroCuentaTransferencia(
            String numeroCuentaTransferencia
    ) {
        this.numeroCuentaTransferencia =
                numeroCuentaTransferencia;
    }

    public String getCciTransferencia() {
        return cciTransferencia;
    }

    public void setCciTransferencia(String cciTransferencia) {
        this.cciTransferencia = cciTransferencia;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(LocalDateTime actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}