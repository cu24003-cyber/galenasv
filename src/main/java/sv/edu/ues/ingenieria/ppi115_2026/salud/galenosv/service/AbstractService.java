package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import java.util.List;

public abstract class AbstractService<T, ID> {

    protected abstract DefaultDAOInterface<T, ID> getDao();

    protected void validar(T entidad) {
        if (entidad == null) {
            throw new IllegalArgumentException("La entidad no puede ser nula.");
        }
    }

    public void crear(T entidad) {
        validar(entidad);
        getDao().create(entidad);
    }

    public void actualizar(T entidad) {
        validar(entidad);
        if (getDao().find(obtenerId(entidad)) == null) {
            throw new IllegalArgumentException("No existe un registro con ese id para actualizar.");
        }
        getDao().update(entidad);
    }

    public void eliminar(ID id) {
        if (getDao().find(id) == null) {
            throw new IllegalArgumentException("No existe un registro con ese id para eliminar.");
        }
        getDao().delete(id);
    }

    public T buscarPorId(ID id) {
        return getDao().find(id);
    }

    public List<T> listarTodos() {
        return getDao().findAll();
    }

    protected abstract ID obtenerId(T entidad);
}