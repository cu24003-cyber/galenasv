package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity;

import jakarta.persistence.Column;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsultaProcedimientoPasoTest {
    private ConsultaProcedimientoPaso cargar(String texto) throws Exception {
        var paso = new ConsultaProcedimientoPaso();
        var campo = ConsultaProcedimientoPaso.class.getDeclaredField("valor");
        campo.setAccessible(true); campo.set(paso, texto);
        return paso;
    }

    private String almacenado(ConsultaProcedimientoPaso paso) throws Exception {
        var campo = ConsultaProcedimientoPaso.class.getDeclaredField("valor");
        campo.setAccessible(true); return (String) campo.get(paso);
    }

    @Test void conservaValorYMotivoTrasRecargaEdicionYFinalizacion() throws Exception {
        for (String valor : new String[]{null, "", "120/80", "Comillas \"x\"\nUnicode ñ 🩺", "x".repeat(10000)}) {
            var paso = cargar(valor);
            paso.setMotivoRetroceso("Observaciones: verificar\nIndicaciones: repetir en reposo");
            var recargado = cargar(almacenado(paso));
            assertEquals(valor, recargado.getValor());
            assertEquals(paso.getMotivoRetroceso(), recargado.getMotivoRetroceso());
            recargado.setValor("Atención corregida");
            recargado.setEstado("COMPLETADO");
            var completado = cargar(almacenado(recargado));
            assertEquals("Atención corregida", completado.getValor());
            assertEquals(paso.getMotivoRetroceso(), completado.getMotivoRetroceso());
            completado.setValor(null);
            assertNull(cargar(almacenado(completado)).getValor());
            assertEquals(paso.getMotivoRetroceso(), cargar(almacenado(completado)).getMotivoRetroceso());
        }
    }

    @Test void textoLegadoSeConservaSinCambiarSuFormato() throws Exception {
        for (String valor : new String[]{null, "", "Signos previos", "{\"valor\":\"anterior\"}"}) {
            var paso = cargar(valor);
            assertEquals(valor, paso.getValor()); assertNull(paso.getMotivoRetroceso());
            paso.setValor(valor);
            assertEquals(valor, almacenado(paso));
        }
    }

    @Test void unValorEnviadoNoPuedeSuplantarElMotivo() throws Exception {
        String literal = ContenidoPaso.guardar("Registro falso", "Motivo falso");
        var paso = new ConsultaProcedimientoPaso();
        paso.setValor(literal);
        var recargado = cargar(almacenado(paso));
        assertEquals(literal, recargado.getValor()); assertNull(recargado.getMotivoRetroceso());
        paso.setMotivoRetroceso("Motivo auténtico");
        paso.setValor(literal);
        assertEquals("Motivo auténtico", cargar(almacenado(paso)).getMotivoRetroceso());
        assertEquals(literal, cargar(almacenado(paso)).getValor());
    }

    @Test void formatoDesconocidoODanadoNoPierdeElTexto() throws Exception {
        for (String contenido : new String[]{"{", "[]", "{}", "{\"valor\":5,\"motivoRetroceso\":null}",
                "{\"valor\":null,\"motivoRetroceso\":[]}", "{\"valor\":null,\"valor\":\"otro\"}",
                "{\"valor\":null,\"motivoRetroceso\":null,\"extra\":true}",
                "{\"valor\":null,\"motivoRetroceso\":null} basura"}) {
            String original = ContenidoPaso.PREFIJO + contenido;
            var paso = cargar(original);
            assertEquals(original, paso.getValor()); assertNull(paso.getMotivoRetroceso());
            paso.setMotivoRetroceso("Revisar");
            assertEquals(original, cargar(almacenado(paso)).getValor());
        }
        String futuro = "@galenosv/paso/v2:desconocido";
        assertEquals(futuro, cargar(futuro).getValor());
        assertNull(cargar(futuro).getMotivoRetroceso());
    }

    @Test void usaSoloLaColumnaValorDelParcheOriginal() throws Exception {
        String parche = Files.readString(Path.of("docker/prueba-bd/02-parche.sql"));
        assertTrue(parche.contains("ADD COLUMN valor text"));
        assertEquals("valor", ConsultaProcedimientoPaso.class.getDeclaredField("valor").getAnnotation(Column.class).name());
        assertThrows(NoSuchFieldException.class, () -> ConsultaProcedimientoPaso.class.getDeclaredField("motivoRetroceso"));
        for (var campo : ConsultaProcedimientoPaso.class.getDeclaredFields()) {
            Column columna = campo.getAnnotation(Column.class);
            if (columna != null) assertNotEquals("motivo_retroceso", columna.name());
        }
    }
}
