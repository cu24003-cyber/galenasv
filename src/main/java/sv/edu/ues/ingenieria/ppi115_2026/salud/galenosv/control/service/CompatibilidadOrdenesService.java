package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ContenidoOrden;

/** No convierte datos: detecta una actualización que omitiría asociaciones antiguas. */
@Singleton
@Startup
public class CompatibilidadOrdenesService {
    @PersistenceContext EntityManager em;

    @PostConstruct
    public void verificar() {
        Number columnas = (Number) em.createNativeQuery("SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema=current_schema() AND table_name='orden_examen' AND column_name='id_examen'")
                .getSingleResult();
        if (columnas.intValue() == 0) return;
        try (var textos = em.createNativeQuery("SELECT indicaciones FROM orden_examen WHERE id_examen IS NOT NULL").getResultStream()) {
            if (textos.anyMatch(texto -> !ContenidoOrden.estructurado((String) texto))) {
                throw new IllegalStateException("Hay órdenes con id_examen pendientes de conversión. Detenga la aplicación, respalde la base y ejecute 20261121_ordenes_base_original.sql antes de desplegar.");
            }
        }
    }
}
