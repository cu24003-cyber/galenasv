package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Clinica;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.ClinicaRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicaServiceTest {

    @Mock
    private ClinicaRepository clinicaRepository;

    @InjectMocks
    private ClinicaService clinicaService;

    @Test
    void crear_generaUUID_cuandoIdEsNulo() {
        Clinica c = new Clinica();
        c.setNombre("Clinica Central");

        clinicaService.crear(c);

        assertNotNull(c.getIdClinica());
        verify(clinicaRepository).create(c);
    }

    @Test
    void crear_noSobreescribeUUID_siYaTieneUno() {
        UUID idFijo = UUID.randomUUID();
        Clinica c = new Clinica();
        c.setIdClinica(idFijo);
        c.setNombre("Clinica Central");

        clinicaService.crear(c);

        assertEquals(idFijo, c.getIdClinica());
        verify(clinicaRepository).create(c);
    }

    @Test
    void crear_lanzaExcepcion_siNombreEsNulo() {
        Clinica c = new Clinica();
        c.setNombre(null);

        assertThrows(IllegalArgumentException.class, () -> clinicaService.crear(c));
        verify(clinicaRepository, never()).create(any());
    }

    @Test
    void crear_lanzaExcepcion_siNombreEstaEnBlanco() {
        Clinica c = new Clinica();
        c.setNombre("   ");

        assertThrows(IllegalArgumentException.class, () -> clinicaService.crear(c));
        verify(clinicaRepository, never()).create(any());
    }

    @Test
    void actualizar_lanzaServiceException_siNoExisteElId() {
        Clinica c = new Clinica();
        c.setIdClinica(UUID.randomUUID());
        c.setNombre("Clinica X");

        when(clinicaRepository.find(c.getIdClinica())).thenReturn(null);

        assertThrows(ServiceException.class, () -> clinicaService.actualizar(c));
        verify(clinicaRepository, never()).update(any());
    }

    @Test
    void eliminar_lanzaServiceException_siNoExisteElId() {
        UUID id = UUID.randomUUID();
        when(clinicaRepository.find(id)).thenReturn(null);

        assertThrows(ServiceException.class, () -> clinicaService.eliminar(id));
        verify(clinicaRepository, never()).delete(any());
    }

    @Test
    void buscarPorId_delegaAlRepository() {
        UUID id = UUID.randomUUID();
        Clinica esperada = new Clinica(id, "Clinica Central");
        when(clinicaRepository.find(id)).thenReturn(esperada);

        Clinica resultado = clinicaService.buscarPorId(id);

        assertSame(esperada, resultado);
    }
}
