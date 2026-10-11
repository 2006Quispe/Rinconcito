package com.rinconcito.controller;

import com.rinconcito.service.CarritoService;
import com.rinconcito.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/carrito")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    /*
     * Muestra todos los productos agregados al carrito.
     */
    @GetMapping
    public String verCarrito(Model model, HttpSession session) {

        model.addAttribute("titulo", "Mi carrito");
        model.addAttribute("items", carritoService.obtenerItems(session));
        model.addAttribute("subtotal", carritoService.subtotal(session));
        model.addAttribute(
                "mensajeTraslado",
                PedidoService.MENSAJE_TRASLADO
        );

        return "paginas/carrito";
    }

    /*
     * Agrega un producto al carrito.
     */
    @PostMapping("/agregar")
    public String agregarProducto(
            @RequestParam("idProducto") Integer idProducto,
            @RequestParam(value = "cantidad", defaultValue = "1") Integer cantidad,
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            carritoService.agregar(session, idProducto, cantidad);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Producto agregado al carrito."
            );

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        /*
         * Permite regresar a la página desde donde se agregó el producto,
         * por ejemplo, la carta.
         */
        if (esRedireccionSegura(redirect)) {
            return "redirect:" + redirect;
        }

        return "redirect:/carrito";
    }

    /*
     * Actualiza la cantidad de un producto.
     *
     * Este método será llamado automáticamente cuando el cliente presione
     * los botones + o - del nuevo carrito. Ya no tendrá botón "OK".
     */
    @PostMapping("/actualizar")
    public String actualizarCantidad(
            @RequestParam("idProducto") Integer idProducto,
            @RequestParam("cantidad") Integer cantidad,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            carritoService.actualizarCantidad(
                    session,
                    idProducto,
                    cantidad
            );

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/carrito";
    }

    /*
     * Elimina completamente un producto del carrito.
     */
    @PostMapping("/eliminar")
    public String eliminarProducto(
            @RequestParam("idProducto") Integer idProducto,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        carritoService.eliminar(session, idProducto);

        redirectAttributes.addFlashAttribute(
                "exito",
                "Producto eliminado del carrito."
        );

        return "redirect:/carrito";
    }

    /*
     * Elimina todos los productos del carrito.
     */
    @PostMapping("/vaciar")
    public String vaciarCarrito(
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        carritoService.vaciar(session);

        redirectAttributes.addFlashAttribute(
                "exito",
                "El carrito fue vaciado."
        );

        return "redirect:/carrito";
    }

    /*
     * Evita redirecciones hacia páginas externas.
     */
    private boolean esRedireccionSegura(String redirect) {
        return redirect != null
                && redirect.startsWith("/")
                && !redirect.startsWith("//");
    }
}