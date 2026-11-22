package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPaso;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.ProcedimientoPasoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RolRepository;

@Stateless
public class ProcedimientoPasoService extends AbstractService<ProcedimientoPaso, UUID> {

    @Inject
    private ProcedimientoPasoRepository procedimientoPasoRepository;

    @Override
    protected RepositoryInterface<ProcedimientoPaso, UUID> getRepository() {
        return procedimientoPasoRepository;
    }

    @Override
    protected UUID obtenerId(ProcedimientoPaso entidad) {
        return entidad.getIdProcedimientoPaso();
    }

    @Inject
    private RolRepository rolRepository;

    /**
     * El responsable de un paso debe ser un rol activo (la pantalla JSF ya lo exige; esto cubre la API).
     * Al editar, lo de rol activo solo se comprueba si el rol cambia, para poder renombrar un paso cuyo rol
     * se desactivó; el Paciente se rechaza siempre.
     */
    private void validarRol(ProcedimientoPaso entidad, UUID rolAnterior) {
        UUID id = entidad == null || entidad.getIdRol() == null ? null : entidad.getIdRol().getIdRol();
        if (id == null) return;
        Rol rol = rolRepository.find(id);
        rechazarPaciente(rol);
        if (id.equals(rolAnterior)) return;
        if (rol == null || !Boolean.TRUE.equals(rol.getActivo())) {
            throw ServiceException.localizada("procedimiento.pasoNombreRol", "Indique nombre y rol activo para el paso.");
        }
    }

    /** El Paciente (código PACIENTE) nunca es responsable de un paso, ni al crearlo ni al editarlo. */
    static void rechazarPaciente(Rol rol) {
        if (rol != null && AutenticacionService.PACIENTE.equals(rol.getCodigo())) {
            throw ServiceException.localizada("procedimiento.pasoRolPaciente",
                    "El rol Paciente no puede ser responsable de un paso.");
        }
    }

    @Override
    public void crear(ProcedimientoPaso entidad) {
        if (entidad != null && entidad.getIdProcedimientoPaso() == null) {
            entidad.setIdProcedimientoPaso(UUID.randomUUID());
        }
        validarRol(entidad, null);
        super.crear(entidad);
    }

    @Override
    public void actualizar(ProcedimientoPaso entidad) {
        ProcedimientoPaso anterior = entidad == null || entidad.getIdProcedimientoPaso() == null ? null
                : procedimientoPasoRepository.find(entidad.getIdProcedimientoPaso());
        validarRol(entidad, anterior == null || anterior.getIdRol() == null ? null : anterior.getIdRol().getIdRol());
        super.actualizar(entidad);
    }

}
