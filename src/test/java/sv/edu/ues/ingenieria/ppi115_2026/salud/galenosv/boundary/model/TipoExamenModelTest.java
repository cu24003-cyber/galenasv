package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoExamen;

class TipoExamenModelTest {
    @Test
    void nuevoTipoDeExamenVieneActivo() {
        TipoExamenModel model = new TipoExamenModel();
        model.nuevo();
        assertEquals(Boolean.TRUE, model.getSeleccionada().getActivo());
    }

    @Test
    void editarConservaTipoDeExamenInactivo() {
        TipoExamenModel model = new TipoExamenModel();
        TipoExamen original = new TipoExamen(UUID.randomUUID());
        original.setNombre("Electrofisiologia");
        original.setActivo(false);
        model.editar(original);
        assertEquals(Boolean.FALSE, model.getSeleccionada().getActivo());
    }
}
