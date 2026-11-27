
(() => {
    const formulario = document.querySelector('.pago-demo');

    if (!formulario) return;

    const panels = formulario.querySelectorAll('[data-pago-panel]');
    const boton = formulario.querySelector('[data-pago-submit]');

    const requiereVoucher =
        formulario.dataset.requiereVoucher === 'true';

    const actualizar = () => {
        const metodo = formulario.elements.metodo.value;

        for (const panel of panels) {
            const oculto = panel.dataset.pagoPanel !== metodo;

            panel.hidden = oculto;

            if (panel instanceof HTMLFieldSetElement) {
                panel.disabled = oculto;
            }

            panel.querySelectorAll('input').forEach(campo => {
                campo.disabled = oculto;

                if (oculto) {
                    campo.value = '';
                }
            });
        }

        if (boton) {
            if (requiereVoucher && metodo !== 'TARJETA') {
                boton.textContent = 'Enviar voucher';
            } else {
                boton.textContent = 'Pagar';
            }
        }
    };

    formulario.addEventListener('change', evento => {
        if (evento.target.name === 'metodo') {
            actualizar();
        }
    });

    actualizar();
})();
