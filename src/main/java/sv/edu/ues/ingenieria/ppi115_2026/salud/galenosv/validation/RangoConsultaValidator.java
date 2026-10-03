package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.validation;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;
import java.time.LocalDate;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model.Mensajes;

@FacesValidator("rangoConsultaValidator")
public class RangoConsultaValidator implements Validator<LocalDate> {
    @Override
    public void validate(FacesContext context, UIComponent component, LocalDate hasta) {
        UIInput inferior = (UIInput) component.findComponent("desde");
        // El valor local incluye Desde cuando ambos campos se envían en la misma petición.
        if (inferior != null && inferior.isValid() && inferior.getValue() instanceof LocalDate desde
                && hasta != null && hasta.isBefore(desde)) {
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    Mensajes.texto("consulta.rangoInvalido"), null));
        }
    }
}
