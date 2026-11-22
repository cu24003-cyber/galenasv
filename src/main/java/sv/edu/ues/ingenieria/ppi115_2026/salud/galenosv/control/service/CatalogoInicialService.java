package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;

/**
 * Inicialización idempotente: conserva los roles existentes y sus identificadores.
 * El Paciente existente se reconoce con la misma normalización que Rol.getCodigo()
 * (y que la unicidad de RolService), para no sembrar un segundo Paciente si el
 * usuario ya creó el suyo con otra escritura.
 */
@Singleton
@Startup
public class CatalogoInicialService {
    @PersistenceContext private EntityManager em;

    @PostConstruct
    public void inicializar() {
        boolean existe = em.createQuery("SELECT r FROM Rol r", Rol.class).getResultList().stream()
                .anyMatch(r -> "PACIENTE".equals(r.getCodigo()));
        if (!existe) {
            Rol rol = new Rol(UUID.randomUUID());
            rol.setNombre("Paciente");
            rol.setActivo(true);
            em.persist(rol);
        }
    }
}
