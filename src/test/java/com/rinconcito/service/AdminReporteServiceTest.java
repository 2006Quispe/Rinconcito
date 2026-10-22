package com.rinconcito.service;

import com.rinconcito.dto.AdminReporteDTO;
import com.rinconcito.dto.ReporteProductoDTO;
import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Producto;
import com.rinconcito.repository.DetallePedidoRepository;
import com.rinconcito.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReporteServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 21);

    @Mock private PagoRepository pagoRepository;
    @Mock private DetallePedidoRepository detallePedidoRepository;
    @InjectMocks private AdminReporteService service;

    private Pedido pedido;

    @BeforeEach
    void prepararPedido() {
        pedido = new Pedido();
        pedido.setIdPedido(1);
    }

    @Test
    void sumaUnidadesDeUnMismoProductoEnPedidosDistintos() {
        Pedido segundo = new Pedido();
        segundo.setIdPedido(2);
        operaciones(List.of(pago(pedido, Pago.EstadoPago.APROBADO),
                pago(segundo, Pago.EstadoPago.APROBADO)));
        when(detallePedidoRepository.findByPedidoIn(List.of(pedido, segundo)))
                .thenReturn(List.of(detalle(10, "Pachamanca", 3, 90),
                        detalle(10, "Pachamanca", 2, 60)));

        List<ReporteProductoDTO> productos = generar().getProductosVendidos();

        assertEquals(1, productos.size());
        assertProducto(productos.getFirst(), "Pachamanca", 5L, 150L);
    }

    @Test
    void cuentaCadaPedidoAprobadoUnaVezYExcluyeOtrosEstados() {
        Pedido rechazado = new Pedido();
        rechazado.setIdPedido(2);
        Pedido pendiente = new Pedido();
        pendiente.setIdPedido(3);
        operaciones(List.of(pago(pedido, Pago.EstadoPago.APROBADO),
                pago(pedido, Pago.EstadoPago.APROBADO),
                pago(rechazado, Pago.EstadoPago.RECHAZADO),
                pago(pendiente, Pago.EstadoPago.PENDIENTE_VALIDACION)));
        when(detallePedidoRepository.findByPedidoIn(List.of(pedido)))
                .thenReturn(List.of(detalle(10, "Pachamanca", 2, 60)));

        AdminReporteDTO reporte = generar();

        assertProducto(reporte.getProductosVendidos().getFirst(), "Pachamanca", 2L, 60L);
        assertEquals(1L, reporte.getRechazados());
        assertEquals(1L, reporte.getPendientes());
        verify(detallePedidoRepository).findByPedidoIn(List.of(pedido));
        verifyNoMoreInteractions(detallePedidoRepository);
        verify(pagoRepository).listarOperacionesParaReporte(
                FECHA.atStartOfDay(), FECHA.plusDays(1).atStartOfDay());
    }

    @Test
    void ordenaPorUnidadesYDesempataPorMontoLimitandoADiez() {
        List<DetallePedido> detalles = new ArrayList<>();
        detalles.add(detalle(100, "Mas unidades", 20, 1));
        detalles.add(detalle(101, "Mayor monto", 12, 500));
        detalles.add(detalle(102, "Menor monto", 12, 100));
        for (int id = 1; id <= 9; id++) {
            detalles.add(detalle(id, "Producto " + id, id, id * 10));
        }
        detallesDePedidoAprobado(detalles);

        List<ReporteProductoDTO> productos = generar().getProductosVendidos();

        assertEquals(10, productos.size());
        assertEquals(List.of("Mas unidades", "Mayor monto", "Menor monto",
                        "Producto 9", "Producto 8", "Producto 7", "Producto 6",
                        "Producto 5", "Producto 4", "Producto 3"),
                productos.stream().map(ReporteProductoDTO::getProducto).toList());
    }

    @Test
    void conservaConteosLongSuperioresAlLimiteDeInteger() {
        detallesDePedidoAprobado(List.of(
                detalle(10, "Pachamanca", Integer.MAX_VALUE, Integer.MAX_VALUE),
                detalle(10, "Pachamanca", 2, 60)));

        assertProducto(generar().getProductosVendidos().getFirst(),
                "Pachamanca", (long) Integer.MAX_VALUE + 2,
                (long) Integer.MAX_VALUE + 60);
    }

    @Test
    void conservaTratamientoDeNulosYProductosSinIdentificador() {
        DetallePedido sinProducto = new DetallePedido();
        detallesDePedidoAprobado(List.of(sinProducto,
                detalle(null, "Sin ID", 5, 100),
                detalle(10, " ", null, null),
                detalle(11, "Cuy", 2, 80)));

        List<ReporteProductoDTO> productos = generar().getProductosVendidos();

        assertEquals(2, productos.size());
        assertProducto(productos.getFirst(), "Cuy", 2, 80);
        assertProducto(productos.getLast(), "Producto", 0, 0);
    }

    @Test
    void sinPedidosAprobadosNoConsultaDetalles() {
        operaciones(List.of(pago(pedido, Pago.EstadoPago.RECHAZADO)));

        assertTrue(generar().getProductosVendidos().isEmpty());
        verifyNoInteractions(detallePedidoRepository);
    }

    private AdminReporteDTO generar() {
        return service.generar(FECHA, FECHA);
    }

    private void operaciones(List<Pago> pagos) {
        when(pagoRepository.listarOperacionesParaReporte(
                FECHA.atStartOfDay(), FECHA.plusDays(1).atStartOfDay()))
                .thenReturn(pagos);
    }

    private void detallesDePedidoAprobado(List<DetallePedido> detalles) {
        operaciones(List.of(pago(pedido, Pago.EstadoPago.APROBADO)));
        when(detallePedidoRepository.findByPedidoIn(List.of(pedido))).thenReturn(detalles);
    }

    private Pago pago(Pedido pedido, Pago.EstadoPago estado) {
        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setEstado(estado);
        pago.setMonto(100);
        pago.setMetodoPago(Pago.MetodoPago.YAPE);
        pago.setFechaPago(FECHA.atTime(12, 0));
        return pago;
    }

    private DetallePedido detalle(Integer id, String nombre, Integer cantidad, Integer subtotal) {
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setNombre(nombre);
        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setSubtotal(subtotal);
        return detalle;
    }

    private void assertProducto(ReporteProductoDTO producto, String nombre, long cantidad, long monto) {
        assertAll(
                () -> assertEquals(nombre, producto.getProducto()),
                () -> assertEquals(cantidad, producto.getCantidad()),
                () -> assertEquals(monto, producto.getMonto())
        );
    }
}
