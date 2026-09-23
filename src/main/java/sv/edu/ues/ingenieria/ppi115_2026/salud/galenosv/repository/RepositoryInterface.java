package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository;

import java.util.List;

    public interface RepositoryInterface<T, ID> {

    void create(T entity);

    T find(ID id);

    void update(T entity);

    void delete(ID id);

    List<T> findAll();
}
