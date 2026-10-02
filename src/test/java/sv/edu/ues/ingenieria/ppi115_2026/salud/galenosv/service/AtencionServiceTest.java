package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.OffsetDateTime;
import java.util.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtencionServiceTest {
    @Mock EntityManager em;
    @Mock sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model.AtencionSesion sesion;
    @InjectMocks AtencionService servicio;
    private final UUID id=UUID.randomUUID();
    private PersonaRol medico;
    @BeforeEach void contextoMedico() {
        Clinica clinica = new Clinica(UUID.randomUUID());
        medico = new PersonaRol(UUID.randomUUID());
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre("Médico"); rol.setActivo(true);
        medico.setIdRol(rol); medico.setIdClinica(clinica);
        when(sesion.getRolActivo()).thenReturn(medico);
        when(em.find(PersonaRol.class, medico.getIdPersonaRol())).thenReturn(medico);
    }
    private Consulta abierta() {
        Consulta c=new Consulta(id);
        c.setFechaInicio(OffsetDateTime.parse("2026-09-29T10:15:30-06:00"));
        PersonaRol paciente = new PersonaRol(UUID.randomUUID()); paciente.setIdClinica(medico.getIdClinica());
        c.setIdPersonaRol(paciente); c.setIdMedicoRol(medico);
        when(em.find(Consulta.class,id,LockModeType.PESSIMISTIC_WRITE)).thenReturn(c);
        @SuppressWarnings("unchecked") TypedQuery<Consulta> consulta = mock(TypedQuery.class);
        when(em.createQuery(anyString(),eq(Consulta.class))).thenReturn(consulta);
        when(consulta.setParameter(anyString(),any())).thenReturn(consulta);
        when(consulta.getSingleResult()).thenReturn(c);
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
        Consulta c=new Consulta(id); c.setFechaInicio(OffsetDateTime.parse("2026-09-29T10:15:30-06:00")); c.setIdPersonaRol(pr); c.setIdMedicoRol(medico);
        TypedQuery<Consulta> q=query(Consulta.class,List.of(c)); when(q.getSingleResult()).thenReturn(c);
        Clinica clinica = new Clinica(UUID.randomUUID()); pr.setIdClinica(clinica);
        medico.setIdClinica(clinica);
        assertSame(c,servicio.abrir(pr.getIdPersonaRol(), medico.getIdPersonaRol()));
        assertEquals(OffsetDateTime.parse("2026-09-29T10:15:30-06:00"),c.getFechaInicio()); verify(em,never()).persist(any());
    }
    @Test void noPermiteModificarConsultaCerrada() {
        Consulta c=abierta(); c.setFechaFin(OffsetDateTime.now());
        assertThrows(ServiceException.class,()->servicio.guardar(id,"cambio","cambio"));
        assertNull(c.getReferenciaExterna());
    }
    @Test void impideLeerYModificarConsultaDeOtroMedico() {
        Consulta c = abierta(); c.setIdMedicoRol(new PersonaRol(UUID.randomUUID()));
        assertThrows(ServiceException.class, () -> servicio.cargar(id));
        assertThrows(ServiceException.class, () -> servicio.guardar(id, "otro", "otro"));
        assertNull(c.getReferenciaExterna());
    }
    @Test void impideLeerConsultaDeOtraClinica() {
        Consulta c = abierta(); c.getIdPersonaRol().setIdClinica(new Clinica(UUID.randomUUID()));
        assertThrows(ServiceException.class, () -> servicio.cargar(id));
    }
    @Test void impideAbrirConsultaDeOtroMedico() {
        PersonaRol paciente = new PersonaRol(UUID.randomUUID());
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre("Paciente"); rol.setActivo(true);
        paciente.setIdRol(rol); paciente.setIdClinica(medico.getIdClinica());
        when(em.find(PersonaRol.class, paciente.getIdPersonaRol(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(paciente);
        Consulta ajena = new Consulta(id); ajena.setIdPersonaRol(paciente);
        ajena.setIdMedicoRol(new PersonaRol(UUID.randomUUID()));
        query(Consulta.class, List.of(ajena));
        assertThrows(ServiceException.class, () -> servicio.abrir(paciente.getIdPersonaRol(), medico.getIdPersonaRol()));
        verify(em, never()).persist(any());
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
    @Test void procedimientoCopiaInicioYAsignaResponsableConCualquierRol() {
        Consulta c=abierta(); UUID procedimiento=UUID.randomUUID();
        Procedimiento p=new Procedimiento(procedimiento); p.setActivo(true);
        when(em.find(Procedimiento.class,procedimiento)).thenReturn(p);
        ProcedimientoPaso paso=new ProcedimientoPaso(UUID.randomUUID());
        query(ProcedimientoPaso.class,List.of(paso));
        Clinica clinica = new Clinica(UUID.randomUUID()); c.getIdPersonaRol().setIdClinica(clinica); medico.setIdClinica(clinica);
        PersonaRol enfermera = responsable(clinica, "Enfermería");
        servicio.agregarProcedimiento(id,procedimiento,"notas", enfermera.getIdPersonaRol());
        ArgumentCaptor<Object> captor=ArgumentCaptor.forClass(Object.class); verify(em,times(2)).persist(captor.capture());
        ConsultaProcedimiento cp=(ConsultaProcedimiento)captor.getAllValues().get(0);
        ConsultaProcedimientoPaso ejecucion=(ConsultaProcedimientoPaso)captor.getAllValues().get(1);
        assertEquals(c.getFechaInicio(),cp.getFechaInicio()); assertNull(cp.getFechaFin());
        assertEquals(enfermera,ejecucion.getIdPersonaRol()); assertEquals(paso,ejecucion.getIdProcedimientoPaso());
        assertEquals(c.getFechaInicio(),ejecucion.getFechaInicio()); assertEquals("PENDIENTE",ejecucion.getEstado());
    }
    @Test void rechazaRolQueNoEsPaciente() {
        PersonaRol pr=new PersonaRol(id); Rol rol=new Rol(); rol.setNombre("Médico"); pr.setIdRol(rol);
        when(em.find(PersonaRol.class,id,LockModeType.PESSIMISTIC_WRITE)).thenReturn(pr);
        assertThrows(ServiceException.class,()->servicio.abrir(id, UUID.randomUUID())); verify(em,never()).persist(any());
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
    @Test void resultadoSoloAceptaOrdenDeLaConsultaDelMedico() {
        Consulta c = abierta(); UUID ordenId = UUID.randomUUID();
        ConsultaProcedimiento procedimiento = new ConsultaProcedimiento(); procedimiento.setIdConsulta(c);
        ConsultaProcedimientoPaso paso = new ConsultaProcedimientoPaso(); paso.setIdConsultaProcedimiento(procedimiento);
        OrdenExamen orden = new OrdenExamen(ordenId); orden.setIdConsultaProcedimientoPaso(paso);
        when(em.find(OrdenExamen.class, ordenId)).thenReturn(orden);
        servicio.registrarResultado(id, ordenId, "  Normal  ", "Sin hallazgos");
        ArgumentCaptor<ExamenResultado> guardado = ArgumentCaptor.forClass(ExamenResultado.class);
        verify(em).persist(guardado.capture());
        assertSame(orden, guardado.getValue().getIdOrdenExamen());
        assertEquals("Normal", guardado.getValue().getResultado());
        clearInvocations(em);
        procedimiento.setIdConsulta(new Consulta(UUID.randomUUID()));
        assertThrows(ServiceException.class, () -> servicio.registrarResultado(id, ordenId, "Ajeno", null));
        verify(em, never()).persist(any());
    }
    @Test void aceptaResultadoDespuesDeCerrarLaConsulta() {
        Consulta c = abierta(); c.setFechaFin(OffsetDateTime.now());
        UUID ordenId = UUID.randomUUID();
        ConsultaProcedimiento procedimiento = new ConsultaProcedimiento(); procedimiento.setIdConsulta(c);
        ConsultaProcedimientoPaso paso = new ConsultaProcedimientoPaso(); paso.setIdConsultaProcedimiento(procedimiento);
        OrdenExamen orden = new OrdenExamen(ordenId); orden.setIdConsultaProcedimientoPaso(paso);
        when(em.find(OrdenExamen.class, ordenId)).thenReturn(orden);
        servicio.registrarResultado(id, ordenId, "Normal", null);
        verify(em).persist(any(ExamenResultado.class));
    }
    private PersonaRol responsable(Clinica clinica, String nombre) {
        PersonaRol pr = new PersonaRol(UUID.randomUUID());
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre(nombre); rol.setActivo(true);
        pr.setIdRol(rol); pr.setIdClinica(clinica);
        when(em.find(PersonaRol.class, pr.getIdPersonaRol())).thenReturn(pr);
        return pr;
    }
    @Test void aperturaRechazaResponsableDeOtraClinica() {
        PersonaRol paciente = new PersonaRol(UUID.randomUUID());
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre("Paciente"); rol.setActivo(true);
        paciente.setIdRol(rol); paciente.setIdClinica(new Clinica(UUID.randomUUID()));
        when(em.find(PersonaRol.class, paciente.getIdPersonaRol(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(paciente);
        PersonaRol doctor = responsable(new Clinica(UUID.randomUUID()), "Médico");
        assertThrows(ServiceException.class, () -> servicio.abrir(paciente.getIdPersonaRol(), doctor.getIdPersonaRol()));
        verify(em, never()).persist(any());
    }
    @Test void asignaPasoAUnRolDistintoDelPaciente() {
        Consulta c = abierta(); Clinica clinica = new Clinica(UUID.randomUUID()); c.getIdPersonaRol().setIdClinica(clinica); medico.setIdClinica(clinica);
        PersonaRol laboratorio = responsable(clinica, "Laboratorio");
        ConsultaProcedimiento cp = new ConsultaProcedimiento(UUID.randomUUID()); cp.setIdConsulta(c);
        ConsultaProcedimientoPaso paso = new ConsultaProcedimientoPaso(UUID.randomUUID()); paso.setIdConsultaProcedimiento(cp);
        when(em.find(ConsultaProcedimientoPaso.class, paso.getIdConsultaProcedimientoPaso())).thenReturn(paso);
        servicio.asignarResponsable(id, paso.getIdConsultaProcedimientoPaso(), laboratorio.getIdPersonaRol());
        assertSame(laboratorio, paso.getIdPersonaRol());
    }
    @Test void noReasignaPasosCompletados() {
        Consulta c = abierta();
        ConsultaProcedimiento cp = new ConsultaProcedimiento(UUID.randomUUID()); cp.setIdConsulta(c);
        ConsultaProcedimientoPaso paso = new ConsultaProcedimientoPaso(UUID.randomUUID()); paso.setIdConsultaProcedimiento(cp);
        paso.setFechaFin(OffsetDateTime.now());
        when(em.find(ConsultaProcedimientoPaso.class, paso.getIdConsultaProcedimientoPaso())).thenReturn(paso);
        assertThrows(ServiceException.class, () -> servicio.asignarResponsable(id, paso.getIdConsultaProcedimientoPaso(), UUID.randomUUID()));
        assertNull(paso.getIdPersonaRol());
    }
    @Test void noReasignaPasosAResponsableDeOtraClinica() {
        Consulta c = abierta(); c.getIdPersonaRol().setIdClinica(new Clinica(UUID.randomUUID()));
        ConsultaProcedimiento cp = new ConsultaProcedimiento(UUID.randomUUID()); cp.setIdConsulta(c);
        ConsultaProcedimientoPaso paso = new ConsultaProcedimientoPaso(UUID.randomUUID()); paso.setIdConsultaProcedimiento(cp);
        when(em.find(ConsultaProcedimientoPaso.class, paso.getIdConsultaProcedimientoPaso())).thenReturn(paso);
        PersonaRol doctor = responsable(new Clinica(UUID.randomUUID()), "Médico");
        assertThrows(ServiceException.class, () -> servicio.asignarResponsable(id, paso.getIdConsultaProcedimientoPaso(), doctor.getIdPersonaRol()));
        assertNull(paso.getIdPersonaRol());
    }

}
