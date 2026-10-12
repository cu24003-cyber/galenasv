package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public abstract class AbstractRepository<T, ID>
        implements RepositoryInterface<T, ID> {

    @PersistenceContext
    protected EntityManager em;

    private final Class<T> entityClass;

    protected AbstractRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public T find(ID id) {
        return em.find(entityClass, id);
    }

    @Override
    public void create(T entity) {
        em.persist(entity);
        em.flush();
    }

    @Override
    public void update(T entity) {
        em.merge(entity);
        em.flush();
    }

    @Override
    public void delete(ID id) {
        T entity = find(id);
        if (entity != null) {
            em.remove(entity);
            em.flush();
        }
    }

    @Override
    public List<T> findAll() {
        return em.createQuery(
                "SELECT e FROM " + entityClass.getSimpleName() + " e",
                entityClass
        ).getResultList();
    }

    /** Rutas de atributos definidas por la aplicación; los valores siempre son parámetros. */
    @Override
    public List<T> findByFilters(Map<String, UUID> filters) {
        if (filters.isEmpty()) {
            return findAll();
        }
        return filteredQuery(filters, null).getResultList();
    }

    @Override
    public boolean matchesFilters(ID id, Map<String, UUID> filters) {
        return !filteredQuery(filters, id).setMaxResults(1).getResultList().isEmpty();
    }

    private TypedQuery<T> filteredQuery(Map<String, UUID> filters, ID id) {
        Map<String, String> aliases = new LinkedHashMap<>();
        Map<String, UUID> parameters = new LinkedHashMap<>();
        List<String> predicates = new ArrayList<>();
        StringBuilder joins = new StringBuilder();
        for (var filter : new TreeMap<>(filters).entrySet()) {
            if (!filter.getKey().matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*")
                    || filter.getValue() == null) {
                throw new IllegalArgumentException("El filtro debe indicar una ruta de atributos y un UUID.");
            }
            String[] attributes = filter.getKey().split("\\.");
            String alias = "e";
            String path = "";
            for (int i = 0; i < attributes.length - 1; i++) {
                path += "." + attributes[i];
                if (!aliases.containsKey(path)) {
                    String nextAlias = "r" + aliases.size();
                    joins.append(" JOIN ").append(alias).append('.').append(attributes[i])
                            .append(' ').append(nextAlias);
                    aliases.put(path, nextAlias);
                }
                alias = aliases.get(path);
            }
            String parameter = "f" + parameters.size();
            predicates.add(alias + "." + attributes[attributes.length - 1] + " = :" + parameter);
            parameters.put(parameter, filter.getValue());
        }
        // Compartir los JOIN permite combinar clínica y rol sobre la misma asignación.
        // DISTINCT evita repetir personas cuando coinciden varias asignaciones.
        if (id != null) predicates.add("e = :registro");
        var query = em.createQuery("SELECT DISTINCT e FROM " + entityClass.getSimpleName() + " e"
                + joins + (predicates.isEmpty() ? "" : " WHERE " + String.join(" AND ", predicates)), entityClass);
        parameters.forEach(query::setParameter);
        if (id != null) query.setParameter("registro", em.getReference(entityClass, id));
        return query;
    }
}
