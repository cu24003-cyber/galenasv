package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

public abstract class DefaultDAO<T, ID>
        implements DefaultDAOInterface<T, ID> {

    @PersistenceContext
    protected EntityManager em;

    private final Class<T> entityClass;

    protected DefaultDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public T find(ID id) {
        return em.find(entityClass, id);
    }

    @Override
    public void create(T entity) {
        em.persist(entity);
    }

    @Override
    public void update(T entity) {
        em.merge(entity);
    }

    @Override
    public void delete(ID id) {
        T entity = find(id);

        if (entity != null) {
            em.remove(entity);
        }
    }

    @Override
    public List<T> findAll() {
        return em.createQuery(
                "SELECT e FROM " + entityClass.getSimpleName() + " e",
                entityClass
        ).getResultList();
    }
}
