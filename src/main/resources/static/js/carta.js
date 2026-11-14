(() => {
    'use strict';

    const aviso = document.querySelector('[data-carta-aviso]');
    let avisoTimer;
    function notificar(mensaje, error = false) {
        if (!aviso) return;
        clearTimeout(avisoTimer);
        aviso.textContent = mensaje;
        aviso.classList.toggle('error', error);
        aviso.classList.add('visible');
        avisoTimer = setTimeout(() => aviso.classList.remove('visible'), error ? 6500 : 3000);
    }

    function actualizarResumen(datos) {
        const flotante = document.querySelector('.carrito-flotante');
        if (flotante) {
            let contador = flotante.querySelector('.carrito-contador');
            if (!contador) {
                contador = document.createElement('span');
                contador.className = 'carrito-contador';
                flotante.appendChild(contador);
            }
            contador.textContent = datos.cantidadCarrito;
            contador.hidden = datos.cantidadCarrito === 0;
            flotante.setAttribute('aria-label', `Abrir carrito, ${datos.cantidadCarrito} unidades`);
        }
        const lista = document.querySelector('[data-resumen-items]');
        if (!lista || !Array.isArray(datos.itemsCarrito)) return;
        lista.replaceChildren();
        datos.itemsCarrito.forEach(item => {
            const fila = document.createElement('li');
            const nombre = document.createElement('span');
            nombre.textContent = `${item.cantidad} × ${item.nombre}`;
            const importe = document.createElement('strong');
            importe.textContent = `S/ ${item.subtotal}`;
            fila.append(nombre, importe);
            lista.appendChild(fila);
        });
        document.querySelector('[data-resumen-vacio]').hidden = datos.itemsCarrito.length > 0;
        document.querySelector('[data-resumen-subtotal]').textContent = `S/ ${datos.subtotalCarrito}`;
    }

    // Serializa las adiciones para no sobrescribir el resumen con respuestas atrasadas.
    let agregando = false;
    document.querySelectorAll('[data-agregar-carrito]').forEach(formulario => {
        formulario.addEventListener('submit', async evento => {
            if (!window.fetch) return;
            evento.preventDefault();
            if (agregando || !formulario.reportValidity()) return;
            agregando = true;
            const botones = Array.from(document.querySelectorAll('[data-agregar-carrito] button[type="submit"]'));
            botones.forEach(boton => { boton.disabled = true; });
            formulario.setAttribute('aria-busy', 'true');
            try {
                const respuesta = await fetch(formulario.dataset.ajaxUrl, {
                    method: 'POST',
                    body: new FormData(formulario),
                    credentials: 'same-origin',
                    headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
                });
                if (respuesta.status === 403 || respuesta.redirected) {
                    throw new Error('Tu sesión necesita actualizarse. Recarga la página e inténtalo de nuevo.');
                }
                if (!(respuesta.headers.get('content-type') || '').includes('application/json')) {
                    throw new Error('No pudimos confirmar la operación. Revisa tu carrito antes de volver a agregar.');
                }
                const datos = await respuesta.json();
                if (!respuesta.ok || !datos.exito) {
                    throw new Error(datos.mensaje || 'No se pudo agregar el producto.');
                }
                actualizarResumen(datos);
                notificar(datos.mensaje || 'Producto agregado al carrito.');
            } catch (error) {
                notificar(error instanceof TypeError
                    ? 'Se perdió la conexión. Revisa tu carrito antes de volver a agregar.'
                    : error.message, true);
            } finally {
                agregando = false;
                botones.forEach(boton => { boton.disabled = false; });
                formulario.removeAttribute('aria-busy');
            }
        });
    });

    document.querySelectorAll('[data-selector-cantidad]').forEach(selector => {
        const input = selector.querySelector('input');
        const botones = selector.querySelectorAll('[data-cantidad-paso]');
        function actualizarLimites() {
            botones.forEach(boton => {
                const paso = Number(boton.dataset.cantidadPaso);
                boton.disabled = paso < 0 ? Number(input.value) <= 1 : Number(input.value) >= 30;
            });
        }
        botones.forEach(boton => boton.addEventListener('click', () => {
            const valor = Number(input.value);
            input.value = Math.min(30, Math.max(1, (Number.isFinite(valor) ? Math.trunc(valor) : 1) + Number(boton.dataset.cantidadPaso)));
            actualizarLimites();
        }));
        input.addEventListener('input', actualizarLimites);
        actualizarLimites();
    });

    const clavePosicion = 'rinconcito.carta.posicion';
    const claveRegreso = 'rinconcito.carta.regresar';
    function filtros(url) {
        return JSON.stringify([url.pathname, url.searchParams.get('q') || '', url.searchParams.get('categoria') || '']);
    }
    document.querySelectorAll('[data-guardar-posicion]').forEach(enlace => {
        enlace.addEventListener('click', () => {
            try {
                sessionStorage.setItem(clavePosicion, JSON.stringify({
                    filtros: filtros(new URL(window.location.href)), y: window.scrollY
                }));
            } catch (_) { /* La navegación funciona también sin almacenamiento local. */ }
        });
    });
    document.querySelector('[data-volver-carta]')?.addEventListener('click', () => {
        try { sessionStorage.setItem(claveRegreso, '1'); } catch (_) { /* Almacenamiento no disponible. */ }
    });
    if (document.querySelector('[data-pagina-carta]')) {
        try {
            const restaurar = sessionStorage.getItem(claveRegreso) === '1';
            sessionStorage.removeItem(claveRegreso);
            const posicion = JSON.parse(sessionStorage.getItem(clavePosicion) || 'null');
            if (restaurar && posicion?.filtros === filtros(new URL(window.location.href)) && Number.isFinite(posicion.y)) {
                const volver = () => requestAnimationFrame(() => window.scrollTo({ top: posicion.y, behavior: 'instant' }));
                if (document.readyState === 'complete') volver();
                else window.addEventListener('load', volver, { once: true });
            }
        } catch (_) { /* No es necesario restaurar una posición inválida. */ }
    }
    window.addEventListener('pageshow', evento => {
        if (evento.persisted) window.location.reload();
    });
})();
