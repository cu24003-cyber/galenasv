package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Clinica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ClinicaService;

@Named("clinicaModel")
@ViewScoped
public class ClinicaModel extends AbstractModel<Clinica, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ClinicaService clinicaService;

    @Override
    protected ClinicaService getService() {
        return clinicaService;
    }

    @Override
    protected Clinica nuevaInstancia() {
        return new Clinica();
    }

    @Override
    protected UUID obtenerId(Clinica entidad) {
        return entidad.getIdClinica();
    }
}
