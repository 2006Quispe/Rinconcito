package com.rinconcito.service;

import com.rinconcito.entity.Galeria;
import com.rinconcito.entity.Galeria.EstadoGaleria;
import com.rinconcito.repository.GaleriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class GaleriaService {

    private static final int MAX_TITULO = 150;
    private static final int MAX_DESCRIPCION = 300;

    private final GaleriaRepository galeriaRepository;
    private final GaleriaImagenStorageService imagenStorageService;

    public GaleriaService(
            GaleriaRepository galeriaRepository,
            GaleriaImagenStorageService imagenStorageService
    ) {
        this.galeriaRepository =
                galeriaRepository;

        this.imagenStorageService =
                imagenStorageService;
    }

    @Transactional(readOnly = true)
    public List<Galeria> listarActivas() {
        return galeriaRepository
                .findByEstadoOrderByFechaPublicacionDesc(
                        EstadoGaleria.ACTIVO
                );
    }

    @Transactional(readOnly = true)
    public List<Galeria> listarTodasAdmin() {
        return galeriaRepository
                .findAllByOrderByFechaPublicacionDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Galeria> buscarPorId(
            Integer idImagen
    ) {
        return galeriaRepository.findById(idImagen);
    }

    @Transactional
    public Galeria guardar(
            Integer idImagen,
            String titulo,
            String descripcion,
            EstadoGaleria estado,
            MultipartFile archivoImagen
    ) {
        Galeria galeria;

        if (idImagen == null) {
            galeria = new Galeria();
        } else {
            galeria = galeriaRepository
                    .findById(idImagen)
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "La publicación de galería no existe."
                            )
                    );
        }

        validarDatos(
                galeria,
                titulo,
                descripcion,
                archivoImagen
        );

        galeria.setTitulo(
                textoOpcional(titulo)
        );

        galeria.setDescripcion(
                textoOpcional(descripcion)
        );

        galeria.setEstado(
                estado != null
                        ? estado
                        : EstadoGaleria.ACTIVO
        );

        String rutaAnterior =
                galeria.getRutaImagen();

        boolean tieneNuevaImagen =
                archivoImagen != null
                        && !archivoImagen.isEmpty();

        if (tieneNuevaImagen) {
            String nuevaRuta =
                    imagenStorageService.guardar(
                            archivoImagen
                    );

            galeria.setRutaImagen(nuevaRuta);
        }

        Galeria guardada =
                galeriaRepository.save(galeria);

        if (
                tieneNuevaImagen
                        && rutaAnterior != null
                        && !rutaAnterior.equals(
                                guardada.getRutaImagen()
                        )
        ) {
            imagenStorageService.eliminarPorRuta(
                    rutaAnterior
            );
        }

        return guardada;
    }

    @Transactional
    public void cambiarEstado(
            Integer idImagen,
            EstadoGaleria estado
    ) {
        if (estado == null) {
            throw new IllegalStateException(
                    "El estado es obligatorio."
            );
        }

        Galeria galeria =
                galeriaRepository
                        .findById(idImagen)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "La publicación de galería no existe."
                                )
                        );

        galeria.setEstado(estado);

        galeriaRepository.save(galeria);
    }

    @Transactional
    public void eliminar(
            Integer idImagen
    ) {
        Galeria galeria =
                galeriaRepository
                        .findById(idImagen)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "La publicación de galería no existe."
                                )
                        );

        String rutaImagen =
                galeria.getRutaImagen();

        galeriaRepository.delete(galeria);

        imagenStorageService.eliminarPorRuta(
                rutaImagen
        );
    }

    private void validarDatos(
            Galeria galeria,
            String titulo,
            String descripcion,
            MultipartFile archivoImagen
    ) {
        String tituloLimpio =
                textoOpcional(titulo);

        String descripcionLimpia =
                textoOpcional(descripcion);

        if (
                tituloLimpio != null
                        && tituloLimpio.length() > MAX_TITULO
        ) {
            throw new IllegalStateException(
                    "El título no puede superar los "
                            + MAX_TITULO
                            + " caracteres."
            );
        }

        if (
                descripcionLimpia != null
                        && descripcionLimpia.length()
                        > MAX_DESCRIPCION
        ) {
            throw new IllegalStateException(
                    "La descripción no puede superar los "
                            + MAX_DESCRIPCION
                            + " caracteres."
            );
        }

        boolean esNueva =
                galeria.getIdImagen() == null;

        boolean tieneImagenGuardada =
                galeria.getRutaImagen() != null
                        && !galeria
                        .getRutaImagen()
                        .isBlank();

        boolean tieneArchivoNuevo =
                archivoImagen != null
                        && !archivoImagen.isEmpty();

        if (
                esNueva
                        && !tieneImagenGuardada
                        && !tieneArchivoNuevo
        ) {
            throw new IllegalStateException(
                    "Debes seleccionar una imagen para la galería."
            );
        }
    }

    private String textoOpcional(
            String texto
    ) {
        if (
                texto == null
                        || texto.isBlank()
        ) {
            return null;
        }

        return texto.trim();
    }
}