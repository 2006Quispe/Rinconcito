(() => {
    const panel = document.getElementById('avisoDelivery');
    const form = panel?.closest('form');
    if (!form) return;
    const {latitud, longitud, distrito} = form.elements;
    const aviso = document.getElementById('cotizacionDelivery');
    const costo = document.getElementById('importeDelivery');
    const total = document.getElementById('importeTotal');
    let solicitud;
    function render(tarifa) {
        costo.textContent = tarifa === null ? 'Por calcular' : 'S/ ' + tarifa;
        total.textContent = tarifa === null ? 'Por calcular' : 'S/ ' + (Number(panel.dataset.subtotal) + tarifa);
    }
    async function cotizar() {
        solicitud?.abort();
        if (form.elements.tipoEntrega.value !== 'DELIVERY') {
            render(0);
            return;
        }
        render(null);
        aviso.textContent = 'Indica el distrito y la ubicación para cotizar.';
        if (!distrito.value.trim() || !latitud.value || !longitud.value) return;
        solicitud = new AbortController();
        const {signal} = solicitud;
        aviso.textContent = 'Calculando envío…';
        const datos = new URLSearchParams({
            distrito: distrito.value.trim(), latitud: latitud.value, longitud: longitud.value
        });
        const csrf = form.elements._csrf;
        if (csrf) datos.set(csrf.name, csrf.value);
        try {
            const respuesta = await fetch(panel.dataset.cotizar, {method: 'POST', body: datos, signal});
            const resultado = await respuesta.json();
            if (signal.aborted) return;
            if (!respuesta.ok) {
                aviso.textContent = resultado.error || 'No se pudo cotizar el envío.';
                return;
            }
            render(resultado.costo);
            aviso.textContent = 'Envío calculado · S/ ' + resultado.costo;
        } catch {
            if (!signal.aborted) aviso.textContent = 'No se pudo cotizar el envío. Intenta nuevamente.';
        }
    }
    form.addEventListener('change', evento => {
        if (['tipoEntrega', 'distrito', 'latitud', 'longitud'].includes(evento.target.name)) cotizar();
    });
    document.getElementById('ubicarEntrega').addEventListener('click', () => {
        if (!navigator.geolocation) {
            aviso.textContent = 'Puedes ingresar las coordenadas manualmente.';
            return;
        }
        aviso.textContent = 'Buscando ubicación…';
        navigator.geolocation.getCurrentPosition(({coords}) => {
            latitud.value = coords.latitude;
            longitud.value = coords.longitude;
            cotizar();
        }, () => {
            aviso.textContent = 'No se pudo obtener la ubicación. Ingresa las coordenadas manualmente.';
        }, {timeout: 10000});
    });
    cotizar();
})();
