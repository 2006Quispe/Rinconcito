(function () {
    "use strict";

    function obtenerCampo(id) {
        return id ? document.getElementById(id) : null;
    }

    function mostrarEstado(elemento, mensaje, tipo) {
        const icono = document.createElement("i");

        icono.className = tipo === "exito"
            ? "bi bi-check-circle-fill"
            : "bi bi-exclamation-circle-fill";

        elemento.className = "dni-consulta-estado " + tipo;
        elemento.replaceChildren(
            icono,
            document.createTextNode(mensaje)
        );
        elemento.hidden = false;
    }

    function inicializarConsulta(contenedor) {
        const campoDni = obtenerCampo(contenedor.dataset.dniCampo);
        const campoNombres = obtenerCampo(contenedor.dataset.nombresCampo);
        const campoApellidos = obtenerCampo(contenedor.dataset.apellidosCampo);
        const siguiente = obtenerCampo(contenedor.dataset.siguienteCampo);
        const boton = contenedor.querySelector("[data-dni-boton]");
        const estado = contenedor.querySelector("[data-dni-estado]");

        if (!campoDni || !campoNombres || !campoApellidos || !boton || !estado) {
            return;
        }

        const formulario = boton.closest("form");
        const campoCsrf = formulario
            ? formulario.querySelector('input[name="_csrf"]')
            : null;
        const icono = boton.querySelector("i");
        const texto = boton.querySelector("span");

        function actualizarBoton() {
            boton.disabled = boton.hidden || campoDni.value.length !== 8;
        }

        function limpiarResultado() {
            estado.hidden = true;
            campoNombres.classList.remove("dato-dni-autocompletado");
            campoApellidos.classList.remove("dato-dni-autocompletado");
        }

        campoDni.addEventListener("input", function () {
            campoDni.value = campoDni.value
                .replace(/\D/g, "")
                .slice(0, 8);
            limpiarResultado();
            actualizarBoton();
        });

        boton.addEventListener("click", async function () {
            if (!/^[0-9]{8}$/.test(campoDni.value)) {
                mostrarEstado(
                    estado,
                    "Ingresa un DNI válido de 8 números.",
                    "error"
                );
                return;
            }

            boton.disabled = true;
            icono.className = "spinner-border spinner-border-sm";
            texto.textContent = "Consultando";

            const cuerpo = new URLSearchParams();
            cuerpo.append("dni", campoDni.value);

            if (campoCsrf) {
                cuerpo.append(campoCsrf.name, campoCsrf.value);
            }

            try {
                const respuesta = await fetch(
                    boton.dataset.consultaUrl,
                    {
                        method: "POST",
                        headers: {
                            "Accept": "application/json",
                            "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
                        },
                        body: cuerpo
                    }
                );

                const datos = await respuesta.json().catch(function () {
                    return {};
                });

                if (!respuesta.ok) {
                    throw new Error(
                        datos.mensaje || "No se pudo consultar el DNI."
                    );
                }

                campoNombres.value = datos.nombres;
                campoApellidos.value = datos.apellidos;
                campoNombres.classList.add("dato-dni-autocompletado");
                campoApellidos.classList.add("dato-dni-autocompletado");
                campoNombres.dispatchEvent(new Event("input", {bubbles: true}));
                campoApellidos.dispatchEvent(new Event("input", {bubbles: true}));

                mostrarEstado(
                    estado,
                    "Datos encontrados correctamente.",
                    "exito"
                );

                if (siguiente) {
                    siguiente.focus();
                }
            } catch (error) {
                mostrarEstado(
                    estado,
                    error.message + " Puedes completar los datos manualmente.",
                    "error"
                );
            } finally {
                icono.className = "bi bi-search";
                texto.textContent = "Consultar";
                actualizarBoton();
            }
        });

        contenedor.addEventListener("dni:actualizar", function () {
            limpiarResultado();
            actualizarBoton();
        });

        actualizarBoton();
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-dni-consulta]")
            .forEach(inicializarConsulta);
    });
})();
