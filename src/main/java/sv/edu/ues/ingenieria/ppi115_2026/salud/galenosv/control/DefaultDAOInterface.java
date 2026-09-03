package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control;

import java.util.List;

    public interface DefaultDAOInterface<T, ID> {

    void create(T entity);

    T find(ID id);

    void update(T entity);

    void delete(ID id);

    List<T> findAll();
}
