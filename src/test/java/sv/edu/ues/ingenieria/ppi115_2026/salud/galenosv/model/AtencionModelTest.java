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
        assertEquals("/paginas/consultas.xhtml?faces-redirect=true", model.cerrar());
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

    @Test void procedimientoSeAbreSoloAlPedirloYCancelaEnDosPasos() {
        assertFalse(model.isProcedimientoVisible()); model.nuevoProcedimiento();
        assertTrue(model.isProcedimientoVisible()); model.setNotasProcedimiento("Borrador");
        try (var estado = mockStatic(FormularioCancelacion.class)) {
            estado.when(FormularioCancelacion::estaVacio).thenReturn(false, true);
            model.cancelarProcedimiento(); assertNull(model.getNotasProcedimiento()); assertTrue(model.isProcedimientoVisible());
            model.cancelarProcedimiento(); assertFalse(model.isProcedimientoVisible());
        }
        assertEquals(consulta.getIdConsulta(), sesion.getConsulta());
    }
    @Test void agregarProcedimientoOcultaSoloDespuesDeGuardarlo() {
        var responsable = new sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol(UUID.randomUUID());
        when(sesion.getRolActivo()).thenReturn(responsable);
        UUID procedimiento = UUID.randomUUID(); model.nuevoProcedimiento(); model.setProcedimientoId(procedimiento.toString());
        model.agregar(); assertFalse(model.isProcedimientoVisible()); assertNull(model.getProcedimientoId());
        verify(servicio).agregarProcedimiento(consulta.getIdConsulta(), procedimiento, null, responsable.getIdPersonaRol());
        assertEquals(consulta.getIdConsulta(), sesion.getConsulta());
    }
    @Test void procedimientoRechazadoConservaFormularioParaCorregir() {
        var responsable = new sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol(UUID.randomUUID());
        when(sesion.getRolActivo()).thenReturn(responsable);
        UUID procedimiento = UUID.randomUUID(); model.nuevoProcedimiento(); model.setProcedimientoId(procedimiento.toString());
        doThrow(new ServiceException("Faltan responsables"))
                .when(servicio).agregarProcedimiento(consulta.getIdConsulta(), procedimiento, null, responsable.getIdPersonaRol());
        FacesContext faces = mock(FacesContext.class);
        try (var contexto = mockStatic(FacesContext.class)) {
            contexto.when(FacesContext::getCurrentInstance).thenReturn(faces);
            model.agregar(); assertTrue(model.isProcedimientoVisible()); assertEquals(procedimiento.toString(), model.getProcedimientoId());
            verify(faces).validationFailed();
        }
    }
}
