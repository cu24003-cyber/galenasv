package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtencionServiceTest {
    @Mock EntityManager em;
    @InjectMocks AtencionService servicio;
    private final UUID id=UUID.randomUUID();
    private Consulta abierta() {
        Consulta c=new Consulta(id);
        c.setFechaInicio(OffsetDateTime.parse("2026-09-29T10:15:30-06:00"));
        c.setIdPersonaRol(new PersonaRol(UUID.randomUUID()));
        when(em.find(Consulta.class,id,LockModeType.PESSIMISTIC_WRITE)).thenReturn(c);
        return c;
    }
    @SuppressWarnings("unchecked")
    private <T> TypedQuery<T> query(Class<T> tipo,List<T> resultado) {
        TypedQuery<T> q=mock(TypedQuery.class);
        when(em.createQuery(anyString(),eq(tipo))).thenReturn(q);
        when(q.setParameter(anyString(),any())).thenReturn(q);
        when(q.getResultList()).thenReturn(resultado); return q;
    }
    @Test void retomaConsultaExistenteSinCambiarInicio() {
        PersonaRol pr=new PersonaRol(UUID.randomUUID()); Rol rol=new Rol(); rol.setNombre("Paciente"); rol.setActivo(true); pr.setIdRol(rol);
        when(em.find(PersonaRol.class,pr.getIdPersonaRol(),LockModeType.PESSIMISTIC_WRITE)).thenReturn(pr);
        Consulta c=new Consulta(id); c.setFechaInicio(OffsetDateTime.parse("2026-09-29T10:15:30-06:00"));
        TypedQuery<Consulta> q=query(Consulta.class,List.of(c)); when(q.getSingleResult()).thenReturn(c);
        assertSame(c,servicio.abrir(pr.getIdPersonaRol()));
        assertEquals(OffsetDateTime.parse("2026-09-29T10:15:30-06:00"),c.getFechaInicio()); verify(em,never()).persist(any());
    }
    @Test void noPermiteModificarConsultaCerrada() {
        Consulta c=abierta(); c.setFechaFin(OffsetDateTime.now());
        assertThrows(ServiceException.class,()->servicio.guardar(id,"cambio","cambio"));
        assertNull(c.getReferenciaExterna());
    }
    @Test void noPermiteOrdenarPasoDeOtroPaciente() {
        abierta(); UUID pasoId=UUID.randomUUID();
        ConsultaProcedimiento cp=new ConsultaProcedimiento(); cp.setIdConsulta(new Consulta(UUID.randomUUID()));
        ConsultaProcedimientoPaso paso=new ConsultaProcedimientoPaso(pasoId); paso.setIdConsultaProcedimiento(cp);
        when(em.find(ConsultaProcedimientoPaso.class,pasoId)).thenReturn(paso);
        assertThrows(ServiceException.class,()->servicio.ordenar(id,pasoId,"Hemograma"));
        verify(em,never()).persist(any());
    }
    @Test void cierreConPendientesNoModificaFechasNiNotas() {
        Consulta c=abierta(); query(ConsultaProcedimientoPaso.class,List.of(new ConsultaProcedimientoPaso()));
        assertThrows(ServiceException.class,()->servicio.cerrar(id,"ref","notas"));
        assertNull(c.getFechaFin()); assertNull(c.getObservaciones());
    }
    @Test void cierreUsaMismoInstanteParaConsultaYProcedimientos() {
        Consulta c=abierta(); OffsetDateTime inicio=c.getFechaInicio();
        ConsultaProcedimientoPaso paso=new ConsultaProcedimientoPaso(); paso.setFechaFin(OffsetDateTime.now());
        ConsultaProcedimiento cp=new ConsultaProcedimiento(); cp.setFechaInicio(inicio);
        query(ConsultaProcedimientoPaso.class,List.of(paso)); query(ConsultaProcedimiento.class,List.of(cp));
        servicio.cerrar(id,"referencia","observación");
        assertEquals(c.getFechaFin(),cp.getFechaFin()); assertNotNull(c.getFechaFin().getOffset());
        assertEquals(inicio,c.getFechaInicio()); assertEquals("observación",c.getObservaciones());
    }
    @Test void procedimientoCopiaInicioYRolPacienteEnTodosLosPasos() {
        Consulta c=abierta(); UUID procedimiento=UUID.randomUUID();
        Procedimiento p=new Procedimiento(procedimiento); p.setActivo(true);
        when(em.find(Procedimiento.class,procedimiento)).thenReturn(p);
        ProcedimientoPaso paso=new ProcedimientoPaso(UUID.randomUUID());
        query(ProcedimientoPaso.class,List.of(paso));
        servicio.agregarProcedimiento(id,procedimiento,"notas");
        ArgumentCaptor<Object> captor=ArgumentCaptor.forClass(Object.class); verify(em,times(2)).persist(captor.capture());
        ConsultaProcedimiento cp=(ConsultaProcedimiento)captor.getAllValues().get(0);
        ConsultaProcedimientoPaso ejecucion=(ConsultaProcedimientoPaso)captor.getAllValues().get(1);
        assertEquals(c.getFechaInicio(),cp.getFechaInicio()); assertNull(cp.getFechaFin());
        assertEquals(c.getIdPersonaRol(),ejecucion.getIdPersonaRol()); assertEquals(paso,ejecucion.getIdProcedimientoPaso());
        assertEquals(c.getFechaInicio(),ejecucion.getFechaInicio()); assertEquals("PENDIENTE",ejecucion.getEstado());
    }
    @Test void rechazaRolQueNoEsPaciente() {
        PersonaRol pr=new PersonaRol(id); Rol rol=new Rol(); rol.setNombre("Médico"); pr.setIdRol(rol);
        when(em.find(PersonaRol.class,id,LockModeType.PESSIMISTIC_WRITE)).thenReturn(pr);
        assertThrows(ServiceException.class,()->servicio.abrir(id)); verify(em,never()).persist(any());
    }
    @Test void respetaDependenciasAntesDeCompletarPaso() {
        Consulta c=abierta(); UUID pasoId=UUID.randomUUID();
        ConsultaProcedimiento cp=new ConsultaProcedimiento(UUID.randomUUID()); cp.setIdConsulta(c);
        ConsultaProcedimientoPaso paso=new ConsultaProcedimientoPaso(pasoId); paso.setIdConsultaProcedimiento(cp); paso.setIdProcedimientoPaso(new ProcedimientoPaso(UUID.randomUUID()));
        when(em.find(ConsultaProcedimientoPaso.class,pasoId)).thenReturn(paso);
        ProcedimientoPasoSecuencia s=new ProcedimientoPasoSecuencia(); s.setIdProcedimientoPaso(new ProcedimientoPaso(UUID.randomUUID()));
        query(ProcedimientoPasoSecuencia.class,List.of(s));
        @SuppressWarnings("unchecked") TypedQuery<Long> cuenta=mock(TypedQuery.class);
        when(em.createQuery(anyString(),eq(Long.class))).thenReturn(cuenta); when(cuenta.setParameter(anyString(),any())).thenReturn(cuenta); when(cuenta.getSingleResult()).thenReturn(1L);
        assertThrows(ServiceException.class,()->servicio.completar(id,pasoId)); assertNull(paso.getFechaFin());
    }
    @Test void registraExamenClasificacionYRelacionConPaso() {
        Consulta c=abierta(); UUID pasoId=UUID.randomUUID();
        ConsultaProcedimiento cp=new ConsultaProcedimiento(UUID.randomUUID()); cp.setIdConsulta(c);
        ProcedimientoPaso definicion=new ProcedimientoPaso(UUID.randomUUID());
        ConsultaProcedimientoPaso paso=new ConsultaProcedimientoPaso(pasoId);
        paso.setIdConsultaProcedimiento(cp); paso.setIdProcedimientoPaso(definicion);
        when(em.find(ConsultaProcedimientoPaso.class,pasoId)).thenReturn(paso);
        TipoExamen tipo=new TipoExamen(UUID.randomUUID()); tipo.setActivo(true);
        when(em.find(TipoExamen.class,tipo.getIdTipoExamen())).thenReturn(tipo);
        servicio.examen(id,pasoId,"Hemograma","Observaciones",tipo.getIdTipoExamen());
        ArgumentCaptor<Object> captor=ArgumentCaptor.forClass(Object.class);
        verify(em,times(3)).persist(captor.capture());
        Examen examen=(Examen)captor.getAllValues().get(0);
        ExamenTipoExamen clasificacion=(ExamenTipoExamen)captor.getAllValues().get(1);
        ProcedimientoPasoExamen relacion=(ProcedimientoPasoExamen)captor.getAllValues().get(2);
        assertSame(examen,clasificacion.getIdExamen()); assertSame(tipo,clasificacion.getIdTipoExamen());
        assertSame(examen,relacion.getIdExamen()); assertSame(definicion,relacion.getIdProcedimientoPaso());
        assertNotNull(clasificacion.getFechaCreacion().getOffset());
        assertNotNull(relacion.getFechaCreacion().getOffset());
    }
    @Test void ordenGuardaFechaAutomaticaYRelacionDelPaciente() {
        Consulta c=abierta(); UUID pasoId=UUID.randomUUID();
        ConsultaProcedimiento cp=new ConsultaProcedimiento(UUID.randomUUID()); cp.setIdConsulta(c);
        ConsultaProcedimientoPaso paso=new ConsultaProcedimientoPaso(pasoId);
        paso.setIdConsultaProcedimiento(cp); paso.setIdPersonaRol(c.getIdPersonaRol());
        when(em.find(ConsultaProcedimientoPaso.class,pasoId)).thenReturn(paso);
        servicio.ordenar(id,pasoId,"Hemograma en ayunas");
        ArgumentCaptor<OrdenExamen> captor=ArgumentCaptor.forClass(OrdenExamen.class);
        verify(em).persist(captor.capture());
        assertSame(paso,captor.getValue().getIdConsultaProcedimientoPaso());
        assertNotNull(captor.getValue().getFechaCreacion().getOffset());
        assertEquals("Hemograma en ayunas",captor.getValue().getIndicaciones());
    }
}
