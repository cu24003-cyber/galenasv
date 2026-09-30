package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.converter;

import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Clinica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ClinicaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model.Mensajes;

@FacesConverter(value = "clinicaConverter", managed = true)
public class ClinicaConverter implements Converter<Clinica> {
    @EJB
    private ClinicaService service;

    @Override
    public Clinica getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Clinica tipo = service.buscarPorId(UUID.fromString(value));
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
    public String getAsString(FacesContext context, UIComponent component, Clinica value) {
        return value == null || value.getIdClinica() == null ? "" : value.getIdClinica().toString();
    }
}
