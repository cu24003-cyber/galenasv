package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Consulta;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtencionModelTest {
    @Mock AtencionService servicio;
    @Spy AtencionSesion sesion = new AtencionSesion();
    @InjectMocks AtencionModel model;
    private Consulta consulta;

    @BeforeEach void prepararConsulta() {
        consulta = new Consulta(UUID.randomUUID());
        consulta.setReferenciaExterna("Referencia");
        consulta.setObservaciones("Observaciones");
        sesion.setConsulta(consulta.getIdConsulta());
        when(servicio.cargar(consulta.getIdConsulta())).thenReturn(consulta);
        model.iniciar();
    }

    @Test void cierreExitosoLiberaSesionYDevuelveNavegacionFaces() {
        assertEquals("/paginas/paciente-list.xhtml?faces-redirect=true", model.cerrar());
        verify(servicio).cerrar(consulta.getIdConsulta(), "Referencia", "Observaciones");
        assertNull(sesion.getConsulta());
    }

    @Test void cierreRechazadoConservaConsultaYNoNavega() {
        doThrow(new ServiceException("Complete los pasos pendientes.")).when(servicio)
                .cerrar(consulta.getIdConsulta(), "Referencia", "Observaciones");
        FacesContext faces = mock(FacesContext.class);
        try (MockedStatic<FacesContext> contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            assertNull(model.cerrar());
            assertEquals(consulta.getIdConsulta(), sesion.getConsulta());
            verify(faces).validationFailed();
            verify(faces).addMessage(isNull(), argThat(m ->
                    "Complete los pasos pendientes.".equals(m.getSummary())));
        }
    }
}
