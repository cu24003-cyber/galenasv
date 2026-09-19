package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.DefaultDAOInterface;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
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
        try {
            getDao().create(entidad);
        } catch (EntityExistsException e) {
            throw new ServiceException("Ya existe un registro con ese identificador.", e);
        } catch (PersistenceException e) {
            throw new ServiceException("No se pudo crear el registro: " + causaLegible(e), e);
        }
    }

    public void actualizar(T entidad) {
        validar(entidad);
        if (getDao().find(obtenerId(entidad)) == null) {
            throw new ServiceException("No existe un registro con ese id para actualizar.");
        }
        try {
            getDao().update(entidad);
        } catch (PersistenceException e) {
            throw new ServiceException("No se pudo actualizar el registro: " + causaLegible(e), e);
        }
    }

    public void eliminar(ID id) {
        if (getDao().find(id) == null) {
            throw new ServiceException("No existe un registro con ese id para eliminar.");
        }
        try {
            getDao().delete(id);
        } catch (PersistenceException e) {
            throw new ServiceException("No se puede eliminar: el registro esta siendo utilizado por otro dato relacionado.", e);
        }
    }

    public T buscarPorId(ID id) {
        return getDao().find(id);
    }

    public List<T> listarTodos() {
        return getDao().findAll();
    }

    protected abstract ID obtenerId(T entidad);

    private String causaLegible(PersistenceException e) {
        Throwable causa = e.getCause();
        return (causa != null && causa.getMessage() != null) ? causa.getMessage() : e.getMessage();
    }
}