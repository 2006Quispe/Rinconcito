package com.rinconcito.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ClubRecompensaFormDTO {

    private Integer idRecompensa;

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{2,40}")
    @Size(max = 40)
    private String codigo;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotBlank
    @Size(max = 300)
    private String descripcion;

    @NotBlank
    private String tipo = "DESCUENTO_FIJO";

    @NotBlank
    private String categoria = "DESCUENTOS";

    @NotNull
    @Min(1)
    @Max(100000)
    private Integer costoPuntos = 100;

    @NotNull
    @Min(1)
    private Integer valor = 5;

    @NotNull
    @Min(0)
    private Integer compraMinima = 0;

    @Size(max = 500)
    private String condiciones;

    @Size(max = 255)
    private String imagen;

    @Min(0)
    private Integer stock;

    @NotNull
    @Min(1)
    @Max(3650)
    private Integer vigenciaDias = 90;

    private Integer idProducto;
    private LocalDate disponibleDesde;
    private LocalDate disponibleHasta;
    private boolean activo = true;

    public Integer getIdRecompensa() { return idRecompensa; }
    public void setIdRecompensa(Integer idRecompensa) { this.idRecompensa = idRecompensa; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public Integer getCostoPuntos() { return costoPuntos; }
    public void setCostoPuntos(Integer costoPuntos) { this.costoPuntos = costoPuntos; }
    public Integer getValor() { return valor; }
    public void setValor(Integer valor) { this.valor = valor; }
    public Integer getCompraMinima() { return compraMinima; }
    public void setCompraMinima(Integer compraMinima) { this.compraMinima = compraMinima; }
    public String getCondiciones() { return condiciones; }
    public void setCondiciones(String condiciones) { this.condiciones = condiciones; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getVigenciaDias() { return vigenciaDias; }
    public void setVigenciaDias(Integer vigenciaDias) { this.vigenciaDias = vigenciaDias; }
    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }
    public LocalDate getDisponibleDesde() { return disponibleDesde; }
    public void setDisponibleDesde(LocalDate disponibleDesde) { this.disponibleDesde = disponibleDesde; }
    public LocalDate getDisponibleHasta() { return disponibleHasta; }
    public void setDisponibleHasta(LocalDate disponibleHasta) { this.disponibleHasta = disponibleHasta; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
