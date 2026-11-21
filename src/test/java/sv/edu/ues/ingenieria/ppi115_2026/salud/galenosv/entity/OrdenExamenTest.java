package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrdenExamenTest {
    private OrdenExamen cargar(String valor) throws Exception {
        OrdenExamen orden = new OrdenExamen(UUID.randomUUID());
        var campo = OrdenExamen.class.getDeclaredField("indicaciones");
        campo.setAccessible(true); campo.set(orden, valor);
        return orden;
    }
    private String almacenado(OrdenExamen orden) throws Exception {
        var campo = OrdenExamen.class.getDeclaredField("indicaciones");
        campo.setAccessible(true); return (String) campo.get(orden);
    }

    @Test void guardaExamenYTextoYLosRecuperaTrasRecarga() throws Exception {
        UUID examen = UUID.randomUUID();
        for (String texto : new String[]{null, "", "En ayunas", "Comillas \"x\"\nUnicode: ñ 🩺", "x".repeat(255)}) {
            OrdenExamen orden = new OrdenExamen(); orden.setIdExamen(examen); orden.setIndicaciones(texto);
            OrdenExamen recargada = cargar(almacenado(orden));
            assertEquals(examen, recargada.getIdExamen()); assertEquals(texto, recargada.getIndicaciones());
            recargada.setIndicaciones("Editado");
            assertEquals(examen, cargar(almacenado(recargada)).getIdExamen());
            assertEquals("Editado", cargar(almacenado(recargada)).getIndicaciones());
        }
    }

    @Test void conservaTextoLegadoSinAdivinarElExamen() throws Exception {
        for (String texto : new String[]{null, "Hemograma en ayunas", "{\"idExamen\":\"cualquier cosa\"}"}) {
            OrdenExamen orden = cargar(texto);
            assertNull(orden.getIdExamen()); assertEquals(texto, orden.getIndicaciones());
            orden.setIdExamen(UUID.randomUUID());
            assertEquals(texto, orden.getIndicaciones());
        }
    }

    @Test void textoQuePareceMetadatosNoPuedeCambiarElExamen() throws Exception {
        String literal = ContenidoOrden.guardar(UUID.randomUUID(), "Texto literal");
        OrdenExamen orden = new OrdenExamen(); orden.setIndicaciones(literal);
        assertNull(cargar(almacenado(orden)).getIdExamen());
        assertEquals(literal, cargar(almacenado(orden)).getIndicaciones());
    }

    @Test void quitarOCambiarElExamenConservaElTexto() throws Exception {
        OrdenExamen orden = new OrdenExamen(); orden.setIndicaciones("Indicaciones");
        orden.setIdExamen(UUID.randomUUID()); orden.setIdExamen(null);
        OrdenExamen recargada = cargar(almacenado(orden));
        assertNull(recargada.getIdExamen()); assertEquals("Indicaciones", recargada.getIndicaciones());
        assertTrue(ContenidoOrden.estructurado(almacenado(recargada)));
    }

    @Test void contenidoInvalidoNoInventaUnaRelacionNiPierdeElOriginal() throws Exception {
        String cabecera = ContenidoOrden.prefijo(UUID.randomUUID());
        for (String texto : new String[]{cabecera + "{", cabecera + "[]", cabecera + "{}",
                cabecera + "{\"indicaciones\":5}", cabecera + "{\"indicaciones\":null,\"extra\":1}",
                "@galenosv/orden/v1:no-uuid:{\"indicaciones\":null}",
                "@galenosv/orden/v2:desconocido", cabecera + "{\"indicaciones\":null} basura"}) {
            OrdenExamen orden = cargar(texto);
            assertFalse(ContenidoOrden.estructurado(texto), texto);
            assertNull(orden.getIdExamen(), texto); assertEquals(texto, orden.getIndicaciones());
        }
    }
}
