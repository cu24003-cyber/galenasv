package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.converter;

import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoExamen;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.TipoExamenService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model.Mensajes;

@FacesConverter(value = "tipoExamenAtencionConverter", managed = true)
public class TipoExamenConverter implements Converter<TipoExamen> {
    @EJB
    private TipoExamenService service;

    @Override
    public TipoExamen getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            TipoExamen tipo = service.buscarPorId(UUID.fromString(value));
            if (tipo != null && Boolean.TRUE.equals(tipo.getActivo())) {
                return tipo;
            }
        } catch (IllegalArgumentException e) {
            // Solo se aceptan identificadores provenientes del catálogo.
        }
        throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
                Mensajes.texto("registro.seleccionInvalida"), null));
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, TipoExamen value) {
        return value == null || value.getIdTipoExamen() == null ? "" : value.getIdTipoExamen().toString();
    }
}
