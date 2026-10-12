package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

    public interface RepositoryInterface<T, ID> {

    void create(T entity);

    T find(ID id);

    void update(T entity);

    void delete(ID id);

    List<T> findAll();

    List<T> findByFilters(Map<String, UUID> filters);

    boolean matchesFilters(ID id, Map<String, UUID> filters);
}
