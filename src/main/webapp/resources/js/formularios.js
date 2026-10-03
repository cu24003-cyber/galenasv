window.GalenoFormularios = {
    prepararCancelacion: function (boton) {
        const formulario = boton.closest('form');
        const editor = boton.closest('.galeno-formulario') || formulario;
        const conDatos = Array.from(editor.querySelectorAll('input, textarea, select')).some(function (campo) {
            if (campo.disabled || ['hidden', 'button', 'submit', 'reset'].includes(campo.type)) return false;
            if (campo.type === 'checkbox' || campo.type === 'radio') {
                // Los valores iniciales de Activo e Indica fin no hacen que el editor esté lleno.
                if (campo.closest('.ui-selectbooleancheckbox')) return campo.checked !== campo.defaultChecked;
                return campo.checked;
            }
            let valor = campo.value.trim();
            if (campo.classList.contains('ui-inputmask')) valor = valor.replace(/[_\s\-/().]/g, '');
            return valor !== '';
        });
        let estado = formulario.querySelector('input[name="galeno.formularioVacio"]');
        if (!estado) {
            estado = document.createElement('input');
            estado.type = 'hidden';
            estado.name = 'galeno.formularioVacio';
            formulario.appendChild(estado);
        }
        estado.value = String(!conDatos);
    }
};
