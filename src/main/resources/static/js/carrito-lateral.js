(() => {
    const panel = document.querySelector('#carrito-lateral');
    const enlace = document.querySelector('.carrito-flotante');
    if (!panel || !enlace || !window.bootstrap?.Offcanvas || !window.fetch) return;

    const contenido = panel.querySelector('[data-carrito-contenido]');
    const titulo = panel.querySelector('h2');
    const lateral = bootstrap.Offcanvas.getOrCreateInstance(panel);

    async function cargar(url, opciones) {
        if (contenido.getAttribute('aria-busy') === 'true') return;
        contenido.setAttribute('aria-busy', 'true');
        try {
            const respuesta = await fetch(url, opciones);
            if (!respuesta.ok) throw new Error();
            const vista = new DOMParser().parseFromString(await respuesta.text(), 'text/html');
            const carrito = vista.querySelector('[data-unidades]');
            if (!carrito) throw new Error();
            const unidades = carrito.dataset.unidades;
            contenido.replaceChildren(carrito);
            titulo.textContent = 'Tu carrito (' + unidades + ')';
            const contador = enlace.querySelector('.carrito-contador') || document.createElement('span');
            contador.className = 'carrito-contador';
            contador.textContent = unidades;
            contador.hidden = unidades === '0';
            if (!contador.parentNode) enlace.appendChild(contador);
            enlace.setAttribute('aria-label', 'Abrir carrito, ' + unidades + ' unidades');
        } catch {
            const aviso = document.createElement('p');
            aviso.setAttribute('role', 'alert');
            aviso.textContent = 'No pudimos actualizar el carrito. ';
            const volver = document.createElement('a');
            volver.href = enlace.href;
            volver.textContent = 'Abrir carrito';
            aviso.appendChild(volver);
            contenido.replaceChildren(aviso);
        } finally {
            contenido.removeAttribute('aria-busy');
        }
    }

    enlace.addEventListener('click', evento => {
        if (evento.ctrlKey || evento.metaKey || evento.shiftKey || evento.altKey) return;
        evento.preventDefault();
        lateral.show(enlace);
        cargar(panel.dataset.carritoUrl);
    });

    contenido.addEventListener('submit', evento => {
        evento.preventDefault();
        const formulario = evento.target;
        cargar(formulario.action, { method: 'POST', body: new FormData(formulario) });
    });

    panel.addEventListener('hidden.bs.offcanvas', () => enlace.focus());
})();
