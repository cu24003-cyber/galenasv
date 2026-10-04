package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtencionServiceTest {
    @Mock EntityManager em;
    @Mock sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionSesion sesion;
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
        c.setIdPersonaRol(paciente);
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
        when(q.setMaxResults(anyInt())).thenReturn(q);
        when(q.getResultList()).thenReturn(resultado); return q;
    }
    @Test void retomaConsultaExistenteSinCambiarInicio() {
        PersonaRol pr=new PersonaRol(UUID.randomUUID()); Rol rol=new Rol(); rol.setNombre("Paciente"); rol.setActivo(true); pr.setIdRol(rol);
        when(em.find(PersonaRol.class,pr.getIdPersonaRol(),LockModeType.PESSIMISTIC_WRITE)).thenReturn(pr);
        Consulta c=new Consulta(id); c.setFechaInicio(OffsetDateTime.parse("2026-09-29T10:15:30-06:00")); c.setIdPersonaRol(pr);
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
    @Test void permiteLeerYModificarConsultaDeLaMismaClinica() {
        Consulta c = abierta();
        assertSame(c, servicio.cargar(id));
        servicio.guardar(id, "REF", "notas");
        assertEquals("REF", c.getReferenciaExterna());
    }
    @Test void impideLeerConsultaDeOtraClinica() {
        Consulta c = abierta(); c.getIdPersonaRol().setIdClinica(new Clinica(UUID.randomUUID()));
        assertThrows(ServiceException.class, () -> servicio.cargar(id));
    }
    @Test void permiteRetomarConsultaAbiertaDeLaMismaClinica() {
        PersonaRol paciente = new PersonaRol(UUID.randomUUID());
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre("Paciente"); rol.setActivo(true);
        paciente.setIdRol(rol); paciente.setIdClinica(medico.getIdClinica());
        when(em.find(PersonaRol.class, paciente.getIdPersonaRol(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(paciente);
        Consulta ajena = new Consulta(id); ajena.setIdPersonaRol(paciente);
        TypedQuery<Consulta> consultas = query(Consulta.class, List.of(ajena));
        when(consultas.getSingleResult()).thenReturn(ajena);
        assertSame(ajena, servicio.abrir(paciente.getIdPersonaRol(), medico.getIdPersonaRol()));
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
        ProcedimientoPaso definicion = new ProcedimientoPaso(UUID.randomUUID()); definicion.setIndicaFin(true); paso.setIdProcedimientoPaso(definicion);
        ConsultaProcedimiento cp=new ConsultaProcedimiento(UUID.randomUUID()); cp.setFechaInicio(inicio); paso.setIdConsultaProcedimiento(cp);
        query(ConsultaProcedimientoPaso.class,List.of(paso)); query(ConsultaProcedimiento.class,List.of(cp));
        servicio.cerrar(id,"referencia","observación");
        assertEquals(c.getFechaFin(),cp.getFechaFin()); assertNotNull(c.getFechaFin().getOffset());
        assertEquals(inicio,c.getFechaInicio()); assertEquals("observación",c.getObservaciones());
    }
    @Test void procedimientoCopiaInicioYAsignaResponsableDelRolDelPaso() {
        Consulta c=abierta(); UUID procedimiento=UUID.randomUUID();
        Procedimiento p=new Procedimiento(procedimiento); p.setActivo(true);
        when(em.find(Procedimiento.class,procedimiento,LockModeType.PESSIMISTIC_WRITE)).thenReturn(p);
        ProcedimientoPaso paso=new ProcedimientoPaso(UUID.randomUUID());
        query(ProcedimientoPaso.class,List.of(paso));
        Clinica clinica = new Clinica(UUID.randomUUID()); c.getIdPersonaRol().setIdClinica(clinica); medico.setIdClinica(clinica);
        PersonaRol enfermera = responsable(clinica, "Enfermería");
        paso.setIdRol(enfermera.getIdRol());
        query(ProcedimientoPasoSecuencia.class,List.of());
        query(PersonaRol.class,List.of(enfermera));
        servicio.agregarProcedimiento(id,procedimiento,"notas", enfermera.getIdPersonaRol());
        ArgumentCaptor<Object> captor=ArgumentCaptor.forClass(Object.class); verify(em,times(2)).persist(captor.capture());
        ConsultaProcedimiento cp=(ConsultaProcedimiento)captor.getAllValues().get(0);
        ConsultaProcedimientoPaso ejecucion=(ConsultaProcedimientoPaso)captor.getAllValues().get(1);
        assertEquals(c.getFechaInicio(),cp.getFechaInicio()); assertNull(cp.getFechaFin());
        assertEquals(enfermera,ejecucion.getIdPersonaRol()); assertEquals(paso,ejecucion.getIdProcedimientoPaso());
        assertEquals(c.getFechaInicio(),ejecucion.getFechaInicio()); assertEquals("PENDIENTE",ejecucion.getEstado());
    }
    @Test void procedimientoSoloCreaPasoInicialYRechazaAusenciaDeResponsable() {
        Consulta c=abierta(); Procedimiento p=new Procedimiento(UUID.randomUUID()); p.setActivo(true);
        when(em.find(Procedimiento.class,p.getIdProcedimiento(),LockModeType.PESSIMISTIC_WRITE)).thenReturn(p);
        ProcedimientoPaso inicial=new ProcedimientoPaso(UUID.randomUUID()), siguiente=new ProcedimientoPaso(UUID.randomUUID());
        Rol rol=new Rol(UUID.randomUUID()); rol.setNombre("Enfermera"); rol.setActivo(true);
        inicial.setIdRol(rol); siguiente.setIdRol(rol);
        ProcedimientoPasoSecuencia secuencia=new ProcedimientoPasoSecuencia(UUID.randomUUID());
        secuencia.setIdProcedimientoPaso(inicial); secuencia.setIdProcedimientoPasoReferencia(siguiente);
        query(ProcedimientoPaso.class,List.of(inicial,siguiente)); query(ProcedimientoPasoSecuencia.class,List.of(secuencia));
        PersonaRol encargada=responsable(c.getIdPersonaRol().getIdClinica(),"Enfermera");
        query(PersonaRol.class,List.of(encargada));
        servicio.agregarProcedimiento(id,p.getIdProcedimiento(),null,medico.getIdPersonaRol());
        ArgumentCaptor<Object> captor=ArgumentCaptor.forClass(Object.class); verify(em,times(2)).persist(captor.capture());
        assertSame(inicial,((ConsultaProcedimientoPaso)captor.getAllValues().get(1)).getIdProcedimientoPaso());
        reset(em);
        when(em.find(Consulta.class,id,LockModeType.PESSIMISTIC_WRITE)).thenReturn(c);
        when(em.find(PersonaRol.class,medico.getIdPersonaRol())).thenReturn(medico);
        when(em.find(Procedimiento.class,p.getIdProcedimiento(),LockModeType.PESSIMISTIC_WRITE)).thenReturn(p);
        query(ProcedimientoPaso.class,List.of(inicial,siguiente)); query(ProcedimientoPasoSecuencia.class,List.of(secuencia));
        query(PersonaRol.class,List.of());
        assertThrows(ServiceException.class,()->servicio.agregarProcedimiento(id,p.getIdProcedimiento(),null,null));
        verify(em,never()).persist(any());
    }
    @Test void historialConvierteDiasLocalesAIntervaloUtcInclusivo() {
        TypedQuery<Consulta> consultas=query(Consulta.class,List.of());
        servicio.historial(java.time.LocalDate.of(2026,10,1),java.time.LocalDate.of(2026,10,2));
        verify(consultas).setParameter("desde",OffsetDateTime.parse("2026-10-01T06:00:00Z"));
        verify(consultas).setParameter("hasta",OffsetDateTime.parse("2026-10-03T06:00:00Z"));
        assertThrows(ServiceException.class,()->servicio.historial(java.time.LocalDate.of(2026,10,3),java.time.LocalDate.of(2026,10,2)));
    }
    @Test void completarPasoCreaSuSucesorConElRolCorrespondiente() {
        Consulta c=abierta(); ConsultaProcedimiento cp=new ConsultaProcedimiento(UUID.randomUUID()); cp.setIdConsulta(c);
        ProcedimientoPaso inicial=new ProcedimientoPaso(UUID.randomUUID()), destino=new ProcedimientoPaso(UUID.randomUUID());
        Rol enfermeria=new Rol(UUID.randomUUID()); enfermeria.setNombre("Enfermería"); enfermeria.setActivo(true); destino.setIdRol(enfermeria);
        ConsultaProcedimientoPaso ejecucion=new ConsultaProcedimientoPaso(UUID.randomUUID());
        ejecucion.setIdConsultaProcedimiento(cp); ejecucion.setIdProcedimientoPaso(inicial);
        when(em.find(ConsultaProcedimientoPaso.class,ejecucion.getIdConsultaProcedimientoPaso())).thenReturn(ejecucion);
        ProcedimientoPasoSecuencia s=new ProcedimientoPasoSecuencia(UUID.randomUUID());
        s.setIdProcedimientoPaso(inicial); s.setIdProcedimientoPasoReferencia(destino);
        when(em.createQuery(anyString(),eq(ProcedimientoPasoSecuencia.class))).thenAnswer(inv->{
            @SuppressWarnings("unchecked") TypedQuery<ProcedimientoPasoSecuencia> q=mock(TypedQuery.class);
            when(q.setParameter(anyString(),any())).thenReturn(q);
            when(q.getResultList()).thenReturn(((String)inv.getArgument(0)).contains("Referencia=:p") ? List.of() : List.of(s));
            return q;
        });
        query(ConsultaProcedimientoPaso.class,List.of());
        TypedQuery<Long> cuenta=query(Long.class,List.of()); when(cuenta.getSingleResult()).thenReturn(1L);
        PersonaRol responsable=responsable(c.getIdPersonaRol().getIdClinica(),"Enfermería");
        query(PersonaRol.class,List.of(responsable));
        servicio.completar(id,ejecucion.getIdConsultaProcedimientoPaso()," 125/80 ");
        ArgumentCaptor<ConsultaProcedimientoPaso> captor=ArgumentCaptor.forClass(ConsultaProcedimientoPaso.class);
        verify(em).persist(captor.capture());
        assertSame(destino,captor.getValue().getIdProcedimientoPaso());
        assertSame(responsable,captor.getValue().getIdPersonaRol());
        assertEquals(ejecucion.getFechaFin(),captor.getValue().getFechaInicio());
        assertEquals("125/80", ejecucion.getValor());
    }
    @Test void cierreRechazaProcedimientoSinPasoFinalAunqueTodosEstanCompletos() {
        Consulta c=abierta(); ConsultaProcedimiento cp=new ConsultaProcedimiento(UUID.randomUUID());
        Procedimiento catalogo=new Procedimiento(UUID.randomUUID()); catalogo.setNombre("Sin final"); cp.setIdProcedimiento(catalogo);
        cp.setIdConsulta(c);
        ConsultaProcedimientoPaso paso=new ConsultaProcedimientoPaso(UUID.randomUUID()); paso.setIdConsultaProcedimiento(cp);
        ProcedimientoPaso definicion=new ProcedimientoPaso(UUID.randomUUID()); definicion.setIndicaFin(false);
        paso.setIdProcedimientoPaso(definicion); paso.setFechaFin(OffsetDateTime.now());
        query(ConsultaProcedimientoPaso.class,List.of(paso)); query(ConsultaProcedimiento.class,List.of(cp));
        assertThrows(ServiceException.class,()->servicio.cerrar(id,null,null));
        assertNull(c.getFechaFin());
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
        pr.setIdPersona(new Persona(UUID.randomUUID()));
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre(nombre); rol.setActivo(true);
        pr.setIdRol(rol); pr.setIdClinica(clinica);
        when(em.find(PersonaRol.class, pr.getIdPersonaRol())).thenReturn(pr);
        return pr;
    }

    private Procedimiento catalogo(List<ProcedimientoPaso> pasos) {
        Procedimiento procedimiento = new Procedimiento(UUID.randomUUID()); procedimiento.setActivo(true);
        when(em.find(Procedimiento.class, procedimiento.getIdProcedimiento(),LockModeType.PESSIMISTIC_WRITE)).thenReturn(procedimiento);
        query(ProcedimientoPaso.class, pasos);
        query(ProcedimientoPasoSecuencia.class, List.of());
        return procedimiento;
    }

    private ProcedimientoPaso definicion(PersonaRol persona, String nombre) {
        ProcedimientoPaso paso = new ProcedimientoPaso(UUID.randomUUID());
        paso.setNombre(nombre); paso.setIdRol(persona.getIdRol());
        return paso;
    }

    private void candidatos(Map<Rol, List<PersonaRol>> personas) {
        TypedQuery<PersonaRol> consulta = query(PersonaRol.class, List.of());
        java.util.concurrent.atomic.AtomicReference<Rol> rol = new java.util.concurrent.atomic.AtomicReference<>();
        when(consulta.setParameter(eq("rol"), any())).thenAnswer(i -> { rol.set(i.getArgument(1)); return consulta; });
        when(consulta.getResultList()).thenAnswer(i -> personas.getOrDefault(rol.get(), List.of()));
    }

    @Test void noIniciaSiFaltaResponsableEnUnPasoPosterior() {
        Consulta consulta = abierta();
        PersonaRol enfermera = responsable(medico.getIdClinica(), "Enfermería");
        PersonaRol laboratorio = responsable(medico.getIdClinica(), "Laboratorio");
        ProcedimientoPaso inicial = definicion(enfermera, "Preparación"), finalPaso = definicion(laboratorio, "Resultado");
        Procedimiento procedimiento = catalogo(List.of(inicial, finalPaso));
        ProcedimientoPasoSecuencia secuencia = new ProcedimientoPasoSecuencia(UUID.randomUUID());
        secuencia.setIdProcedimientoPaso(inicial); secuencia.setIdProcedimientoPasoReferencia(finalPaso);
        query(ProcedimientoPasoSecuencia.class, List.of(secuencia));
        candidatos(Map.of(enfermera.getIdRol(), List.of(enfermera)));
        ServiceException error = assertThrows(ServiceException.class,
                () -> servicio.agregarProcedimiento(id, procedimiento.getIdProcedimiento(), null, null));
        assertEquals("consulta.sinResponsableAutomatico", error.getMessageKey());
        assertArrayEquals(new Object[]{"Laboratorio", "Resultado"}, error.getMessageArguments());
        verify(em, never()).persist(any());
        assertNull(consulta.getFechaFin());
    }

    @Test void cuatroRolesRequierenCuatroPersonasConLosRolesCorrespondientes() {
        abierta();
        List<ProcedimientoPaso> pasos = new ArrayList<>();
        Map<Rol, List<PersonaRol>> personas = new LinkedHashMap<>();
        for (String rol : List.of("Médico", "Enfermería", "Laboratorio", "Farmacia")) {
            PersonaRol persona = responsable(medico.getIdClinica(), rol);
            pasos.add(definicion(persona, rol)); personas.put(persona.getIdRol(), List.of(persona));
        }
        Procedimiento procedimiento = catalogo(pasos);
        candidatos(personas);
        servicio.agregarProcedimiento(id, procedimiento.getIdProcedimiento(), null, null);
        ArgumentCaptor<ConsultaProcedimientoPaso> guardados = ArgumentCaptor.forClass(ConsultaProcedimientoPaso.class);
        verify(em, times(4)).persist(guardados.capture());
        assertEquals(4, guardados.getAllValues().stream().map(p -> p.getIdPersonaRol().getIdPersona().getIdPersona()).distinct().count());
        assertTrue(guardados.getAllValues().stream().allMatch(p -> p.getIdProcedimientoPaso().getIdRol().equals(p.getIdPersonaRol().getIdRol())));
    }

    @Test void unaPersonaConDosRolesNoSustituyeDosPersonasRequeridas() {
        abierta();
        PersonaRol primera = responsable(medico.getIdClinica(), "Enfermería");
        PersonaRol segunda = responsable(medico.getIdClinica(), "Laboratorio");
        segunda.setIdPersona(primera.getIdPersona());
        Procedimiento procedimiento = catalogo(List.of(definicion(primera, "Inicio"), definicion(segunda, "Fin")));
        candidatos(Map.of(primera.getIdRol(), List.of(primera), segunda.getIdRol(), List.of(segunda)));
        ServiceException error = assertThrows(ServiceException.class,
                () -> servicio.agregarProcedimiento(id, procedimiento.getIdProcedimiento(), null, null));
        assertEquals("consulta.personalInsuficiente", error.getMessageKey());
        verify(em, never()).persist(any());
    }

    @Test void encuentraPersonalDistintoAunqueDebaReubicarUnaPersonaConVariosRoles() {
        abierta();
        PersonaRol ambosEnfermeria = responsable(medico.getIdClinica(), "Enfermería");
        PersonaRol soloEnfermeria = responsable(medico.getIdClinica(), "Enfermería"); soloEnfermeria.setIdRol(ambosEnfermeria.getIdRol());
        PersonaRol ambosLaboratorio = responsable(medico.getIdClinica(), "Laboratorio"); ambosLaboratorio.setIdPersona(ambosEnfermeria.getIdPersona());
        Procedimiento procedimiento = catalogo(List.of(definicion(ambosEnfermeria, "Inicio"), definicion(ambosLaboratorio, "Fin")));
        candidatos(Map.of(ambosEnfermeria.getIdRol(), List.of(ambosEnfermeria, soloEnfermeria), ambosLaboratorio.getIdRol(), List.of(ambosLaboratorio)));
        servicio.agregarProcedimiento(id, procedimiento.getIdProcedimiento(), null, null);
        ArgumentCaptor<ConsultaProcedimientoPaso> guardados = ArgumentCaptor.forClass(ConsultaProcedimientoPaso.class);
        verify(em, times(2)).persist(guardados.capture());
        assertSame(soloEnfermeria, guardados.getAllValues().get(0).getIdPersonaRol());
        assertSame(ambosLaboratorio, guardados.getAllValues().get(1).getIdPersonaRol());
    }

    @Test void rechazaRolInactivoEnPasoPosteriorAntesDePersistir() {
        abierta();
        PersonaRol enfermera = responsable(medico.getIdClinica(), "Enfermería");
        PersonaRol laboratorio = responsable(medico.getIdClinica(), "Laboratorio"); laboratorio.getIdRol().setActivo(false);
        Procedimiento procedimiento = catalogo(List.of(definicion(enfermera, "Inicio"), definicion(laboratorio, "Fin")));
        candidatos(Map.of(enfermera.getIdRol(), List.of(enfermera)));
        ServiceException error = assertThrows(ServiceException.class,
                () -> servicio.agregarProcedimiento(id, procedimiento.getIdProcedimiento(), null, null));
        assertEquals("consulta.pasoRolInactivo", error.getMessageKey());
        verify(em, never()).persist(any());
    }
    private PersonaRol paciente(Clinica clinica) {
        PersonaRol paciente = new PersonaRol(UUID.randomUUID());
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre("Paciente"); rol.setActivo(true);
        paciente.setIdRol(rol); paciente.setIdClinica(clinica);
        when(em.find(PersonaRol.class, paciente.getIdPersonaRol(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(paciente);
        return paciente;
    }
    @Test void creacionGuardaPacienteAutorFechasYDatosEnLaMismaOperacion() {
        PersonaRol paciente = paciente(medico.getIdClinica());
        TypedQuery<Consulta> consultas = query(Consulta.class, List.of());
        doAnswer(i -> { when(consultas.getSingleResult()).thenReturn(i.getArgument(0)); return null; })
                .when(em).persist(any(Consulta.class));
        Consulta creada = servicio.crear(paciente.getIdPersonaRol(), "  REF-01  ", "  Primera atención  ");
        assertSame(paciente, creada.getIdPersonaRol());
        assertNotNull(creada.getIdConsulta()); assertNotNull(creada.getFechaInicio());
        assertEquals(java.time.ZoneOffset.UTC, creada.getFechaInicio().getOffset()); assertNull(creada.getFechaFin());
        assertEquals("REF-01", creada.getReferenciaExterna()); assertEquals("Primera atención", creada.getObservaciones());
        verify(em).persist(creada); verify(em).flush();
    }
    @Test void creacionRechazaPacienteDeOtraClinica() {
        PersonaRol paciente = paciente(new Clinica(UUID.randomUUID()));
        assertThrows(ServiceException.class, () -> servicio.crear(paciente.getIdPersonaRol(), null, null));
        verify(em, never()).persist(any());
    }
    @Test void creacionNoDuplicaNiSobrescribeConsultaAbierta() {
        PersonaRol paciente = paciente(medico.getIdClinica());
        Consulta existente = new Consulta(id); existente.setIdPersonaRol(paciente);
        existente.setObservaciones("Original"); query(Consulta.class, List.of(existente));
        assertThrows(ServiceException.class, () -> servicio.crear(paciente.getIdPersonaRol(), null, "Cambio"));
        assertEquals("Original", existente.getObservaciones()); verify(em, never()).persist(any());
    }
    @Test void creacionNoPermiteAbrirOtraConsultaDuranteLaAtencion() {
        when(sesion.getConsulta()).thenReturn(id);
        assertThrows(ServiceException.class, () -> servicio.crear(UUID.randomUUID(), null, null));
        verify(em, never()).persist(any());
    }
    @Test void creacionValidaLongitudesAntesDePersistir() {
        assertThrows(ServiceException.class, () -> servicio.crear(UUID.randomUUID(), "x".repeat(256), null));
        assertThrows(ServiceException.class, () -> servicio.crear(UUID.randomUUID(), null, "x".repeat(256)));
        verify(em, never()).persist(any());
    }
    @Test void historialYPacientesSeConsultanConElContextoDeLaSesion() {
        TypedQuery<Consulta> consultas = query(Consulta.class, List.of());
        TypedQuery<PersonaRol> pacientes = query(PersonaRol.class, List.of());
        assertTrue(servicio.historial().isEmpty()); assertTrue(servicio.pacientesClinica().isEmpty());
        verify(consultas).setParameter("clinica", medico.getIdClinica());
        verify(pacientes).setParameter("clinica", medico.getIdClinica());
        verify(em).createQuery(contains("WHERE paciente.idClinica=:clinica"), eq(Consulta.class));
        verify(em).createQuery(contains("WHERE r.idClinica=:clinica"), eq(PersonaRol.class));
    }
    @Test void sinRolMedicoNoExponeHistorialNiPacientes() {
        when(sesion.getRolActivo()).thenReturn(null);
        assertThrows(ServiceException.class, servicio::historial);
        assertThrows(ServiceException.class, servicio::pacientesClinica);
        assertThrows(ServiceException.class, () -> servicio.crear(UUID.randomUUID(), null, null));
        verify(em, never()).createQuery(anyString(), any()); verify(em, never()).persist(any());
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
