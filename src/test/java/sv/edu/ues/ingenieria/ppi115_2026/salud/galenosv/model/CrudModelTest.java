package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrudModelTest {
    private final PersonaService personas = mock(PersonaService.class);
    private final RolService roles = mock(RolService.class);
    private final FacesContext context = mock(FacesContext.class);
    private MockedStatic<FacesContext> current;
    private MockedStatic<Mensajes> messages;
    private PersonaModel personaModel;
    private RolModel rolModel;

    @BeforeEach
    void setup() throws Exception {
        current = mockStatic(FacesContext.class);
        current.when(FacesContext::getCurrentInstance).thenReturn(context);
        messages = mockStatic(Mensajes.class);
        personaModel = new PersonaModel();
        rolModel = new RolModel();
        inject(personaModel, "personaService", personas);
        inject(rolModel, "rolService", roles);
    }

    @AfterEach
    void close() { messages.close(); current.close(); }

    private void inject(Object target, String field, Object service) throws Exception {
        var member = target.getClass().getDeclaredField(field);
        member.setAccessible(true);
        member.set(target, service);
    }

    @Test
    void deletingPersonReloadsListAndClearsSelection() {
        Persona persona = new Persona();
        persona.setIdPersona(UUID.randomUUID());
        personaModel.setSeleccionada(persona);
        when(personas.listarTodos()).thenReturn(List.of());
        personaModel.eliminar(persona);
        verify(personas).eliminar(persona.getIdPersona());
        verify(personas).listarTodos();
        assertNull(personaModel.getSeleccionada());
    }

    @Test
    void failedPersonDeletionPreservesSelectionAndReportsError() {
        Persona persona = new Persona();
        persona.setIdPersona(UUID.randomUUID());
        personaModel.setSeleccionada(persona);
        doThrow(new ServiceException("used")).when(personas).eliminar(persona.getIdPersona());
        personaModel.eliminar(persona);
        assertSame(persona, personaModel.getSeleccionada());
        verify(context).validationFailed();
        verify(context).addMessage(isNull(), any());
        verify(personas, never()).listarTodos();
    }

    @Test
    void newRoleSavesAndReloads() {
        rolModel.newRol();
        Rol rol = rolModel.getRolSeleccionado();
        assertTrue(rol.getActivo());
        rol.setNombre("Test");
        rolModel.saveRol();
        verify(roles).crear(rol);
        verify(roles).listarTodos();
        assertNull(rolModel.getRolSeleccionado());
    }

    @Test
    void failedCreationCanRetryEvenAfterServiceAssignedId() {
        rolModel.newRol();
        Rol rol = rolModel.getRolSeleccionado();
        rol.setNombre("Test");
        doAnswer(call -> {
            rol.setIdRol(UUID.randomUUID());
            throw new ServiceException("failure");
        }).doNothing().when(roles).crear(rol);
        rolModel.saveRol();
        assertSame(rol, rolModel.getRolSeleccionado());
        verify(context).validationFailed();
        rolModel.saveRol();
        verify(roles, times(2)).crear(rol);
        verify(roles, never()).actualizar(any());
    }

    @Test
    void cancellingEditDoesNotChangeOriginalRow() {
        Rol original = new Rol(UUID.randomUUID());
        original.setNombre("Original");
        rolModel.editRol(original);
        rolModel.getRolSeleccionado().setNombre("Changed");
        rolModel.cancelRol();
        assertEquals("Original", original.getNombre());
        assertNull(rolModel.getRolSeleccionado());
        verifyNoInteractions(roles);
    }

    @Test
    void editingUsesUpdateAndDeletionReloadsList() {
        Rol original = new Rol(UUID.randomUUID());
        original.setNombre("Original");
        rolModel.editRol(original);
        Rol edited = rolModel.getRolSeleccionado();
        rolModel.saveRol();
        verify(roles).actualizar(edited);
        rolModel.deleteRol(original);
        verify(roles).eliminar(original.getIdRol());
        verify(roles, times(2)).listarTodos();
    }

    @Test
    void blankRoleNeverReachesService() {
        rolModel.newRol();
        rolModel.getRolSeleccionado().setNombre("  ");
        rolModel.saveRol();
        verifyNoInteractions(roles);
        verify(context).validationFailed();
    }

    @Test
    void failedRoleDeletionReportsErrorAndDoesNotReload() {
        Rol original = new Rol(UUID.randomUUID());
        doThrow(new ServiceException("used")).when(roles).eliminar(original.getIdRol());
        rolModel.deleteRol(original);
        verify(context).validationFailed();
        verify(context).addMessage(isNull(), any());
        verify(roles, never()).listarTodos();
    }
}
