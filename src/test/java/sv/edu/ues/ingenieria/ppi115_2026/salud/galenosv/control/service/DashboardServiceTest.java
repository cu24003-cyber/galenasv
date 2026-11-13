package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.DashboardRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock AccesoSesion acceso;
    @Mock AtencionSesion atencion;
    @Mock AutenticacionService autenticacion;
    @Mock DashboardRepository repositorio;
    @InjectMocks DashboardService servicio;

    static final ZoneId ZONA = ZoneId.of("America/El_Salvador");
    final UUID usuario = UUID.randomUUID(), clinica = UUID.randomUUID(), rolDoctor = UUID.randomUUID(), rolAdmin = UUID.randomUUID();
    final UUID asignacionId = UUID.randomUUID();
    PersonaRol activo;

    // 2026-11-14T03:00Z es todavía 2026-11-13 21:00 en El Salvador.
    static final Instant AHORA = Instant.parse("2026-11-14T03:00:00Z");

    @BeforeEach void sesionDoctor() {
        servicio.reloj = Clock.fixed(AHORA, ZONA);
        activo = asignacion(asignacionId, "DOCTOR", rolDoctor);
        lenient().when(acceso.vigente()).thenReturn(true);
        lenient().when(acceso.getIdUsuario()).thenReturn(usuario);
        lenient().when(atencion.getRolActivo()).thenReturn(new PersonaRol(asignacionId));
        lenient().when(autenticacion.contexto(usuario, asignacionId)).thenAnswer(i -> new AutenticacionService.Contexto(null, activo));
    }

    PersonaRol asignacion(UUID id, String codigo, UUID idRol) {
        Rol rol = new Rol(idRol); rol.setCodigo(codigo); rol.setNombre(codigo); rol.setActivo(true);
        PersonaRol a = new PersonaRol(id); a.setIdRol(rol); a.setIdClinica(new Clinica(clinica, "Clínica")); return a;
    }

    static OffsetDateTime utc(String s) { return OffsetDateTime.parse(s); }
    static Object[] consulta(UUID id, String inicio, UUID persona, Date nacimiento) {
        return new Object[]{id, utc(inicio), null, persona, "Ana", "Pérez", nacimiento};
    }
    /** Fila de pasos con las 18 columnas de DashboardRepository.pasos. */
    static Object[] paso(UUID consulta, UUID cp, UUID proc, UUID id, UUID def, String nombre, String estado,
            String inicio, String fin, UUID rolDef, boolean valor) {
        return new Object[]{consulta, cp, proc, "Procedimiento", utc("2026-11-13T14:00Z"), null, id, def, nombre, estado,
                inicio == null ? null : utc(inicio), fin == null ? null : utc(fin), "Luis", "Gómez", "Doctor", rolDef, rolDef, valor};
    }

    // ---- sesión y rol

    @Test void sinSesionDa401SinConsultar() {
        when(acceso.vigente()).thenReturn(false);
        var e = assertThrows(AccesoException.class, () -> servicio.consultas(null, null));
        assertEquals(401, e.getStatus()); assertEquals("auth.sesion", e.getMessageKey());
        verifyNoInteractions(repositorio, autenticacion);
    }

    @Test void sinRolActivoDa403SinConsultar() {
        when(atencion.getRolActivo()).thenReturn(null);
        var e = assertThrows(AccesoException.class, () -> servicio.consultas(null, null));
        assertEquals(403, e.getStatus()); assertEquals("auth.rolActivo", e.getMessageKey());
        verifyNoInteractions(repositorio, autenticacion);
    }

    @Test void asignacionAjenaOInactivaSePropagaSinConsultar() {
        when(autenticacion.contexto(usuario, asignacionId)).thenThrow(new AccesoException(403, "auth.asignacion", "x"));
        var e = assertThrows(AccesoException.class, () -> servicio.consultas(null, null));
        assertEquals("auth.asignacion", e.getMessageKey());
        verifyNoInteractions(repositorio);
    }

    @Test void administracionTambienVeElDashboardDeSuClinica() {
        activo = asignacion(asignacionId, "ADMINISTRACION", rolAdmin);
        when(repositorio.consultas(eq(clinica), any(), any())).thenReturn(List.of());
        assertTrue(servicio.consultas(null, null).isEmpty());
        verify(repositorio).consultas(eq(clinica), any(), any());
    }

    // ---- fechas

    @Test void sinFechasUsaHoyEnElSalvador() {
        when(repositorio.consultas(any(), any(), any())).thenReturn(List.of());
        servicio.consultas(null, null);
        verify(repositorio).consultas(clinica, LocalDate.of(2026, 11, 13).atStartOfDay(ZONA).toOffsetDateTime(),
                LocalDate.of(2026, 11, 14).atStartOfDay(ZONA).toOffsetDateTime());
    }

    @Test void conUnaSolaFechaUsaEseDiaEnAmbosExtremos() {
        when(repositorio.consultas(any(), any(), any())).thenReturn(List.of());
        servicio.consultas("2026-10-01", null);
        servicio.consultas(null, "2026-10-01");
        verify(repositorio, times(2)).consultas(clinica, LocalDate.of(2026, 10, 1).atStartOfDay(ZONA).toOffsetDateTime(),
                LocalDate.of(2026, 10, 2).atStartOfDay(ZONA).toOffsetDateTime());
    }

    @Test void fechaInvalidaDa400SinConsultar() {
        var e = assertThrows(AccesoException.class, () -> servicio.consultas("13/11/2026", null));
        assertEquals(400, e.getStatus()); assertEquals("dashboard.fecha", e.getMessageKey());
        verifyNoInteractions(repositorio);
    }

    @Test void rangoInvertidoDa400() {
        var e = assertThrows(AccesoException.class, () -> servicio.consultas("2026-11-10", "2026-11-09"));
        assertEquals(400, e.getStatus()); assertEquals("dashboard.rangoInvertido", e.getMessageKey());
        verifyNoInteractions(repositorio);
    }

    @Test void rangoDe31DiasSeAceptaY32SeRechaza() {
        when(repositorio.consultas(any(), any(), any())).thenReturn(List.of());
        servicio.consultas("2026-11-01", "2026-12-01");
        var e = assertThrows(AccesoException.class, () -> servicio.consultas("2026-11-01", "2026-12-02"));
        assertEquals(400, e.getStatus()); assertEquals("dashboard.rangoExcedido", e.getMessageKey());
        verify(repositorio, times(1)).consultas(any(), any(), any());
    }

    // ---- armado

    @Test void sinConsultasNoEjecutaLasDemasProyecciones() {
        when(repositorio.consultas(any(), any(), any())).thenReturn(List.of());
        assertTrue(servicio.consultas(null, null).isEmpty());
        verify(repositorio, never()).pasos(any()); verify(repositorio, never()).documentos(any()); verify(repositorio, never()).secuencias(any());
    }

    @Test void cadaProyeccionSeEjecutaUnaSolaVezConTodosLosIds() {
        UUID c1 = UUID.randomUUID(), c2 = UUID.randomUUID(), c3 = UUID.randomUUID();
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID();
        UUID proc1 = UUID.randomUUID(), proc2 = UUID.randomUUID();
        when(repositorio.consultas(any(), any(), any())).thenReturn(List.of(
                consulta(c1, "2026-11-13T14:00Z", p1, null), consulta(c2, "2026-11-13T15:00Z", p2, null), consulta(c3, "2026-11-13T16:00Z", p1, null)));
        when(repositorio.pasos(any())).thenReturn(List.of(
                paso(c1, UUID.randomUUID(), proc1, UUID.randomUUID(), UUID.randomUUID(), "A", "PENDIENTE", "2026-11-13T14:00Z", null, rolDoctor, false),
                paso(c2, UUID.randomUUID(), proc2, UUID.randomUUID(), UUID.randomUUID(), "B", "PENDIENTE", "2026-11-13T15:00Z", null, rolDoctor, false)));
        servicio.consultas(null, null);
        verify(repositorio, times(1)).pasos(List.of(c1, c2, c3));
        verify(repositorio, times(1)).documentos(List.of(p1, p2));
        verify(repositorio, times(1)).secuencias(argThat(l -> l.size() == 2 && l.containsAll(List.of(proc1, proc2))));
    }

    @Test void armaConsultaPacienteProcedimientosYPasos() {
        UUID c1 = UUID.randomUUID(), persona = UUID.randomUUID(), cp = UUID.randomUUID(), cpVacio = UUID.randomUUID();
        UUID proc = UUID.randomUUID(), paso1 = UUID.randomUUID(), def1 = UUID.randomUUID();
        when(repositorio.consultas(any(), any(), any())).thenReturn(List.<Object[]>of(consulta(c1, "2026-11-13T14:00Z", persona,
                Timestamp.from(Instant.parse("1990-05-02T06:00:00Z")))));
        Object[] vacio = new Object[18];
        vacio[0] = c1; vacio[1] = cpVacio; vacio[2] = proc; vacio[3] = "Sin pasos"; vacio[4] = utc("2026-11-13T15:00Z");
        when(repositorio.pasos(any())).thenReturn(List.of(
                paso(c1, cp, proc, paso1, def1, "Signos", "COMPLETADO", "2026-11-13T14:00Z", "2026-11-13T14:30Z", rolDoctor, true), vacio));
        when(repositorio.documentos(any())).thenReturn(List.<Object[]>of(new Object[]{persona, "DUI", "00000000-3"}));
        var lista = servicio.consultas(null, null);
        assertEquals(1, lista.size());
        var c = lista.getFirst();
        assertEquals(c1, c.idConsulta());
        assertEquals(ZoneOffset.ofHours(-6), c.fechaInicio().getOffset());
        assertEquals("Ana", c.paciente().nombres());
        assertEquals(LocalDate.of(1990, 5, 2), c.paciente().fechaNacimiento());
        assertEquals(36, c.paciente().edad());
        assertEquals(List.of(new DashboardService.DocumentoPaciente("DUI", "00000000-3")), c.paciente().documentos());
        assertEquals(2, c.procedimientos().size());
        var p = c.procedimientos().getFirst();
        assertEquals(cp, p.idConsultaProcedimiento());
        var s = p.pasos().getFirst();
        assertEquals(paso1, s.idConsultaProcedimientoPaso());
        assertEquals("Signos", s.nombre()); assertEquals(1, s.orden()); assertEquals(0, s.nivel());
        assertEquals("COMPLETADO", s.estado()); assertTrue(s.tieneValor());
        assertEquals(new DashboardService.Responsable("Luis Gómez", "Doctor"), s.responsable());
        assertEquals(ZoneOffset.ofHours(-6), s.fechaFin().getOffset());
        assertEquals("Sin pasos", c.procedimientos().get(1).nombre());
        assertTrue(c.procedimientos().get(1).pasos().isEmpty());
        assertFalse(c.pendienteParaMi());
    }

    @Test void elPasoNoTieneCampoValor() {
        assertTrue(Arrays.stream(DashboardService.PasoDashboard.class.getRecordComponents()).noneMatch(r -> r.getName().equals("valor")));
        assertTrue(Arrays.stream(DashboardService.ConsultaDashboard.class.getRecordComponents())
                .noneMatch(r -> Set.of("observaciones", "referenciaExterna").contains(r.getName())));
    }

    @Test void ordenaPasosPorSecuenciaConNivelYDejaAlFinalLosSinDefinicion() {
        UUID c1 = UUID.randomUUID(), cp = UUID.randomUUID(), proc = UUID.randomUUID();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), cc = UUID.randomUUID(), d = UUID.randomUUID();
        when(repositorio.consultas(any(), any(), any())).thenReturn(List.<Object[]>of(consulta(c1, "2026-11-13T14:00Z", UUID.randomUUID(), null)));
        when(repositorio.pasos(any())).thenReturn(List.of(
                paso(c1, cp, proc, UUID.randomUUID(), null, null, "COMPLETADO", "2026-11-13T13:00Z", "2026-11-13T13:10Z", null, false),
                paso(c1, cp, proc, UUID.randomUUID(), d, "D", "PENDIENTE", "2026-11-13T17:00Z", null, rolDoctor, false),
                paso(c1, cp, proc, UUID.randomUUID(), cc, "C", "COMPLETADO", "2026-11-13T15:00Z", "2026-11-13T16:00Z", rolDoctor, false),
                paso(c1, cp, proc, UUID.randomUUID(), b, "B", "COMPLETADO", "2026-11-13T15:30Z", "2026-11-13T16:30Z", rolDoctor, false),
                paso(c1, cp, proc, UUID.randomUUID(), a, "A", "COMPLETADO", "2026-11-13T14:00Z", "2026-11-13T15:00Z", rolDoctor, false)));
        when(repositorio.secuencias(any())).thenReturn(List.of(
                new Object[]{a, b}, new Object[]{a, cc}, new Object[]{b, d}, new Object[]{cc, d}));
        var pasos = servicio.consultas(null, null).getFirst().procedimientos().getFirst().pasos();
        assertEquals(Arrays.asList("A", "C", "B", "D", null), pasos.stream().map(DashboardService.PasoDashboard::nombre).toList());
        assertEquals(List.of(0, 1, 1, 2, 0), pasos.stream().map(DashboardService.PasoDashboard::nivel).toList());
        assertEquals(List.of(1, 2, 3, 4, 5), pasos.stream().map(DashboardService.PasoDashboard::orden).toList());
    }

    @Test void consultasEnOrdenAscendenteAunqueLleguenDesordenadas() {
        UUID tarde = UUID.randomUUID(), temprano = UUID.randomUUID();
        when(repositorio.consultas(any(), any(), any())).thenReturn(List.of(
                consulta(tarde, "2026-11-13T20:00Z", UUID.randomUUID(), null), consulta(temprano, "2026-11-13T14:00Z", UUID.randomUUID(), null)));
        assertEquals(List.of(temprano, tarde), servicio.consultas(null, null).stream().map(DashboardService.ConsultaDashboard::idConsulta).toList());
    }

    @Test void pendienteParaMiSoloConUnPasoPendienteDelRolActivo() {
        UUID mia = UUID.randomUUID(), otroRol = UUID.randomUUID(), completada = UUID.randomUUID();
        when(repositorio.consultas(any(), any(), any())).thenReturn(List.of(
                consulta(mia, "2026-11-13T14:00Z", UUID.randomUUID(), null),
                consulta(otroRol, "2026-11-13T15:00Z", UUID.randomUUID(), null),
                consulta(completada, "2026-11-13T16:00Z", UUID.randomUUID(), null)));
        when(repositorio.pasos(any())).thenReturn(List.of(
                paso(mia, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "X", "PENDIENTE", "2026-11-13T14:00Z", null, rolDoctor, false),
                paso(otroRol, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Y", "PENDIENTE", "2026-11-13T15:00Z", null, rolAdmin, false),
                paso(completada, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Z", "COMPLETADO", "2026-11-13T16:00Z", "2026-11-13T16:10Z", rolDoctor, true)));
        assertEquals(List.of(true, false, false), servicio.consultas(null, null).stream().map(DashboardService.ConsultaDashboard::pendienteParaMi).toList());
    }

    @Test void pendienteParaMiUsaElRolDelResponsableSiElPasoNoTieneDefinicion() {
        UUID c1 = UUID.randomUUID();
        when(repositorio.consultas(any(), any(), any())).thenReturn(List.<Object[]>of(consulta(c1, "2026-11-13T14:00Z", UUID.randomUUID(), null)));
        Object[] fila = paso(c1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null, "PENDIENTE", "2026-11-13T14:00Z", null, null, false);
        fila[16] = rolDoctor;
        when(repositorio.pasos(any())).thenReturn(List.<Object[]>of(fila));
        assertTrue(servicio.consultas(null, null).getFirst().pendienteParaMi());
    }

    @Test void edadEnAniosCumplidos() {
        LocalDate hoy = LocalDate.of(2026, 11, 13);
        assertEquals(36, DashboardService.edad(LocalDate.of(1990, 11, 13), hoy));
        assertEquals(35, DashboardService.edad(LocalDate.of(1990, 11, 14), hoy));
        assertEquals(25, DashboardService.edad(LocalDate.of(2000, 2, 29), LocalDate.of(2026, 2, 28)));
        assertEquals(26, DashboardService.edad(LocalDate.of(2000, 2, 29), LocalDate.of(2026, 3, 1)));
        assertNull(DashboardService.edad(null, hoy));
    }

    @Test void fechaDeNacimientoSeLeeEnHoraDeElSalvadorSinImportarLaZonaDeLaJvm() {
        TimeZone anterior = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            assertEquals(LocalDate.of(1990, 5, 2), DashboardService.dia(Timestamp.from(Instant.parse("1990-05-02T06:00:00Z"))));
            // Como la lista JSF de personas: medianoche UTC del 1 de enero es aún 31 de diciembre en El Salvador.
            assertEquals(LocalDate.of(1999, 12, 31), DashboardService.dia(Timestamp.from(Instant.parse("2000-01-01T00:00:00Z"))));
            assertEquals(LocalDate.of(1999, 12, 31), DashboardService.dia(OffsetDateTime.parse("2000-01-01T00:00:00Z")));
            assertNull(DashboardService.dia(null));
        } finally {
            TimeZone.setDefault(anterior);
        }
    }

    @Test void responsableNuloSiElPasoNoTieneAsignado() {
        UUID c1 = UUID.randomUUID();
        when(repositorio.consultas(any(), any(), any())).thenReturn(List.<Object[]>of(consulta(c1, "2026-11-13T14:00Z", UUID.randomUUID(), null)));
        Object[] fila = paso(c1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "X", "PENDIENTE", "2026-11-13T14:00Z", null, rolDoctor, false);
        fila[12] = null; fila[13] = null; fila[14] = null;
        when(repositorio.pasos(any())).thenReturn(List.<Object[]>of(fila));
        assertNull(servicio.consultas(null, null).getFirst().procedimientos().getFirst().pasos().getFirst().responsable());
    }

    // ---- valor de un paso

    @Test void valorDevuelveLosCincoCamposConLaClinicaValidada() {
        UUID paso = UUID.randomUUID();
        when(repositorio.valorPaso(paso, clinica)).thenReturn(List.<Object[]>of(new Object[]{paso, "Signos", "COMPLETADO", utc("2026-11-13T14:30Z"), "120/80"}));
        var v = servicio.valor(paso);
        assertEquals(new DashboardService.ValorPaso(paso, "Signos", "COMPLETADO",
                utc("2026-11-13T14:30Z").atZoneSameInstant(ZONA).toOffsetDateTime(), "120/80"), v);
        verify(repositorio).valorPaso(paso, clinica);
    }

    @Test void valorInexistenteODeOtraClinicaDa404() {
        UUID paso = UUID.randomUUID();
        when(repositorio.valorPaso(paso, clinica)).thenReturn(List.of());
        var e = assertThrows(AccesoException.class, () -> servicio.valor(paso));
        assertEquals(404, e.getStatus()); assertEquals("consulta.pasoNoExiste", e.getMessageKey());
    }

    @Test void valorSinSesionOSinRolNoConsulta() {
        when(acceso.vigente()).thenReturn(false);
        assertEquals(401, assertThrows(AccesoException.class, () -> servicio.valor(UUID.randomUUID())).getStatus());
        when(acceso.vigente()).thenReturn(true);
        when(atencion.getRolActivo()).thenReturn(null);
        assertEquals(403, assertThrows(AccesoException.class, () -> servicio.valor(UUID.randomUUID())).getStatus());
        verifyNoInteractions(repositorio);
    }

    @Test void valorConAdministracionUsaSuClinica() {
        activo = asignacion(asignacionId, "ADMINISTRACION", rolAdmin);
        UUID paso = UUID.randomUUID();
        when(repositorio.valorPaso(paso, clinica)).thenReturn(List.<Object[]>of(new Object[]{paso, "Signos", "PENDIENTE", null, null}));
        assertNull(servicio.valor(paso).valor());
    }
}
