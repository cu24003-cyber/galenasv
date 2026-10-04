package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.HistorialConsultaModel.RangoConsultaValidator;

class RangoConsultaValidatorTest {
    @Test void fechaEscritaManualmenteSeComparaConElDesdeLocalDeLaMismaPeticion() {
        try (var contexto = mockStatic(FacesContext.class)) {
            UIInput desde = mock(UIInput.class);
            UIInput hasta = mock(UIInput.class);
            when(hasta.findComponent("desde")).thenReturn(desde);
            when(desde.isValid()).thenReturn(true);
            var fecha = LocalDate.of(2026, 10, 3); when(desde.getValue()).thenReturn(fecha);
            RangoConsultaValidator validator = new RangoConsultaValidator();
            assertThrows(ValidatorException.class, () -> validator.validate(null, hasta, fecha.minusDays(1)));
            assertDoesNotThrow(() -> validator.validate(null, hasta, fecha));
            assertDoesNotThrow(() -> validator.validate(null, hasta, fecha.plusDays(1)));
            assertDoesNotThrow(() -> validator.validate(null, hasta, null));
            when(desde.getValue()).thenReturn(null);
            assertDoesNotThrow(() -> validator.validate(null, hasta, fecha.minusDays(1)));
        }
    }
}
