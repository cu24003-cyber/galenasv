package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.faces.application.Application;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.AbstractModel.ExpresionRegularValidator;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.RegistroPersonaModel.ValorCatalogoValidator;

class ValorCatalogoValidatorTest {
    @Test
    void validaDocumentoContactoYConfiguracionInvalida() {
        FacesContext context = mock(FacesContext.class);
        Application application = mock(Application.class);
        when(context.getApplication()).thenReturn(application);
        when(application.getResourceBundle(context, "msg")).thenReturn(
                ResourceBundle.getBundle("i18n.Messages", Locale.forLanguageTag("es")));
        try (var current = mockStatic(FacesContext.class)) {
            current.when(FacesContext::getCurrentInstance).thenReturn(context);
            UIInput campo = new UIInput();
            ValorCatalogoValidator validator = new ValorCatalogoValidator();
            campo.getAttributes().put("expresionRegular", "[0-9]{8}-[0-9]");
            assertDoesNotThrow(() -> validator.validate(context, campo, "12345678-9"));
            assertThrows(ValidatorException.class, () -> validator.validate(context, campo, "123456789"));
            campo.getAttributes().put("expresionRegular", "[0-9]{4}-[0-9]{4}");
            assertDoesNotThrow(() -> validator.validate(context, campo, "7777-7777"));
            assertThrows(ValidatorException.class, () -> validator.validate(context, campo, "incorrecto"));
            campo.getAttributes().put("expresionRegular", "[");
            assertThrows(ValidatorException.class, () -> validator.validate(context, campo, "123"));
            ExpresionRegularValidator catalogo = new ExpresionRegularValidator();
            assertThrows(ValidatorException.class, () -> catalogo.validate(context, campo, "["));
            assertDoesNotThrow(() -> catalogo.validate(context, campo, "[0-9]{4}"));
        }
    }
}
