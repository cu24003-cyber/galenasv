package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import java.util.List;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;

/** Consulta únicamente las asignaciones activas de la cuenta autenticada. */
@Stateless
public class ContextoRolService {
    @jakarta.inject.Inject private AccesoSesion acceso;
    @jakarta.inject.Inject private AutenticacionService autenticacion;

    public List<PersonaRol> listar() {
        return acceso.vigente() ? autenticacion.asignaciones(acceso.getIdUsuario()) : List.of();
    }

    public PersonaRol cargar(UUID id) {
        return listar().stream().filter(r -> r.getIdPersonaRol().equals(id)).findFirst()
                .orElseThrow(() -> ServiceException.localizada("rolCambio.asignacionActiva", "Seleccione una asignación con un rol activo."));
    }
}
