(() => {
    const formulario = document.querySelector('.pago-demo');
    if (!formulario) return;
    const panels = formulario.querySelectorAll('[data-pago-panel]');
    const actualizar = () => {
        const metodo = formulario.elements.metodo.value;
        for (const panel of panels) {
            panel.hidden = panel.dataset.pagoPanel !== metodo;
            if (panel instanceof HTMLFieldSetElement) {
                panel.disabled = panel.hidden;
                if (panel.hidden) panel.querySelectorAll('input').forEach(campo => { campo.value = ''; });
            }
        }
    };
    formulario.addEventListener('change', evento => {
        if (evento.target.name === 'metodo') actualizar();
    });
    actualizar();
})();
