package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.ws.rs.container.ResourceContext;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ConsultaProcedimientoPasoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ConsultaProcedimientoPaso;

/**
 * En Liberty, ResourceContext.getResource() de un recurso @RequestScoped devuelve un proxy CDI:
 * sus campos no son los de la instancia que atiende la petición. El mock que delega en la
 * instancia real reproduce ese comportamiento: las llamadas llegan a la instancia, las escrituras
 * de campos se quedan en el proxy.
 */
class ColeccionRelacionadaProxyTest {

    private static void inyectar(Object destino, String campo, Object valor) throws Exception {
        Field f = destino.getClass().getDeclaredField(campo);
        f.setAccessible(true);
        f.set(destino, valor);
    }

    @Test
    void elPadreSeFijaEnLaInstanciaRealAunqueElHijoSeaUnProxy() throws Exception {
        UUID paso = UUID.randomUUID();
        ConsultaProcedimientoPasoService pasos = mock(ConsultaProcedimientoPasoService.class);
        when(pasos.buscarPorId(paso)).thenReturn(new ConsultaProcedimientoPaso(paso));
        ConsultaProcedimientoPasoResource padre = new ConsultaProcedimientoPasoResource();
        inyectar(padre, "service", pasos);

        OrdenExamenResource real = new OrdenExamenResource();
        OrdenExamenResource proxy = mock(OrdenExamenResource.class, withSettings().defaultAnswer(AdditionalAnswers.delegatesTo(real)));
        ResourceContext contexto = mock(ResourceContext.class);
        when(contexto.getResource(OrdenExamenResource.class)).thenReturn(proxy);

        assertSame(proxy, padre.relacionados(paso.toString(), "ordenes-examen", contexto));
        assertEquals(paso, real.idPadre());
        assertEquals("idConsultaProcedimientoPaso", real.filtroPadre());
        // Con el contexto fijado, el hijo anidado solo admite las escrituras de la colección.
        assertEquals(java.util.Set.of("POST"), real.metodosEscritura());
    }
}
