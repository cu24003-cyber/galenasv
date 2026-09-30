package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.validation;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;
import java.util.regex.PatternSyntaxException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model.Mensajes;

@FacesValidator("valorCatalogoValidator")
public class ValorCatalogoValidator implements Validator<String> {
    @Override
    public void validate(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) return; // required valida los campos vacíos.
        String expresion = (String) component.getAttributes().get("expresionRegular");
        try {
            if (FormatoExpresion.coincide(value, expresion)) return;
        } catch (PatternSyntaxException e) {
            throw error("registro.patronInvalido");
        }
        throw error("registro.formatoInvalido");
    }

    private ValidatorException error(String clave) {
        return new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, Mensajes.texto(clave), null));
    }
}
