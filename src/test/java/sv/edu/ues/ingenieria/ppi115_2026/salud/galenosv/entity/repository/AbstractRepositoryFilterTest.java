package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Documento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Persona;

@ExtendWith(MockitoExtension.class)
class AbstractRepositoryFilterTest {
    @Mock EntityManager em;
    @Mock TypedQuery<Persona> personas;
    @Mock TypedQuery<Documento> documentos;

    @Test
    void clinicaYRolSeAplicanALaMismaAsignacionSinDuplicarPersonas() {
        PersonaRepository repository = new PersonaRepository();
        repository.em = em;
        UUID clinica = UUID.randomUUID();
        UUID rol = UUID.randomUUID();
        when(em.createQuery(anyString(), eq(Persona.class))).thenReturn(personas);
        List<Persona> resultado = List.of(new Persona(UUID.randomUUID()));
        when(personas.getResultList()).thenReturn(resultado);

        assertSame(resultado, repository.findByFilters(Map.of(
                "personaRolCollection.idClinica.idClinica", clinica,
                "personaRolCollection.idRol.idRol", rol)));

        verify(em).createQuery("SELECT DISTINCT e FROM Persona e"
                + " JOIN e.personaRolCollection r0 JOIN r0.idClinica r1 JOIN r0.idRol r2"
                + " WHERE r1.idClinica = :f0 AND r2.idRol = :f1", Persona.class);
        verify(personas).setParameter("f0", clinica);
        verify(personas).setParameter("f1", rol);
    }

    @Test
    void documentosSeFiltranPorPersonaYTipoConParametros() {
        DocumentoRepository repository = new DocumentoRepository();
        repository.em = em;
        UUID persona = UUID.randomUUID();
        UUID tipo = UUID.randomUUID();
        when(em.createQuery(anyString(), eq(Documento.class))).thenReturn(documentos);
        when(documentos.getResultList()).thenReturn(List.of());

        assertTrue(repository.findByFilters(Map.of(
                "idPersona.idPersona", persona, "idTipoDocumento.idTipoDocumento", tipo)).isEmpty());

        verify(em).createQuery("SELECT DISTINCT e FROM Documento e"
                + " JOIN e.idPersona r0 JOIN e.idTipoDocumento r1"
                + " WHERE r0.idPersona = :f0 AND r1.idTipoDocumento = :f1", Documento.class);
        verify(documentos).setParameter("f0", persona);
        verify(documentos).setParameter("f1", tipo);
    }

    @Test
    void sinFiltrosNoAgregaJoinsQueExcluyanPersonasSinRoles() {
        PersonaRepository repository = new PersonaRepository();
        repository.em = em;
        when(em.createQuery("SELECT e FROM Persona e", Persona.class)).thenReturn(personas);
        when(personas.getResultList()).thenReturn(List.of(new Persona()));

        assertEquals(1, repository.findByFilters(Map.of()).size());
        verify(personas, never()).setParameter(anyString(), any());
    }

    @Test
    void rechazaFragmentosDeConsultaComoRutasDeAtributos() {
        PersonaRepository repository = new PersonaRepository();
        repository.em = em;

        assertThrows(IllegalArgumentException.class,
                () -> repository.findByFilters(Map.of("idPersona OR 1=1", UUID.randomUUID())));

        verifyNoInteractions(em);
    }

    @Test
    void pertenenciaCompruebaElRegistroYElPadreEnLaMismaConsulta() {
        DocumentoRepository repository = new DocumentoRepository();
        repository.em = em;
        UUID id = UUID.randomUUID();
        UUID persona = UUID.randomUUID();
        Documento registro = new Documento(id);
        when(em.getReference(Documento.class, id)).thenReturn(registro);
        when(em.createQuery(anyString(), eq(Documento.class))).thenReturn(documentos);
        when(documentos.setMaxResults(1)).thenReturn(documentos);
        when(documentos.getResultList()).thenReturn(List.of());

        assertFalse(repository.matchesFilters(id, Map.of("idPersona.idPersona", persona)));

        verify(em).createQuery("SELECT DISTINCT e FROM Documento e JOIN e.idPersona r0"
                + " WHERE r0.idPersona = :f0 AND e = :registro", Documento.class);
        verify(documentos).setParameter("f0", persona);
        verify(documentos).setParameter("registro", registro);
    }
}
