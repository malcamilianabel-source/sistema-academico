/*
 * Comportamiento común de los formularios en modales.
 * - Un campo que el servidor marcó en rojo deja de estarlo cuando el usuario lo corrige.
 * - Al cerrar un modal se limpian sus errores, para que no aparezcan al abrirlo de nuevo.
 * - Fechas y horas: el fin no puede ser anterior al inicio (además se valida en el servidor).
 */
(function () {
    'use strict';

    function limpiarCampo(campo) {
        if (!campo.classList.contains('is-invalid')) return;
        campo.classList.remove('is-invalid');
        const feedback = campo.parentElement && campo.parentElement.querySelector('.invalid-feedback');
        // El mensaje del servidor se reemplaza por la regla del campo, por si el navegador lo vuelve a marcar
        if (feedback && campo.title) feedback.textContent = campo.title;
    }

    document.addEventListener('input', e => limpiarCampo(e.target));
    document.addEventListener('change', e => limpiarCampo(e.target));

    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('hidden.bs.modal', () => {
            modal.querySelectorAll('.is-invalid').forEach(c => c.classList.remove('is-invalid'));
            modal.querySelectorAll('form').forEach(f => f.classList.remove('was-validated'));
            modal.querySelectorAll('.alert-danger').forEach(a => a.remove());
        });
    });

    function diaSiguiente(fechaIso) {
        const d = new Date(fechaIso + 'T00:00:00');
        d.setDate(d.getDate() + 1);
        return d.toISOString().slice(0, 10);
    }

    // Enlaza inicio y fin dentro de un mismo formulario (por nombre del campo)
    function enlazar(form, nombreInicio, nombreFin, calcularMinimo) {
        const inicio = form.querySelector('[name="' + nombreInicio + '"]');
        const fin = form.querySelector('[name="' + nombreFin + '"]');
        if (!inicio || !fin) return;
        const actualizar = () => {
            fin.min = inicio.value ? calcularMinimo(inicio.value) : '';
        };
        inicio.addEventListener('change', actualizar);
        inicio.addEventListener('input', actualizar);
        // Los modales de edición cargan valores por JavaScript: se recalcula al enfocar el fin
        fin.addEventListener('focus', actualizar);
        actualizar();
    }

    document.querySelectorAll('form').forEach(form => {
        enlazar(form, 'fechaInicio', 'fechaFin', diaSiguiente);
        enlazar(form, 'horaInicio', 'horaFin', v => v);
    });
})();
