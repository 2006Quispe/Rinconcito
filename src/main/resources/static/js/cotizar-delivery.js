(() => {
    const panel = document.getElementById('avisoDelivery');
    const form = panel?.closest('form');
    if (!form) return;
    const {latitud, longitud, distrito, direccionTexto} = form.elements;
    const aviso = document.getElementById('cotizacionDelivery');
    const costo = document.getElementById('importeDelivery');
    const total = document.getElementById('importeTotal');
    const prueba = document.getElementById('probarUbicacion');
    let solicitud;
    let tarifaValida = false;
    let faltaDistritoDeUbicacion = false;

    const esDelivery = () => form.elements.tipoEntrega.value === 'DELIVERY';

    function render(tarifa) {
        tarifaValida = tarifa !== null;
        costo.textContent = tarifa === null ? 'Por calcular' : 'S/ ' + tarifa.toFixed(2);
        total.textContent = tarifa === null ? 'Por calcular' : 'S/ ' + (Number(panel.dataset.subtotal) + tarifa).toFixed(2);
    }

    function iniciar(mensaje) {
        solicitud?.abort();
        solicitud = new AbortController();
        aviso.textContent = mensaje;
        render(null);
        return solicitud.signal;
    }

    async function consultar(url, datos, signal) {
        const body = new URLSearchParams(datos);
        const csrf = form.elements._csrf;
        if (csrf) body.set(csrf.name, csrf.value);
        const respuesta = await fetch(url, {method: 'POST', body, signal});
        const resultado = await respuesta.json();
        if (!respuesta.ok) throw new Error(resultado.error || 'Intenta nuevamente.');
        return resultado;
    }

    async function cotizar() {
        solicitud?.abort();
        if (!esDelivery()) {
            render(0);
            return;
        }
        if (!distrito.value.trim() || !latitud.value || !longitud.value) {
            render(null);
            aviso.textContent = '';
            return;
        }
        const signal = iniciar('Calculando el envío…');
        try {
            const resultado = await consultar(panel.dataset.cotizar, {
                distrito: distrito.value.trim(), latitud: latitud.value, longitud: longitud.value
            }, signal);
            if (signal.aborted) return;
            render(Number(resultado.costo));
            aviso.textContent = 'Envío calculado.';
        } catch (error) {
            if (!signal.aborted) aviso.textContent = error instanceof SyntaxError ? 'No se pudo cotizar el envío.' : error.message;
        }
    }

    async function ubicar(datos, signal) {
        try {
            const resultado = await consultar(panel.dataset.ubicar, datos, signal);
            if (signal.aborted) return;
            if (datos.latitud !== undefined || datos.distanciaDemo !== undefined) {
                direccionTexto.value = resultado.direccion;
                distrito.value = resultado.distrito || '';
                faltaDistritoDeUbicacion = !distrito.value;
            }
            latitud.value = resultado.latitud;
            longitud.value = resultado.longitud;
            if (!distrito.value.trim()) {
                aviso.textContent = 'Completa el distrito para calcular el envío.';
                return;
            }
            cotizar();
        } catch (error) {
            if (!signal.aborted) aviso.textContent = error instanceof SyntaxError ? 'No se pudo buscar la dirección.' : error.message;
        }
    }

    direccionTexto.addEventListener('input', () => {
        latitud.value = '';
        longitud.value = '';
        cotizar();
    });
    distrito.addEventListener('change', () => {
        if (!faltaDistritoDeUbicacion) {
            latitud.value = '';
            longitud.value = '';
        }
        faltaDistritoDeUbicacion = !distrito.value.trim() && Boolean(latitud.value);
        cotizar();
    });
    form.addEventListener('change', evento => {
        if (evento.target.name === 'tipoEntrega') cotizar();
    });
    document.getElementById('buscarDireccionEntrega')?.addEventListener('click', () => {
        ubicar({direccion: direccionTexto.value.trim(), distrito: distrito.value.trim()},
            iniciar('Buscando la dirección…'));
    });
    document.getElementById('ubicarEntrega')?.addEventListener('click', () => {
        const signal = iniciar('Buscando tu ubicación…');
        if (!navigator.geolocation) {
            aviso.textContent = 'Escribe tu dirección y selecciona Buscar dirección.';
            return;
        }
        navigator.geolocation.getCurrentPosition(({coords}) => {
            if (!signal.aborted) ubicar({latitud: coords.latitude, longitud: coords.longitude}, signal);
        }, () => {
            if (!signal.aborted) aviso.textContent = 'Escribe tu dirección y selecciona Buscar dirección.';
        }, {timeout: 10000});
    });
    prueba?.addEventListener('click', () => {
        latitud.value = '';
        longitud.value = '';
        ubicar({distanciaDemo: document.getElementById('distanciaDemo').value},
            iniciar('Cargando ubicación de prueba.'));
    });
    form.addEventListener('submit', evento => {
        if (!esDelivery() || (latitud.value && longitud.value)) return;
        evento.preventDefault();
        aviso.textContent = 'Selecciona Usar mi ubicación o Buscar dirección para calcular el envío.';
    });
    cotizar();
})();
