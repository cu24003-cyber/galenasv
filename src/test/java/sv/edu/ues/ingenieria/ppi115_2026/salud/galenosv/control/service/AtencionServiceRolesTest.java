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

/**
 * B8: un paso solo lo atiende, completa, retrocede o le ordena exámenes quien tiene en la clínica
 * el rol responsable del paso; la consulta la cierra quien completó el paso de finalización.
 * Escenario del guion: Triaje (Enfermera) → Consulta médica (Doctor) → Entrega (Atención al público).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtencionServiceRolesTest {
    @Mock EntityManager em;
    @Mock AtencionSesion sesion;
    @InjectMocks AtencionService servicio;

    final UUID id = UUID.randomUUID();
    final Clinica clinica = new Clinica(UUID.randomUUID());
    final Rol rolEnfermera = rol("ENFERMERA", "Enfermera"), rolDoctor = rol("DOCTOR", "Doctor"),
            rolAtencion = rol("ADMINISTRACION", "Atención al público"), rolPaciente = rol("PACIENTE", "Paciente");
    final PersonaRol lucia = empleado(rolEnfermera), carlos = empleado(rolDoctor), marta = empleado(rolAtencion);
    final ProcedimientoPaso triaje = definicion("Triaje", rolEnfermera, false),
            medica = definicion("Consulta médica", rolDoctor, false),
            entrega = definicion("Entrega de indicaciones", rolAtencion, true);
    Consulta consulta;
    ConsultaProcedimiento cp;
    List<ConsultaProcedimientoPaso> pasos = new ArrayList<>();

    static Rol rol(String codigo, String nombre) {
        Rol r = new Rol(UUID.randomUUID()); r.setCodigo(codigo); r.setNombre(nombre); r.setActivo(true); return r;
    }
    PersonaRol empleado(Rol rol) {
        PersonaRol p = new PersonaRol(UUID.randomUUID()); p.setIdRol(rol); p.setIdClinica(clinica);
        p.setIdPersona(new Persona(UUID.randomUUID())); return p;
    }
    static ProcedimientoPaso definicion(String nombre, Rol rol, boolean fin) {
        ProcedimientoPaso p = new ProcedimientoPaso(UUID.randomUUID()); p.setNombre(nombre); p.setIdRol(rol); p.setIndicaFin(fin); return p;
    }
    ConsultaProcedimientoPaso paso(ProcedimientoPaso def, PersonaRol responsable, boolean completado) {
        ConsultaProcedimientoPaso p = new ConsultaProcedimientoPaso(UUID.randomUUID());
        p.setIdConsultaProcedimiento(cp); p.setIdProcedimientoPaso(def); p.setIdPersonaRol(responsable);
        p.setFechaInicio(OffsetDateTime.parse("2026-11-13T08:00:00-06:00"));
        p.setEstado(completado ? "COMPLETADO" : "PENDIENTE");
        if (completado) { p.setFechaFin(OffsetDateTime.parse("2026-11-13T08:10:00-06:00").plusMinutes(pasos.size())); p.setValor("Registrado"); }
        when(em.find(ConsultaProcedimientoPaso.class, p.getIdConsultaProcedimientoPaso())).thenReturn(p);
        pasos.add(p);
        return p;
    }
    void activo(PersonaRol rol) {
        when(sesion.getRolActivo()).thenReturn(rol);
        when(em.find(PersonaRol.class, rol.getIdPersonaRol())).thenReturn(rol);
    }

    @SuppressWarnings("unchecked")
    @BeforeEach void preparar() {
        PersonaRol paciente = empleado(rolPaciente);
        consulta = new Consulta(id); consulta.setIdPersonaRol(paciente);
        consulta.setFechaInicio(OffsetDateTime.parse("2026-11-13T08:00:00-06:00"));
        Procedimiento proc = new Procedimiento(UUID.randomUUID()); proc.setNombre("Consulta general");
        cp = new ConsultaProcedimiento(UUID.randomUUID()); cp.setIdConsulta(consulta); cp.setIdProcedimiento(proc);
        when(em.find(Consulta.class, id, LockModeType.PESSIMISTIC_WRITE)).thenReturn(consulta);
        TypedQuery<Consulta> qc = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Consulta.class))).thenReturn(qc);
        when(qc.setParameter(anyString(), any())).thenReturn(qc);
        when(qc.getSingleResult()).thenReturn(consulta);
        TypedQuery<ConsultaProcedimientoPaso> qp = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(ConsultaProcedimientoPaso.class))).thenReturn(qp);
        when(qp.setParameter(anyString(), any())).thenReturn(qp);
        when(qp.getResultList()).thenAnswer(x -> pasos);
        TypedQuery<ConsultaProcedimiento> qcp = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(ConsultaProcedimiento.class))).thenReturn(qcp);
        when(qcp.setParameter(anyString(), any())).thenReturn(qcp);
        when(qcp.getResultList()).thenAnswer(x -> List.of(cp));
        TypedQuery<ProcedimientoPasoSecuencia> qs = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(ProcedimientoPasoSecuencia.class))).thenReturn(qs);
        when(qs.setParameter(anyString(), any())).thenReturn(qs);
        when(qs.getResultList()).thenReturn(List.of());
        TypedQuery<Long> ql = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(ql);
        when(ql.setParameter(anyString(), any())).thenReturn(ql);
        when(ql.getSingleResult()).thenReturn(1L);
    }

    private String rechazo(org.junit.jupiter.api.function.Executable accion) {
        return assertThrows(ServiceException.class, accion).getMessageKey();
    }

    // ---- Completar

    @Test void laEnfermeraCompletaElTriajeYQuedaComoQuienLoRealizo() {
        ConsultaProcedimientoPaso t = paso(triaje, empleado(rolEnfermera), false);
        activo(lucia);
        servicio.completar(id, t.getIdConsultaProcedimientoPaso(), "PA 120/80");
        assertEquals("COMPLETADO", t.getEstado());
        assertSame(lucia, t.getIdPersonaRol(), "el paso queda a nombre de quien lo completó");
    }

    @Test void elDoctorNoPuedeCompletarElTriaje() {
        ConsultaProcedimientoPaso t = paso(triaje, lucia, false);
        activo(carlos);
        assertEquals("consulta.noResponsablePaso", rechazo(() -> servicio.completar(id, t.getIdConsultaProcedimientoPaso(), "x")));
        assertEquals("PENDIENTE", t.getEstado()); assertNull(t.getFechaFin()); assertSame(lucia, t.getIdPersonaRol());
    }

    @Test void elMensajeDeRechazoNombraElRolDelPaso() {
        ConsultaProcedimientoPaso t = paso(triaje, lucia, false);
        activo(carlos);
        ServiceException e = assertThrows(ServiceException.class, () -> servicio.completar(id, t.getIdConsultaProcedimientoPaso(), null));
        assertEquals("Este paso lo atiende el rol Enfermera.", e.getMessage());
        assertArrayEquals(new Object[]{"Enfermera"}, e.getMessageArguments());
    }

    @Test void otraPersonaConElMismoRolEnLaClinicaTambienPuedeCompletar() {
        PersonaRol otraEnfermera = empleado(rolEnfermera);
        ConsultaProcedimientoPaso t = paso(triaje, lucia, false);
        activo(otraEnfermera);
        servicio.completar(id, t.getIdConsultaProcedimientoPaso(), null);
        assertSame(otraEnfermera, t.getIdPersonaRol());
    }

    // ---- Retroceder

    @Test void soloElRolDelPasoActualPuedeRetroceder() {
        paso(triaje, lucia, true);
        ConsultaProcedimientoPaso m = paso(medica, carlos, false);
        activo(lucia);
        assertEquals("consulta.noResponsablePaso", rechazo(() -> servicio.retroceder(id, m.getIdConsultaProcedimientoPaso())));
        verify(em, never()).remove(any());
    }

    // ---- Órdenes y exámenes

    @Test void laEnfermeraNoOrdenaExamenesEnLaConsultaMedica() {
        ConsultaProcedimientoPaso m = paso(medica, carlos, false);
        activo(lucia);
        assertEquals("consulta.noResponsablePaso", rechazo(() -> servicio.ordenar(id, m.getIdConsultaProcedimientoPaso(), UUID.randomUUID(), null)));
        assertEquals("consulta.noResponsablePaso", rechazo(() -> servicio.ordenar(id, m.getIdConsultaProcedimientoPaso(), "Hemograma")));
        TipoExamen tipo = new TipoExamen(UUID.randomUUID()); tipo.setActivo(true);
        when(em.find(TipoExamen.class, tipo.getIdTipoExamen())).thenReturn(tipo);
        assertEquals("consulta.noResponsablePaso", rechazo(() -> servicio.examen(id, m.getIdConsultaProcedimientoPaso(), "Nuevo", null, tipo.getIdTipoExamen())));
        verify(em, never()).persist(any());
    }

    @Test void elDoctorOrdenaEnLaConsultaMedica() {
        ConsultaProcedimientoPaso m = paso(medica, carlos, false);
        activo(carlos);
        servicio.ordenar(id, m.getIdConsultaProcedimientoPaso(), "Hemograma");
        verify(em).persist(any(OrdenExamen.class));
    }

    @Test void soloElResponsableDelPasoReasignaElPaso() {
        ConsultaProcedimientoPaso t = paso(triaje, lucia, false);
        PersonaRol otraEnfermera = empleado(rolEnfermera);
        when(em.find(PersonaRol.class, otraEnfermera.getIdPersonaRol())).thenReturn(otraEnfermera);
        activo(carlos);
        assertEquals("consulta.noResponsablePaso", rechazo(() -> servicio.asignarResponsable(id, t.getIdConsultaProcedimientoPaso(), otraEnfermera.getIdPersonaRol())));
        assertSame(lucia, t.getIdPersonaRol());
    }

    // ---- Cierre

    @Test void cierraQuienCompletoElPasoDeFinalizacion() {
        paso(triaje, lucia, true); paso(medica, carlos, true); paso(entrega, marta, true);
        activo(marta);
        servicio.cerrar(id, null, null);
        assertNotNull(consulta.getFechaFin());
    }

    @Test void otroEmpleadoNoPuedeCerrarAunqueTodoEsteCompleto() {
        paso(triaje, lucia, true); paso(medica, carlos, true); paso(entrega, marta, true);
        activo(carlos);
        assertEquals("consulta.soloFinalizador", rechazo(() -> servicio.cerrar(id, null, null)));
        assertNull(consulta.getFechaFin());
    }

    @Test void otraPersonaConElRolDeFinalizacionTampocoCierraSiNoLoCompleto() {
        paso(triaje, lucia, true); paso(medica, carlos, true); paso(entrega, marta, true);
        activo(empleado(rolAtencion));
        assertEquals("consulta.soloFinalizador", rechazo(() -> servicio.cerrar(id, null, null)));
    }

    // ---- Seleccionar la consulta para atenderla (PUT consultas/{id}/sesion)

    @Test void seAtiendeSiHayUnPasoPendienteDeMiRol() {
        paso(triaje, lucia, true); paso(medica, carlos, false);
        activo(carlos);
        assertDoesNotThrow(() -> servicio.exigirAtendible(id));
        activo(lucia);
        assertEquals("consulta.sinPasoParaRol", rechazo(() -> servicio.exigirAtendible(id)));
    }

    @Test void conTodoCompletoSoloLaAtiendeQuienDebeCerrarla() {
        paso(triaje, lucia, true); paso(medica, carlos, true); paso(entrega, marta, true);
        activo(marta);
        assertDoesNotThrow(() -> servicio.exigirAtendible(id));
        activo(carlos);
        assertEquals("consulta.sinPasoParaRol", rechazo(() -> servicio.exigirAtendible(id)));
    }

    @Test void unaConsultaSinPasosLaPuedeAtenderCualquierEmpleadoParaAgregarElProcedimiento() {
        activo(lucia);
        assertDoesNotThrow(() -> servicio.exigirAtendible(id));
    }

    @Test void unaConsultaCerradaNoSeAtiende() {
        consulta.setFechaFin(OffsetDateTime.now());
        activo(marta);
        assertEquals("consulta.yaCerrada", rechazo(() -> servicio.exigirAtendible(id)));
    }

    // ---- Quién es empleado y quién es paciente

    @Test void enfermeraYAtencionAlPublicoConsultanElHistorialDeSuClinica() {
        @SuppressWarnings("unchecked") TypedQuery<Consulta> q = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Consulta.class))).thenReturn(q);
        when(q.setParameter(anyString(), any())).thenReturn(q);
        when(q.getResultList()).thenReturn(List.of());
        for (PersonaRol empleado : List.of(lucia, marta)) {
            activo(empleado);
            assertEquals(List.of(), servicio.historial());
        }
    }

    @Test void unRolPacienteOInactivoNoEsEmpleado() {
        activo(empleado(rolPaciente));
        assertEquals("consulta.soloEmpleado", rechazo(servicio::historial));
        Rol inactivo = rol("FARMACEUTICO", "Farmacéutico"); inactivo.setActivo(false);
        activo(empleado(inactivo));
        assertEquals("consulta.soloEmpleado", rechazo(servicio::historial));
    }

    @Test void sinRolActivoSePideElegirRolYClinica() {
        when(sesion.getRolActivo()).thenReturn(null);
        assertEquals("consulta.rolClinica", rechazo(servicio::historial));
    }

    @Test void elPacienteSeReconocePorCodigoYNoPorNombre() {
        activo(marta);
        Rol sinCodigo = rol(null, "Paciente");
        PersonaRol falso = empleado(sinCodigo);
        when(em.find(PersonaRol.class, falso.getIdPersonaRol(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(falso);
        assertEquals("consulta.rolPaciente", rechazo(() -> servicio.crear(falso.getIdPersonaRol(), null, null)));
        Rol renombrado = rol("PACIENTE", "Paciente externo");
        PersonaRol real = empleado(renombrado);
        when(em.find(PersonaRol.class, real.getIdPersonaRol(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(real);
        @SuppressWarnings("unchecked") TypedQuery<Consulta> q = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Consulta.class))).thenReturn(q);
        when(q.setParameter(anyString(), any())).thenReturn(q);
        when(q.getResultList()).thenReturn(List.of());
        assertSame(real, servicio.crear(real.getIdPersonaRol(), null, null).getIdPersonaRol());
    }
}
