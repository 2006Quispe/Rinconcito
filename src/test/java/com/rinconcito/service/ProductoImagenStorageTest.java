package com.rinconcito.service;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductoImagenStorageTest {
    @TempDir Path directorio;

    private ProductoImagenStorageService almacenamiento() {
        return new ProductoImagenStorageService(directorio.toString(), 3145728);
    }

    @Test
    void noBorraUrlsExternasNiImagenesEstaticas() throws Exception {
        Path imagen = Files.writeString(directorio.resolve("foto.jpg"), "original");
        for (String ruta : new String[] {null, "", "/img/foto.jpg",
                "https://images.unsplash.com/foto.jpg?w=600", "https\\://images.unsplash.com/foto.jpg",
                "/productos/imagenes/../foto.jpg", "/productos/imagenes/..\\foto.jpg",
                "/productos/imagenes/carpeta/foto.jpg", "/productos/imagenes/C:foto.jpg"}) {
            assertDoesNotThrow(() -> almacenamiento().eliminar(ruta));
            assertTrue(Files.exists(imagen));
        }
    }

    @Test
    void eliminaSoloLaImagenSubidaIndicada() throws Exception {
        Path imagen = Files.writeString(directorio.resolve("foto.jpg"), "original");
        Path otra = Files.writeString(directorio.resolve("otra.png"), "otra");
        almacenamiento().eliminar("/productos/imagenes/foto.jpg");
        assertFalse(Files.exists(imagen));
        assertTrue(Files.exists(otra));
        assertDoesNotThrow(() -> almacenamiento().eliminar("/productos/imagenes/foto.jpg"));
    }

    @Test
    void reemplazaFotoExternaSinPerderNuevaImagen() throws Exception {
        ProductoRepository productos = mock(ProductoRepository.class);
        CategoriaRepository categorias = mock(CategoriaRepository.class);
        when(categorias.findById(1)).thenReturn(Optional.of(new Categoria()));
        when(productos.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        ProductoService servicio = new ProductoService(productos, categorias, mock(UsuarioRepository.class),
                mock(HistorialDisponibilidadProductoRepository.class), almacenamiento());
        Producto producto = new Producto();
        producto.setNombre("Chicha morada");
        producto.setPrecio(6);
        producto.setImagen("https://images.unsplash.com/photo-1546173159-315724a31696?w=600");
        MockMultipartFile foto = new MockMultipartFile("imagenArchivo", "chicha.png", "image/png",
                Files.readAllBytes(Path.of("src/main/resources/static/img/pachamanca.png")));
        assertDoesNotThrow(() -> servicio.guardar(producto, 1, foto, false));
        assertTrue(producto.getImagen().startsWith("/productos/imagenes/"));
        assertTrue(Files.exists(directorio.resolve(producto.getImagen().substring("/productos/imagenes/".length()))));
        verify(productos).save(producto);
    }
}
