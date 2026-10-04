package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Consulta;

@Named("consultaModel")
@ViewScoped
public class ConsultaModel extends AbstractModel<Consulta, UUID> {

    private static final long serialVersionUID = 1L;

    @EJB
    private ConsultaService consultaService;

    @Override
    protected ConsultaService getService() {
        return consultaService;
    }

    @Override
    protected Consulta nuevaInstancia() {
        return new Consulta();
    }

    @Override
    protected UUID obtenerId(Consulta entidad) {
        return entidad.getIdConsulta();
    }
}
