package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.context.FacesContext;

/** Decide si cancelar limpia o cierra usando los valores actuales del formulario en el navegador. */
public final class FormularioCancelacion {
    private FormularioCancelacion() { }

    public static boolean estaVacio() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null || context.getExternalContext() == null) return true;
        String vacio = context.getExternalContext().getRequestParameterMap().get("galeno.formularioVacio");
        return vacio == null || Boolean.parseBoolean(vacio);
    }
}
