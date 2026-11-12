package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.CitaPublicaRepository;

@ExtendWith(MockitoExtension.class)
class CitaPublicaServiceTest {
    @Mock CitaPublicaRepository repositorio;
    @InjectMocks CitaPublicaService servicio;
    private final UUID tipo = UUID.randomUUID(), clinica = UUID.randomUUID();
    private final UUID c1 = UUID.randomUUID(), c2 = UUID.randomUUID();
    private final OffsetDateTime f1 = OffsetDateTime.parse("2026-11-12T08:00-06:00"), f2 = OffsetDateTime.parse("2026-11-19T10:00-06:00");

    @Test void elRangoSeInterpretaEnElSalvadorConHastaIncluido() {
        servicio.citas(tipo, "00000000-3", clinica, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        verify(repositorio).citas(tipo, "00000000-3", clinica,
                OffsetDateTime.parse("2026-11-01T00:00-06:00"), OffsetDateTime.parse("2026-12-01T00:00-06:00"));
    }

    @Test void rangoInvertidoOMayorDe366DiasEs400() {
        AccesoException invertido = assertThrows(AccesoException.class,
                () -> servicio.citas(tipo, "x", clinica, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1)));
        assertEquals(400, invertido.getStatus()); assertEquals("citas.rango", invertido.getMessageKey());
        assertEquals(400, assertThrows(AccesoException.class,
                () -> servicio.citas(tipo, "x", clinica, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 3))).getStatus());
        assertDoesNotThrow(() -> servicio.citas(tipo, "x", clinica, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 2)));
        assertEquals(400, assertThrows(AccesoException.class,
                () -> servicio.citas(tipo, "x", clinica, null, LocalDate.of(2026, 1, 1))).getStatus());
    }

    @Test void sinCoincidenciasDevuelveListaVaciaSinConsultarProcedimientos() {
        when(repositorio.citas(any(), any(), any(), any(), any())).thenReturn(List.of());
        assertEquals(List.of(), servicio.citas(tipo, "no-existe", clinica, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)));
        verify(repositorio, never()).procedimientos(any());
    }

    @Test void agrupaProcedimientosSinRepetirYEnOrden() {
        when(repositorio.citas(any(), any(), any(), any(), any())).thenReturn(List.of(
                new Object[]{c1, f1, "Clínica Uno"}, new Object[]{c2, f2, "Clínica Uno"}));
        when(repositorio.procedimientos(List.of(c1, c2))).thenReturn(List.of(
                new Object[]{c1, "Consulta general"}, new Object[]{c1, "Curación"}, new Object[]{c1, "Consulta general"},
                new Object[]{c2, "Consulta general"}));
        var citas = servicio.citas(tipo, "x", clinica, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertEquals(2, citas.size());
        assertEquals(new CitaPublicaService.CitaPublica(c1, f1, "Clínica Uno", List.of("Consulta general", "Curación")), citas.get(0));
        assertEquals(List.of("Consulta general"), citas.get(1).procedimientos());
    }

    @Test void citaSinProcedimientosTieneListaVacia() {
        when(repositorio.cita(tipo, "x", c1)).thenReturn(List.<Object[]>of(new Object[]{c1, f1, "Clínica Uno"}));
        when(repositorio.procedimientos(List.of(c1))).thenReturn(List.of());
        assertEquals(List.of(), servicio.cita(tipo, "x", c1).orElseThrow().procedimientos());
    }

    @Test void citaAjenaOInexistenteEsVacio() {
        when(repositorio.cita(tipo, "x", c1)).thenReturn(List.of());
        assertTrue(servicio.cita(tipo, "x", c1).isEmpty());
    }

    @Test void clinicasSeConviertenAIdYNombre() {
        when(repositorio.clinicas(tipo, "x")).thenReturn(List.<Object[]>of(new Object[]{clinica, "Clínica Uno"}));
        assertEquals(List.of(new CitaPublicaService.ClinicaPublica(clinica, "Clínica Uno")), servicio.clinicas(tipo, "x"));
    }
}
