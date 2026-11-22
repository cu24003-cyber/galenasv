package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ProcedimientoPasoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.ProcedimientoPasoExamenRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Examen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.ExamenRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;

@Stateless
public class ProcedimientoPasoExamenService extends AbstractService<ProcedimientoPasoExamen, UUID> {

    @Inject
    private ProcedimientoPasoExamenRepository procedimientoPasoExamenRepository;

    @Override
    protected RepositoryInterface<ProcedimientoPasoExamen, UUID> getRepository() {
        return procedimientoPasoExamenRepository;
    }

    @Override
    protected UUID obtenerId(ProcedimientoPasoExamen entidad) {
        return entidad.getIdProcedimientoPasoExamen();
    }

    @Inject
    private ExamenRepository examenRepository;

    /** Solo se asocian exámenes activos (como en la pantalla JSF); al editar, solo si el examen cambia. */
    private void validarExamen(ProcedimientoPasoExamen entidad, UUID examenAnterior) {
        UUID id = entidad == null || entidad.getIdExamen() == null ? null : entidad.getIdExamen().getIdExamen();
        if (id == null || id.equals(examenAnterior)) return;
        Examen examen = examenRepository.find(id);
        if (examen == null || !Boolean.TRUE.equals(examen.getActivo())) {
            throw ServiceException.localizada("procedimiento.asociacionInvalida",
                    "Seleccione un paso del procedimiento y un examen activo.");
        }
    }

    @Override
    public void crear(ProcedimientoPasoExamen entidad) {
        if (entidad != null && entidad.getIdProcedimientoPasoExamen() == null) {
            entidad.setIdProcedimientoPasoExamen(UUID.randomUUID());
        }
        validarExamen(entidad, null);
        super.crear(entidad);
    }

    @Override
    public void actualizar(ProcedimientoPasoExamen entidad) {
        ProcedimientoPasoExamen anterior = entidad == null || entidad.getIdProcedimientoPasoExamen() == null ? null
                : procedimientoPasoExamenRepository.find(entidad.getIdProcedimientoPasoExamen());
        validarExamen(entidad, anterior == null || anterior.getIdExamen() == null ? null : anterior.getIdExamen().getIdExamen());
        super.actualizar(entidad);
    }

}
