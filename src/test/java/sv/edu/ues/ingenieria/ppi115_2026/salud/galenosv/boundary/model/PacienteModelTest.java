package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionSesion;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;

@ExtendWith(MockitoExtension.class)
class PacienteModelTest {
    @Mock AtencionService atencion;
    @Mock AtencionSesion sesion;
    @InjectMocks PacienteModel model;

    @Test
    void sinRolPacienteActivoMuestraElMismoMensajeQueConsultas() {
        when(sesion.getRolActivo()).thenReturn(new PersonaRol());
        when(atencion.pacientesClinica()).thenThrow(ServiceException.localizada("consulta.sinRolPaciente",
                "No hay un rol Paciente activo. Créelo o actívelo en Roles."));
        FacesContext faces = mock(FacesContext.class);
        try (var contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            model.cargar();
        }
        ArgumentCaptor<FacesMessage> mensaje = ArgumentCaptor.forClass(FacesMessage.class);
        verify(faces).addMessage(isNull(), mensaje.capture());
        assertEquals("No hay un rol Paciente activo. Créelo o actívelo en Roles.", mensaje.getValue().getSummary());
        assertTrue(model.getPacientes().isEmpty());
    }
}
