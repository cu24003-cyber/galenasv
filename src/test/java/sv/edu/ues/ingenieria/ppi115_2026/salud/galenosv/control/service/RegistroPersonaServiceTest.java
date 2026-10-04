package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.PersonaRolRepository;

@ExtendWith(MockitoExtension.class)
class RegistroPersonaServiceTest {
    @Mock PersonaService personas;
    @Mock RolService roles;
    @Mock ClinicaService clinicas;
    @Mock PersonaRolService asignaciones;
    @Mock PersonaRolRepository repositorio;
    @InjectMocks RegistroPersonaService servicio;

    private Rol rol;
    private Clinica clinica;

    private void catalogos() {
        rol = new Rol(UUID.randomUUID());
        rol.setActivo(true);
        clinica = new Clinica(UUID.randomUUID());
        clinica.setActivo(true);
        when(roles.buscarPorId(rol.getIdRol())).thenReturn(rol);
        when(clinicas.buscarPorId(clinica.getIdClinica())).thenReturn(clinica);
    }

    @Test
    void creaPersonaYRelacionConClinica() {
        catalogos();
        Persona persona = new Persona();
        doAnswer(i -> { persona.setIdPersona(UUID.randomUUID()); return null; }).when(personas).crear(persona);
        servicio.guardar(persona, rol, clinica, null);
        var captor = org.mockito.ArgumentCaptor.forClass(PersonaRol.class);
        verify(asignaciones).crear(captor.capture());
        assertSame(persona, captor.getValue().getIdPersona());
        assertSame(rol, captor.getValue().getIdRol());
        assertSame(clinica, captor.getValue().getIdClinica());
    }

    @Test
    void actualizarConservaIdDeRelacionYNoCreaDuplicados() {
        catalogos();
        Persona persona = new Persona(UUID.randomUUID());
        PersonaRol relacion = new PersonaRol(UUID.randomUUID());
        relacion.setIdPersona(persona);
        when(asignaciones.buscarPorId(relacion.getIdPersonaRol())).thenReturn(relacion);
        assertEquals(relacion.getIdPersonaRol(),
                servicio.guardar(persona, rol, clinica, relacion.getIdPersonaRol()));
        verify(personas).actualizar(persona);
        verify(asignaciones).actualizar(relacion);
        verify(asignaciones, never()).crear(any());
    }

    @Test
    void rechazaRelacionDeOtraPersonaAntesDeGuardar() {
        catalogos();
        PersonaRol relacion = new PersonaRol(UUID.randomUUID());
        relacion.setIdPersona(new Persona(UUID.randomUUID()));
        when(asignaciones.buscarPorId(relacion.getIdPersonaRol())).thenReturn(relacion);
        assertThrows(ServiceException.class, () ->
                servicio.guardar(new Persona(UUID.randomUUID()), rol, clinica, relacion.getIdPersonaRol()));
        verifyNoInteractions(personas);
        verify(asignaciones, never()).actualizar(any());
    }

    @Test
    void rechazaCatalogosInactivosAntesDeGuardar() {
        catalogos();
        rol.setActivo(false);
        assertThrows(ServiceException.class, () -> servicio.guardar(new Persona(), rol, clinica, null));
        verifyNoInteractions(personas, asignaciones);
    }

    @Test
    void creaPersonaSinRolNiClinica() {
        Persona persona = new Persona();
        assertNull(servicio.guardar(persona, null, null, null));
        verify(personas).crear(persona);
        verifyNoInteractions(roles, clinicas, asignaciones);
    }

    @Test
    void actualizaPersonaSinRol() {
        Persona persona = new Persona(UUID.randomUUID());
        assertNull(servicio.guardar(persona, null, null, null));
        verify(personas).actualizar(persona);
        verifyNoInteractions(asignaciones);
    }

    @Test
    void omitirRolNoEliminaAsignacionesExistentes() {
        Persona persona = new Persona(UUID.randomUUID());
        PersonaRol relacion = new PersonaRol(UUID.randomUUID()); relacion.setIdPersona(persona);
        when(asignaciones.buscarPorId(relacion.getIdPersonaRol())).thenReturn(relacion);
        assertEquals(relacion.getIdPersonaRol(), servicio.guardar(persona, null, null, relacion.getIdPersonaRol()));
        verify(personas).actualizar(persona);
        verify(asignaciones, never()).actualizar(any());
        verify(asignaciones, never()).eliminar(any());
    }

    @Test
    void rechazaRolSinClinicaAntesDeGuardarPersona() {
        assertThrows(ServiceException.class, () -> servicio.guardar(new Persona(), new Rol(), null, null));
        verifyNoInteractions(personas, asignaciones);
    }

    @Test
    void creaPertenenciaALaClinicaSinAsignarUnRol() {
        Persona persona = new Persona();
        Clinica clinica = new Clinica(UUID.randomUUID()); clinica.setActivo(true);
        when(clinicas.buscarPorId(clinica.getIdClinica())).thenReturn(clinica);
        doAnswer(i -> { ((PersonaRol)i.getArgument(0)).setIdPersonaRol(UUID.randomUUID()); return null; })
                .when(asignaciones).crear(any());
        UUID id = servicio.guardar(persona, null, clinica, null);
        var captor = org.mockito.ArgumentCaptor.forClass(PersonaRol.class);
        verify(asignaciones).crear(captor.capture()); verify(personas).crear(persona);
        assertNotNull(id); assertNull(captor.getValue().getIdRol());
        assertSame(clinica, captor.getValue().getIdClinica()); assertSame(persona, captor.getValue().getIdPersona());
        verifyNoInteractions(roles);
    }

    @Test void editarPertenenciaSinRolActualizaLaMismaRelacion() {
        Persona persona = new Persona(UUID.randomUUID());
        Clinica clinica = new Clinica(UUID.randomUUID()); clinica.setActivo(true);
        PersonaRol pertenencia = new PersonaRol(UUID.randomUUID()); pertenencia.setIdPersona(persona);
        when(clinicas.buscarPorId(clinica.getIdClinica())).thenReturn(clinica);
        when(asignaciones.buscarPorId(pertenencia.getIdPersonaRol())).thenReturn(pertenencia);
        assertEquals(pertenencia.getIdPersonaRol(), servicio.guardar(persona, null, clinica, pertenencia.getIdPersonaRol()));
        verify(asignaciones).actualizar(pertenencia); verify(asignaciones, never()).crear(any());
        assertSame(clinica, pertenencia.getIdClinica()); assertNull(pertenencia.getIdRol());
    }

    @Test void clinicaInactivaSinRolNoGuardaLaPersona() {
        Clinica clinica = new Clinica(UUID.randomUUID()); clinica.setActivo(false);
        when(clinicas.buscarPorId(clinica.getIdClinica())).thenReturn(clinica);
        assertThrows(ServiceException.class, () -> servicio.guardar(new Persona(), null, clinica, null));
        verifyNoInteractions(personas, asignaciones);
    }
}
