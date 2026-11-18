package com.rinconcito.controller;

import com.rinconcito.entity.Producto;
import com.rinconcito.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminCartaIngredientesTest {
    @Test
    void recibeIngredientesAlCrearYAlEditarSinCambiarLaImagen() throws Exception {
        ProductoService servicio = mock(ProductoService.class);
        AdminCartaController controlador = new AdminCartaController();
        ReflectionTestUtils.setField(controlador, "productoService", servicio);
        MockMvc web = MockMvcBuilders.standaloneSetup(controlador).build();
        Producto existente = new Producto();
        existente.setIdProducto(1);
        existente.setImagen("/img/plato.png");
        when(servicio.buscarPorId(1)).thenReturn(Optional.of(existente));
        for (String id : new String[]{"", "1"}) {
            web.perform(multipart("/admin/carta/guardar")
                            .file(new MockMultipartFile("archivoImagen", new byte[0]))
                            .param("idProducto", id).param("nombre", "Pachamanca")
                            .param("precio", "75").param("idCategoria", "1")
                            .param("ingredientes", "Pollo, Papas").param("disponible", "true"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/carta"));
        }
        ArgumentCaptor<Producto> guardados = ArgumentCaptor.forClass(Producto.class);
        verify(servicio, times(2)).guardar(guardados.capture(), eq(1), any(), eq(false));
        assertEquals("Pollo, Papas", guardados.getAllValues().get(0).getIngredientes());
        assertEquals("Pollo, Papas", existente.getIngredientes());
        assertEquals("/img/plato.png", existente.getImagen());
    }
}
