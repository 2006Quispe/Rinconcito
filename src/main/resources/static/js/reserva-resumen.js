const reserva = document.querySelector('[data-resumen-url]');
if (reserva) {
    const fecha = reserva.elements.fecha;
    const personas = reserva.elements.numeroPersonas;
    const evento = reserva.querySelector('[data-evento]');
    const entradas = reserva.querySelector('[data-entradas]');
    const total = reserva.querySelector('[data-total]');
    const boton = reserva.querySelector('button[type="submit"]');
    let solicitud;

    async function actualizarResumen() {
        solicitud?.abort();
        entradas.hidden = true;
        total.hidden = true;
        boton.textContent = 'Revisar reserva';
        if (!fecha.value) {
            evento.textContent = 'Elige una fecha para ver el importe.';
            return;
        }

        solicitud = new AbortController();
        const signal = solicitud.signal;
        evento.textContent = 'Consultando la fecha.';
        try {
            const url = new URL(reserva.dataset.resumenUrl, window.location.origin);
            url.searchParams.set('fecha', fecha.value);
            const respuesta = await fetch(url, { signal });
            if (!respuesta.ok) throw new Error('Resumen no disponible');
            const resumen = await respuesta.json();
            if (signal.aborted) return;

            const cantidad = Number(personas.value);
            evento.textContent = resumen.evento || 'Reserva gratuita';
            if (resumen.precioEntrada > 0) {
                entradas.textContent = 'S/ ' + resumen.precioEntrada.toFixed(2) + ' por persona';
                entradas.hidden = false;
                if (Number.isInteger(cantidad) && cantidad > 0) {
                    entradas.textContent = cantidad + ' � S/ ' + resumen.precioEntrada.toFixed(2);
                    total.textContent = 'Total � S/ ' + (cantidad * resumen.precioEntrada).toFixed(2);
                    total.hidden = false;
                }
                boton.textContent = 'Continuar al pago';
            } else {
                boton.textContent = 'Confirmar reserva';
            }
        } catch {
            if (!signal.aborted) evento.textContent = 'El importe se verificar� al continuar.';
        }
    }

    fecha.addEventListener('change', actualizarResumen);
    personas.addEventListener('input', actualizarResumen);
    actualizarResumen();
}
