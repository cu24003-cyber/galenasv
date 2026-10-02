package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Persona;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Documento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.MedioContacto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.PersonaRepository;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaServiceTest {

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private EntityManager em;

    @InjectMocks
    private PersonaService personaService;

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        Persona p = new Persona();
        personaService.crear(p);
        assertNotNull(p.getIdPersona());
        verify(personaRepository).create(p);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        Persona p = new Persona();
        p.setIdPersona(idFijo);
        personaService.crear(p);
        assertEquals(idFijo, p.getIdPersona());
    }

    @Test
    void crear_asignaFechaCreacion_cuandoEsNula() {
        Persona p = new Persona();
        assertNull(p.getFechaCreacion());
        personaService.crear(p);
        assertNotNull(p.getFechaCreacion());
    }

    @Test
    void crear_noSobreescribeFechaCreacion_siYaTieneUna() {
        Date fechaFija = new Date(0L);
        Persona p = new Persona();
        p.setFechaCreacion(fechaFija);
        personaService.crear(p);
        assertEquals(fechaFija, p.getFechaCreacion());
    }

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        Persona esperada = new Persona();
        when(personaRepository.find(id)).thenReturn(esperada);

        Persona resultado = personaService.buscarPorId(id);

        assertSame(esperada, resultado);
        verify(personaRepository).find(id);
    }

    @Test
    void eliminar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();

        ServiceException error = assertThrows(ServiceException.class, () -> personaService.eliminar(id));
        assertEquals("error.noExisteEliminar", error.getMessageKey());
        verify(em).find(Persona.class, id, LockModeType.PESSIMISTIC_WRITE);
        verify(em, never()).remove(any());
        verifyNoInteractions(personaRepository);
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        Persona e = new Persona();
        e.setIdPersona(id);
        when(personaRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> personaService.actualizar(e));
        verify(personaRepository, never()).update(any());
    }

    @Test
    void crear_lanzaServiceException_porErrorDePersistencia() {
        Persona e = new Persona();
        doThrow(new PersistenceException("Error BD")).when(personaRepository).create(any());

        assertThrows(ServiceException.class, () -> personaService.crear(e));
    }

    @Test
    void actualizar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        Persona e = new Persona();
        e.setIdPersona(id);
        when(personaRepository.find(id)).thenReturn(e);
        doThrow(new PersistenceException("Error BD")).when(personaRepository).update(e);

        assertThrows(ServiceException.class, () -> personaService.actualizar(e));
    }

    @Test
    void eliminar_lanzaServiceException_porErrorDePersistencia() {
        UUID id = UUID.randomUUID();
        PersistenceException causa = new PersistenceException("Error BD");
        when(em.find(Persona.class, id, LockModeType.PESSIMISTIC_WRITE)).thenThrow(causa);

        ServiceException error = assertThrows(ServiceException.class, () -> personaService.eliminar(id));
        assertEquals("persona.eliminar.error", error.getMessageKey());
        assertSame(causa, error.getCause());
        verify(em, never()).remove(any());
    }

    @Test
    void eliminar_rechazaIdNuloSinAccederALaBase() {
        ServiceException error = assertThrows(ServiceException.class, () -> personaService.eliminar(null));

        assertEquals("error.noExisteEliminar", error.getMessageKey());
        verifyNoInteractions(em, personaRepository);
    }

    @Test
    void eliminar_bloqueaPersonaConConsultasSinEliminarSusDatos() {
        Persona persona = prepararEliminacion(1, 0);

        ServiceException error = assertThrows(ServiceException.class,
                () -> personaService.eliminar(persona.getIdPersona()));

        assertEquals("persona.eliminar.historial", error.getMessageKey());
        verify(em, never()).remove(any());
        verify(em, never()).flush();
    }

    @Test
    void eliminar_bloqueaPersonalConPasosClinicosAunqueNoSeaPaciente() {
        Persona persona = prepararEliminacion(0, 1);

        ServiceException error = assertThrows(ServiceException.class,
                () -> personaService.eliminar(persona.getIdPersona()));

        assertEquals("persona.eliminar.historial", error.getMessageKey());
        verify(em, never()).remove(any());
        verify(em, never()).flush();
    }

    @Test
    void eliminar_sinHistorialBorraTodosLosDependientesAntesDeLaPersona() {
        Persona persona = prepararEliminacion(0, 0);
        Documento documento = new Documento(UUID.randomUUID());
        MedioContacto contacto = new MedioContacto(UUID.randomUUID());
        prepararDatosPersonales(persona, List.of(documento), List.of(contacto));

        personaService.eliminar(persona.getIdPersona());

        var orden = inOrder(em);
        orden.verify(em).remove(documento);
        orden.verify(em).remove(contacto);
        orden.verify(em, times(2)).remove(isA(PersonaRol.class));
        orden.verify(em).flush();
        orden.verify(em).remove(persona);
        orden.verify(em).flush();
        verify(em, times(5)).remove(any());
        verifyNoInteractions(personaRepository);
    }

    @Test
    void eliminar_permitePersonaSinDocumentosNiContactos() {
        Persona persona = prepararEliminacion(0, 0);
        prepararDatosPersonales(persona, List.of(), List.of());

        assertDoesNotThrow(() -> personaService.eliminar(persona.getIdPersona()));

        verify(em).remove(persona);
        verify(em, times(2)).remove(isA(PersonaRol.class));
        verify(em, times(3)).remove(any());
    }

    @Test
    void eliminar_permitePersonaSinNingunDatoRelacionado() {
        Persona persona = prepararEliminacion(0, 0, List.of());
        prepararDatosPersonales(persona, List.of(), List.of());

        assertDoesNotThrow(() -> personaService.eliminar(persona.getIdPersona()));

        verify(em).remove(persona);
        verify(em, times(1)).remove(any());
    }

    @Test
    void eliminar_traduceFalloAlBorrarDependientesSinBorrarLaPersona() {
        Persona persona = prepararEliminacion(0, 0);
        prepararDatosPersonales(persona, List.of(), List.of());
        PersistenceException causa = new PersistenceException("Fallo al eliminar asignaciones");
        doThrow(causa).when(em).flush();

        ServiceException error = assertThrows(ServiceException.class,
                () -> personaService.eliminar(persona.getIdPersona()));

        assertEquals("persona.eliminar.error", error.getMessageKey());
        assertSame(causa, error.getCause());
        verify(em, never()).remove(persona);
    }

    @Test
    void eliminar_traduceFalloFinalParaQueLaTransaccionSeRevierta() {
        Persona persona = prepararEliminacion(0, 0);
        prepararDatosPersonales(persona, List.of(), List.of());
        PersistenceException causa = new PersistenceException("Fallo al eliminar persona");
        doNothing().doThrow(causa).when(em).flush();

        ServiceException error = assertThrows(ServiceException.class,
                () -> personaService.eliminar(persona.getIdPersona()));

        assertEquals("persona.eliminar.error", error.getMessageKey());
        assertSame(causa, error.getCause());
        verify(em).remove(persona);
    }

    @Test
    void buscarPorNombre_devuelveCoincidenciasDelRepositorio() {
        java.util.List<Persona> personas = java.util.List.of(new Persona());
        when(personaRepository.buscarPorNombre("Ana")).thenReturn(personas);

        assertSame(personas, personaService.buscarPorNombre("Ana"));
    }

    @Test
    void buscarPorNombre_traduceErroresDePersistencia() {
        when(personaRepository.buscarPorNombre("Ana"))
                .thenThrow(new PersistenceException("Error BD"));

        ServiceException error = assertThrows(ServiceException.class,
                () -> personaService.buscarPorNombre("Ana"));

        assertEquals("error.cargarPersonas", error.getMessageKey());
    }

    @Test
    void rechazaNacimientoFuturoAlCrearYActualizar() {
        Persona persona = new Persona(UUID.randomUUID());
        persona.setFechaNacimiento(fechaEnDias(1));
        assertThrows(ServiceException.class, () -> personaService.crear(persona));
        assertThrows(ServiceException.class, () -> personaService.actualizar(persona));
        verifyNoInteractions(personaRepository);
    }

    @Test
    void permiteNacimientoHoyYRechazaCreacionFutura() {
        Persona persona = new Persona();
        persona.setFechaNacimiento(fechaEnDias(0));
        personaService.crear(persona);
        verify(personaRepository).create(persona);
        persona.setFechaCreacion(fechaEnDias(1));
        assertThrows(ServiceException.class, () -> personaService.actualizar(persona));
        verify(personaRepository, never()).update(any());
    }

    private Date fechaEnDias(int dias) {
        java.time.ZoneId zona = java.time.ZoneId.of("America/El_Salvador");
        return Date.from(java.time.LocalDate.now(zona).plusDays(dias).atStartOfDay(zona).toInstant());
    }

    private Persona prepararEliminacion(long consultas, long pasos) {
        return prepararEliminacion(consultas, pasos,
                List.of(new PersonaRol(UUID.randomUUID()), new PersonaRol(UUID.randomUUID())));
    }

    private Persona prepararEliminacion(long consultas, long pasos, List<PersonaRol> asignaciones) {
        Persona persona = new Persona(UUID.randomUUID());
        when(em.find(Persona.class, persona.getIdPersona(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(persona);
        TypedQuery<PersonaRol> query = prepararLista(
                "SELECT pr FROM PersonaRol pr WHERE pr.idPersona = :persona ORDER BY pr.idPersonaRol",
                PersonaRol.class, persona, asignaciones);
        when(query.setLockMode(LockModeType.PESSIMISTIC_WRITE)).thenReturn(query);
        prepararConteo("SELECT COUNT(c) FROM Consulta c WHERE c.idPersonaRol.idPersona = :persona", persona, consultas);
        prepararConteo("SELECT COUNT(p) FROM ConsultaProcedimientoPaso p WHERE p.idPersonaRol.idPersona = :persona", persona, pasos);
        return persona;
    }

    private void prepararDatosPersonales(Persona persona, List<Documento> documentos, List<MedioContacto> contactos) {
        prepararLista("SELECT d FROM Documento d WHERE d.idPersona = :persona", Documento.class, persona, documentos);
        prepararLista("SELECT m FROM MedioContacto m WHERE m.idPersona = :persona", MedioContacto.class, persona, contactos);
    }

    @SuppressWarnings("unchecked")
    private <T> TypedQuery<T> prepararLista(String jpql, Class<T> tipo, Persona persona, List<T> resultado) {
        TypedQuery<T> query = mock(TypedQuery.class);
        when(em.createQuery(jpql, tipo)).thenReturn(query);
        when(query.setParameter("persona", persona)).thenReturn(query);
        when(query.getResultList()).thenReturn(resultado);
        return query;
    }

    @SuppressWarnings("unchecked")
    private void prepararConteo(String jpql, Persona persona, long resultado) {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(em.createQuery(jpql, Long.class)).thenReturn(query);
        when(query.setParameter("persona", persona)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(resultado);
    }
}
