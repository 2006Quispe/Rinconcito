document.addEventListener('DOMContentLoaded', () => {
    const dialog = document.getElementById('club-modal-canje');
    if (!dialog || typeof dialog.showModal !== 'function') return;

    const image = document.getElementById('club-modal-canje-imagen');
    const name = document.getElementById('club-modal-canje-nombre');
    const description = document.getElementById('club-modal-canje-descripcion');
    const balance = document.getElementById('club-modal-saldo');
    const cost = document.getElementById('club-modal-costo');
    const remaining = document.getElementById('club-modal-restante');
    const conditions = document.getElementById('club-modal-canje-condiciones');
    const confirmButton = document.getElementById('club-modal-canje-confirmar');
    const numberFormat = new Intl.NumberFormat('es-PE');
    let pendingForm = null;

    const addCondition = (text) => {
        const item = document.createElement('li');
        item.textContent = text;
        conditions.append(item);
    };

    document.querySelectorAll('.club-form-canje').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (form.dataset.clubConfirmado === 'true') {
                delete form.dataset.clubConfirmado;
                return;
            }

            event.preventDefault();
            const submitter = event.submitter || form.querySelector('.club-canjear');
            if (!submitter || submitter.disabled) return;

            pendingForm = form;
            const rewardName = submitter.dataset.nombre || 'Recompensa';
            const currentBalance = Number(submitter.dataset.saldo) || 0;
            const rewardCost = Math.max(0, Number(submitter.dataset.costo) || 0);
            const conditionsText = (submitter.dataset.condiciones || '').trim();
            const rewardType = submitter.dataset.tipo || '';

            name.textContent = rewardName;
            description.textContent = submitter.dataset.descripcion || '';
            balance.textContent = numberFormat.format(currentBalance);
            cost.textContent = '\u2212' + numberFormat.format(rewardCost);
            remaining.textContent = numberFormat.format(Math.max(0, currentBalance - rewardCost));

            conditions.replaceChildren();
            if (conditionsText) addCondition(conditionsText);

            const minimum = Number(submitter.dataset.compraMinima) || 0;
            if (minimum > 0) addCondition('Pedido m\u00ednimo: S/ ' + numberFormat.format(minimum) + '.');

            const validity = Number(submitter.dataset.vigenciaDias) || 0;
            if (validity > 0) {
                addCondition('V\u00e1lida durante ' + validity + ' d\u00edas desde el canje.');
            }

            const rewardIsPhysical = ['PRODUCTO', 'PRODUCTO_MERCH', 'PRODUCTO_CARTA', 'EXPERIENCIA']
                .includes(rewardType);
            if (rewardIsPhysical) {
                if (!/presenta.*c.digo.*caja/i.test(conditionsText)) {
                    addCondition('Presenta tu c\u00f3digo de canje en caja.');
                }
            } else if (rewardType === 'DELIVERY_GRATIS') {
                addCondition('Beneficio aplicable en pedidos delivery.');
            } else {
                addCondition('El descuento se aplica desde tu pedido en l\u00ednea.');
            }

            if (!/sujeto a disponibilidad/i.test(conditionsText)) {
                addCondition('Sujeta a disponibilidad.');
            }

            const imagePath = submitter.dataset.imagen;
            if (imagePath) {
                image.src = imagePath;
                image.alt = rewardName;
                image.hidden = false;
            } else {
                image.removeAttribute('src');
                image.alt = '';
                image.hidden = true;
            }

            dialog.showModal();
        });
    });

    document.querySelectorAll('[data-club-modal-cerrar]').forEach((button) => {
        button.addEventListener('click', () => dialog.close());
    });

    dialog.addEventListener('click', (event) => {
        if (event.target === dialog) dialog.close();
    });

    dialog.addEventListener('close', () => {
        pendingForm = null;
    });

    confirmButton.addEventListener('click', () => {
        const form = pendingForm;
        const submitter = form?.querySelector('.club-canjear');
        if (!form || !submitter || submitter.disabled) return;

        pendingForm = null;
        form.dataset.clubConfirmado = 'true';
        dialog.close();
        form.requestSubmit(submitter);
    });
});
