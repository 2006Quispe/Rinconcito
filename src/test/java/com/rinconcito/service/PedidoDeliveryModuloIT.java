package com.rinconcito.service;

import com.rinconcito.dto.CarritoItemDTO;
import com.rinconcito.dto.PedidoFormDTO;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pago.EstadoPago;
import com.rinconcito.entity.Pago.MetodoPago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Producto;
import com.rinconcito.entity.Producto.EstadoProducto;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.ProductoRepository;
import com.rinconcito.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PedidoDeliveryModuloIT {

    @Autowired
    private CarritoService carritoService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private PagoPedidoService pagoPedidoService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private MockHttpSession session;
    private Producto producto;

    @BeforeEach
    void setUp() {

        session = new MockHttpSession();

        producto = productoRepository
                .findByEstado(EstadoProducto.ACTIVO)
                .stream()
                .filter(Producto::isDisponible)
                .filter(p -> p.getPrecio() != null && p.getPrecio() > 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Se necesita al menos un producto ACTIVO, "
                        + "disponible y con precio mayor a 0."
                ));
    }

    /*
     * Crea los datos necesarios para registrar
     * un pedido delivery de prueba.
     */
    private PedidoFormDTO formDelivery(String dni) {

        PedidoFormDTO dto = new PedidoFormDTO();

        dto.setDni(dni);
        dto.setNombres("Pedido");
        dto.setApellidos("Test");
        dto.setTelefono("999111222");
        dto.setDireccionTexto("Av. Ayacucho 123");
        dto.setDistrito("Huamanga");
        dto.setReferencia("Frente al parque");

        /*
         * Esta observación corresponde al pedido completo,
         * no a un producto individual.
         */
        dto.setObservaciones("Sin ají");

        return dto;
    }

    /*
     * Genera una imagen JPG pequeña para simular
     * la captura del pago por Yape o Plin.
     */
    private MockMultipartFile captura() {

        byte[] bytes = new byte[]{
                (byte) 0xFF,
                (byte) 0xD8,
                (byte) 0xFF,
                (byte) 0xD9
        };

        return new MockMultipartFile(
                "captura",
                "voucher.jpg",
                "image/jpeg",
                bytes
        );
    }

    /*
     * Busca un usuario que tenga el rol CAJA.
     */
    private Usuario caja() {

        return usuarioRepository
                .findByCorreo("caja@rinconcito.pe")
                .orElseGet(() -> usuarioRepository
                        .findAll()
                        .stream()
                        .filter(usuario ->
                                usuario.getRol() != null
                                && "CAJA".equalsIgnoreCase(
                                        usuario.getRol().getNombre()
                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No hay un usuario CAJA de prueba."
                                )
                        )
                );
    }

    @Test
    void flujoCarritoPedidoEvidenciaAprobar() {

        /*
         * Agregar dos unidades al carrito.
         */
        carritoService.agregar(
                session,
                producto.getIdProducto(),
                2
        );

        assertEquals(
                2,
                carritoService.contarUnidades(session)
        );

        assertTrue(
                carritoService.subtotal(session) > 0
        );

        /*
         * Crear el pedido usando los productos del carrito.
         */
        List<CarritoItemDTO> items =
                carritoService.obtenerItems(session);

        Pedido pedido = pedidoService.crearPedidoDelivery(
                formDelivery("41000001"),
                items,
                null
        );

        /*
         * Verificar los datos iniciales del pedido.
         */
        assertEquals(
                EstadoPedido.PENDIENTE_PAGO,
                pedido.getEstado()
        );

        assertNotNull(pedido.getCodigoPedido());

        assertTrue(
                pedido.getCodigoPedido()
                        .matches("PED-\\d{4}-\\d{6}")
        );

        assertEquals(
                0,
                pedido.getCostoDelivery()
        );

        assertEquals(
                producto.getPrecio() * 2,
                pedido.getSubtotal()
        );

        assertEquals(
                pedido.getSubtotal(),
                pedido.getTotal()
        );

        /*
         * El carrito todavía no debe vaciarse.
         * Se vacía después de registrar correctamente
         * la evidencia desde el controlador.
         */
        assertFalse(
                carritoService.estaVacio(session)
        );

        /*
         * Registrar evidencia de pago mediante Yape.
         */
        Pago pago = pagoPedidoService.registrarEvidencia(
                pedido.getCodigoPedido(),
                MetodoPago.YAPE,
                "OP-PED-41000001",
                captura()
        );

        assertEquals(
                EstadoPago.PENDIENTE_VALIDACION,
                pago.getEstado()
        );

        assertNotNull(
                pago.getEvidenciaPago()
        );

        /*
         * La base de datos guarda solamente el nombre
         * privado del archivo, no una ruta completa.
         */
        assertFalse(
                pago.getEvidenciaPago().contains("/")
        );

        /*
         * Después de subir la captura, Caja debe revisarla.
         */
        Pedido enRevision = pagoPedidoService
                .buscarPorCodigo(pedido.getCodigoPedido())
                .orElseThrow();

        assertEquals(
                EstadoPedido.PENDIENTE_VALIDACION,
                enRevision.getEstado()
        );

        /*
         * Caja aprueba el pago.
         */
        Pedido confirmado = pagoPedidoService.aprobar(
                pago.getIdPago(),
                caja()
        );

        assertEquals(
                EstadoPedido.CONFIRMADO,
                confirmado.getEstado()
        );

        Pago aprobado = pagoPedidoService
                .ultimoPagoDe(confirmado)
                .orElseThrow();

        assertEquals(
                EstadoPago.APROBADO,
                aprobado.getEstado()
        );

        assertNotNull(
                aprobado.getFechaValidacion()
        );

        assertNotNull(
                aprobado.getUsuarioValidador()
        );
    }

    @Test
    void rechazarPermiteReenvioDeEvidencia() {

        /*
         * Agregar un producto y crear el pedido.
         */
        carritoService.agregar(
                session,
                producto.getIdProducto(),
                1
        );

        Pedido pedido = pedidoService.crearPedidoDelivery(
                formDelivery("41000002"),
                carritoService.obtenerItems(session),
                null
        );

        /*
         * Registrar evidencia mediante Plin.
         */
        Pago pago = pagoPedidoService.registrarEvidencia(
                pedido.getCodigoPedido(),
                MetodoPago.PLIN,
                null,
                captura()
        );

        /*
         * Caja rechaza la captura.
         */
        pagoPedidoService.rechazar(
                pago.getIdPago(),
                caja(),
                "Captura borrosa"
        );

        Pedido otraVez = pagoPedidoService
                .buscarPorCodigo(pedido.getCodigoPedido())
                .orElseThrow();

        /*
         * El cliente debe poder volver a realizar
         * el envío de la evidencia.
         */
        assertEquals(
                EstadoPedido.PENDIENTE_PAGO,
                otraVez.getEstado()
        );

        Pago rechazo = pagoPedidoService
                .ultimoPagoDe(otraVez)
                .orElseThrow();

        assertEquals(
                EstadoPago.RECHAZADO,
                rechazo.getEstado()
        );

        assertEquals(
                "Captura borrosa",
                rechazo.getObservacionValidacion()
        );

        /*
         * El cliente vuelve a enviar otra captura.
         */
        Pago reenvio = pagoPedidoService.registrarEvidencia(
                pedido.getCodigoPedido(),
                MetodoPago.YAPE,
                "OP-REENVIO-41000002",
                captura()
        );

        assertEquals(
                EstadoPago.PENDIENTE_VALIDACION,
                reenvio.getEstado()
        );

        Pedido pedidoReenviado = pagoPedidoService
                .buscarPorCodigo(pedido.getCodigoPedido())
                .orElseThrow();

        assertEquals(
                EstadoPedido.PENDIENTE_VALIDACION,
                pedidoReenviado.getEstado()
        );
    }

    @Test
    void noConfiarEnPrecioDelCarritoUsaPrecioMySQL() {

        /*
         * Agregar un producto al carrito.
         */
        carritoService.agregar(
                session,
                producto.getIdProducto(),
                1
        );

        List<CarritoItemDTO> items =
                carritoService.obtenerItems(session);

        /*
         * Simular que alguien modifica el precio
         * guardado temporalmente en el carrito.
         */
        items.get(0).setPrecioUnitario(1);

        Pedido pedido = pedidoService.crearPedidoDelivery(
                formDelivery("41000003"),
                items,
                null
        );

        /*
         * PedidoService debe ignorar el precio manipulado
         * y consultar nuevamente el precio real de MySQL.
         */
        assertEquals(
                producto.getPrecio(),
                pedido.getSubtotal()
        );

        assertEquals(
                producto.getPrecio(),
                pedido.getTotal()
        );
    }
}