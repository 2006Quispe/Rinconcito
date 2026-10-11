package com.rinconcito.service;

import com.rinconcito.dto.ConfiguracionRestauranteFormDTO;
import com.rinconcito.entity.ConfiguracionRestaurante;
import com.rinconcito.repository.ConfiguracionRestauranteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class ConfiguracionRestauranteService {

    private static final Pattern TELEFONO_PATTERN =
            Pattern.compile("^[0-9]{9}$");

    private static final Pattern CORREO_PATTERN =
            Pattern.compile(
                    "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            );

    private final ConfiguracionRestauranteRepository
            configuracionRestauranteRepository;

    public ConfiguracionRestauranteService(
            ConfiguracionRestauranteRepository
                    configuracionRestauranteRepository
    ) {
        this.configuracionRestauranteRepository =
                configuracionRestauranteRepository;
    }

    /*
     * Obtiene la configuración para las páginas públicas.
     */
    @Transactional(readOnly = true)
    public ConfiguracionRestaurante obtener() {
        return configuracionRestauranteRepository
                .findAll()
                .stream()
                .findFirst()
                .orElse(null);
    }

    /*
     * Obtiene los datos para el formulario del administrador.
     */
    @Transactional(readOnly = true)
    public ConfiguracionRestauranteFormDTO obtenerFormulario() {

        ConfiguracionRestaurante configuracion =
                configuracionRestauranteRepository
                        .findAll()
                        .stream()
                        .findFirst()
                        .orElse(null);

        ConfiguracionRestauranteFormDTO formulario =
                new ConfiguracionRestauranteFormDTO();

        if (configuracion == null) {
            return formulario;
        }

        formulario.setIdConfiguracion(
                configuracion.getIdConfiguracion()
        );

        formulario.setNombre(
                configuracion.getNombre()
        );

        formulario.setDireccion(
                configuracion.getDireccion()
        );

        formulario.setReferencia(
                configuracion.getReferencia()
        );

        formulario.setTelefono(
                configuracion.getTelefono()
        );

        formulario.setWhatsapp(
                configuracion.getWhatsapp()
        );

        formulario.setCorreo(
                configuracion.getCorreo()
        );

        formulario.setUrlFacebook(
                configuracion.getUrlFacebook()
        );

        formulario.setUrlInstagram(
                configuracion.getUrlInstagram()
        );

        formulario.setUrlGoogleMaps(
                configuracion.getUrlGoogleMaps()
        );

        formulario.setNumeroYape(
                configuracion.getNumeroYape()
        );

        formulario.setTitularYape(
                configuracion.getTitularYape()
        );

        formulario.setQrYapeActual(
                configuracion.getQrYape()
        );

        formulario.setNumeroPlin(
                configuracion.getNumeroPlin()
        );

        formulario.setTitularPlin(
                configuracion.getTitularPlin()
        );

        formulario.setQrPlinActual(
                configuracion.getQrPlin()
        );

        formulario.setBancoTransferencia(
                configuracion.getBancoTransferencia()
        );

        formulario.setTitularTransferencia(
                configuracion.getTitularTransferencia()
        );

        formulario.setNumeroCuentaTransferencia(
                configuracion.getNumeroCuentaTransferencia()
        );

        formulario.setCciTransferencia(
                configuracion.getCciTransferencia()
        );

        return formulario;
    }

    /*
     * Guarda los datos generales y los medios de pago.
     * Las imágenes QR se actualizarán desde un servicio separado.
     */
    @Transactional
    public ConfiguracionRestaurante guardar(
            ConfiguracionRestauranteFormDTO formulario
    ) {
        validar(formulario);

        ConfiguracionRestaurante configuracion =
                obtenerOCrearConfiguracion(
                        formulario.getIdConfiguracion()
                );

        configuracion.setNombre(
                normalizar(formulario.getNombre())
        );

        configuracion.setDireccion(
                normalizar(formulario.getDireccion())
        );

        configuracion.setReferencia(
                normalizarOpcional(
                        formulario.getReferencia()
                )
        );

        configuracion.setTelefono(
                normalizarOpcional(
                        formulario.getTelefono()
                )
        );

        configuracion.setWhatsapp(
                normalizarOpcional(
                        formulario.getWhatsapp()
                )
        );

        configuracion.setCorreo(
                normalizarOpcional(
                        formulario.getCorreo()
                )
        );

        configuracion.setUrlFacebook(
                normalizarOpcional(
                        formulario.getUrlFacebook()
                )
        );

        configuracion.setUrlInstagram(
                normalizarOpcional(
                        formulario.getUrlInstagram()
                )
        );

        configuracion.setUrlGoogleMaps(
                normalizarOpcional(
                        formulario.getUrlGoogleMaps()
                )
        );

        configuracion.setNumeroYape(
                normalizarOpcional(
                        formulario.getNumeroYape()
                )
        );

        configuracion.setTitularYape(
                normalizarOpcional(
                        formulario.getTitularYape()
                )
        );

        configuracion.setNumeroPlin(
                normalizarOpcional(
                        formulario.getNumeroPlin()
                )
        );

        configuracion.setTitularPlin(
                normalizarOpcional(
                        formulario.getTitularPlin()
                )
        );

        configuracion.setBancoTransferencia(
                normalizarOpcional(
                        formulario.getBancoTransferencia()
                )
        );

        configuracion.setTitularTransferencia(
                normalizarOpcional(
                        formulario.getTitularTransferencia()
                )
        );

        configuracion.setNumeroCuentaTransferencia(
                normalizarOpcional(
                        formulario.getNumeroCuentaTransferencia()
                )
        );

        configuracion.setCciTransferencia(
                normalizarOpcional(
                        formulario.getCciTransferencia()
                )
        );

        return configuracionRestauranteRepository.save(
                configuracion
        );
    }

    /*
     * Actualiza la ruta del código QR de Yape.
     */
    @Transactional
    public void actualizarQrYape(String rutaQr) {

        ConfiguracionRestaurante configuracion =
                obtenerConfiguracionObligatoria();

        configuracion.setQrYape(
                normalizarOpcional(rutaQr)
        );

        configuracionRestauranteRepository.save(
                configuracion
        );
    }

    /*
     * Actualiza la ruta del código QR de Plin.
     */
    @Transactional
    public void actualizarQrPlin(String rutaQr) {

        ConfiguracionRestaurante configuracion =
                obtenerConfiguracionObligatoria();

        configuracion.setQrPlin(
                normalizarOpcional(rutaQr)
        );

        configuracionRestauranteRepository.save(
                configuracion
        );
    }

    private ConfiguracionRestaurante obtenerOCrearConfiguracion(
            Integer idConfiguracion
    ) {
        if (idConfiguracion != null) {
            return configuracionRestauranteRepository
                    .findById(idConfiguracion)
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "La configuración no existe."
                            )
                    );
        }

        return configuracionRestauranteRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseGet(ConfiguracionRestaurante::new);
    }

    private ConfiguracionRestaurante
    obtenerConfiguracionObligatoria() {

        return configuracionRestauranteRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Primero debes guardar los datos "
                                        + "generales del restaurante."
                        )
                );
    }

    private void validar(
            ConfiguracionRestauranteFormDTO formulario
    ) {
        if (formulario == null) {
            throw new IllegalStateException(
                    "Los datos de configuración son obligatorios."
            );
        }

        validarObligatorio(
                formulario.getNombre(),
                "El nombre del restaurante es obligatorio.",
                150
        );

        validarObligatorio(
                formulario.getDireccion(),
                "La dirección del restaurante es obligatoria.",
                200
        );

        validarLongitud(
                formulario.getReferencia(),
                "La referencia",
                200
        );

        validarTelefono(
                formulario.getTelefono(),
                "El teléfono"
        );

        validarTelefono(
                formulario.getWhatsapp(),
                "El número de WhatsApp"
        );

        validarCorreo(
                formulario.getCorreo()
        );

        validarUrl(
                formulario.getUrlFacebook(),
                "La dirección de Facebook"
        );

        validarUrl(
                formulario.getUrlInstagram(),
                "La dirección de Instagram"
        );

        validarUrl(
                formulario.getUrlGoogleMaps(),
                "La dirección de Google Maps"
        );

        validarNumeroPago(
                formulario.getNumeroYape(),
                "El número de Yape"
        );

        validarNumeroPago(
                formulario.getNumeroPlin(),
                "El número de Plin"
        );

        validarLongitud(
                formulario.getTitularYape(),
                "El titular de Yape",
                150
        );

        validarLongitud(
                formulario.getTitularPlin(),
                "El titular de Plin",
                150
        );

        validarLongitud(
                formulario.getBancoTransferencia(),
                "El banco",
                100
        );

        validarLongitud(
                formulario.getTitularTransferencia(),
                "El titular de la transferencia",
                150
        );

        validarLongitud(
                formulario.getNumeroCuentaTransferencia(),
                "El número de cuenta",
                50
        );

        validarLongitud(
                formulario.getCciTransferencia(),
                "El CCI",
                30
        );
    }

    private void validarObligatorio(
            String valor,
            String mensaje,
            int longitudMaxima
    ) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(mensaje);
        }

        if (valor.trim().length() > longitudMaxima) {
            throw new IllegalStateException(
                    "El campo no puede superar "
                            + longitudMaxima
                            + " caracteres."
            );
        }
    }

    private void validarLongitud(
            String valor,
            String nombreCampo,
            int longitudMaxima
    ) {
        if (
                valor != null
                        && !valor.isBlank()
                        && valor.trim().length() > longitudMaxima
        ) {
            throw new IllegalStateException(
                    nombreCampo
                            + " no puede superar "
                            + longitudMaxima
                            + " caracteres."
            );
        }
    }

    private void validarTelefono(
            String telefono,
            String nombreCampo
    ) {
        if (telefono == null || telefono.isBlank()) {
            return;
        }

        String valor = telefono.trim();

        if (!TELEFONO_PATTERN.matcher(valor).matches()) {
            throw new IllegalStateException(
                    nombreCampo
                            + " debe contener exactamente 9 números."
            );
        }
    }

    private void validarNumeroPago(
            String numero,
            String nombreCampo
    ) {
        if (numero == null || numero.isBlank()) {
            return;
        }

        String valor = numero.trim();

        if (!TELEFONO_PATTERN.matcher(valor).matches()) {
            throw new IllegalStateException(
                    nombreCampo
                            + " debe contener exactamente 9 números."
            );
        }
    }

    private void validarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return;
        }

        String valor = correo.trim();

        if (valor.length() > 120) {
            throw new IllegalStateException(
                    "El correo no puede superar 120 caracteres."
            );
        }

        if (!CORREO_PATTERN.matcher(valor).matches()) {
            throw new IllegalStateException(
                    "El correo ingresado no es válido."
            );
        }
    }

    private void validarUrl(
            String url,
            String nombreCampo
    ) {
        if (url == null || url.isBlank()) {
            return;
        }

        String valor = url.trim();

        if (
                !valor.startsWith("http://")
                        && !valor.startsWith("https://")
        ) {
            throw new IllegalStateException(
                    nombreCampo
                            + " debe comenzar con http:// o https://."
            );
        }

        if (valor.length() > 500) {
            throw new IllegalStateException(
                    nombreCampo + " es demasiado extensa."
            );
        }
    }

    private String normalizar(String valor) {
        return valor.trim().replaceAll("\\s+", " ");
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}