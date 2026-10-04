package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;

/** Inicialización idempotente: conserva los roles existentes y sus identificadores. */
@Singleton
@Startup
public class CatalogoInicialService {
    @PersistenceContext private EntityManager em;

    @PostConstruct
    public void inicializar() {
        Long cantidad = em.createQuery(
                "SELECT COUNT(r) FROM Rol r WHERE LOWER(TRIM(r.nombre)) = :nombre", Long.class)
                .setParameter("nombre", "paciente").getSingleResult();
        if (cantidad == 0) {
            Rol rol = new Rol(UUID.randomUUID());
            rol.setNombre("Paciente");
            rol.setActivo(true);
            em.persist(rol);
        }
    }
}
