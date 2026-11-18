(() => {
    const dni = document.querySelector('[data-dni-url]');
    if (!dni || dni.readOnly) return;

    const formulario = dni.form;
    const nombres = formulario.elements.nombres;
    const apellidos = formulario.elements.apellidos;
    const estado = document.getElementById('dni-estado');
    const csrf = formulario.querySelector('input[type="hidden"]');
    let consulta;
    let temporizador;
    let valoresAplicados;
    let ultimoDni = '';

    dni.addEventListener('input', () => {
        dni.value = dni.value.replace(/\D/g, '').slice(0, 8);
        if (dni.value === ultimoDni) return;
        ultimoDni = dni.value;
        clearTimeout(temporizador);
        consulta?.abort();
        estado.textContent = '';

        if (valoresAplicados) {
            if (nombres.value === valoresAplicados.nombres) nombres.value = '';
            if (apellidos.value === valoresAplicados.apellidos) apellidos.value = '';
            valoresAplicados = null;
        }

        if (dni.value.length === 8) {
            estado.textContent = 'Consultando DNI…';
            temporizador = setTimeout(() => consultar(dni.value), 300);
        }
    });

    async function consultar(numero) {
        consulta = new AbortController();
        const { signal } = consulta;
        const inicial = { nombres: nombres.value, apellidos: apellidos.value };
        const cuerpo = new URLSearchParams({ dni: numero });
        if (csrf) cuerpo.set(csrf.name, csrf.value);

        try {
            const respuesta = await fetch(dni.dataset.dniUrl, {
                method: 'POST',
                headers: { Accept: 'application/json' },
                body: cuerpo,
                signal
            });
            const datos = await respuesta.json();
            if (signal.aborted || dni.value !== numero) return;
            if (!respuesta.ok || !datos.nombres || !datos.apellidos) {
                throw new Error('Consulta sin datos');
            }

            valoresAplicados = {};
            if (nombres.value === inicial.nombres) {
                nombres.value = datos.nombres;
                valoresAplicados.nombres = datos.nombres;
            }
            if (apellidos.value === inicial.apellidos) {
                apellidos.value = datos.apellidos;
                valoresAplicados.apellidos = datos.apellidos;
            }
            estado.textContent = 'DNI encontrado. Revisa tus nombres y apellidos.';
        } catch {
            if (!signal.aborted && dni.value === numero) {
                estado.textContent = 'No pudimos consultar el DNI. Puedes completar tus datos manualmente.';
            }
        }
    }
})();
