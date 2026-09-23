package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service;

import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.repository.RepositoryInterface;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import java.util.List;

public abstract class AbstractService<T, ID> {

    protected abstract RepositoryInterface<T, ID> getRepository();

    protected void validar(T entidad) {
        if (entidad == null) {
            throw new IllegalArgumentException("La entidad no puede ser nula.");
        }
    }

    public void crear(T entidad) {
        validar(entidad);
        try {
            getRepository().create(entidad);
        } catch (EntityExistsException e) {
            throw new ServiceException("Ya existe un registro con ese identificador.", e);
        } catch (PersistenceException e) {
            throw new ServiceException("No se pudo crear el registro: " + causaLegible(e), e);
        }
    }

    public void actualizar(T entidad) {
        validar(entidad);
        if (getRepository().find(obtenerId(entidad)) == null) {
            throw new ServiceException("No existe un registro con ese id para actualizar.");
        }
        try {
            getRepository().update(entidad);
        } catch (PersistenceException e) {
            throw new ServiceException("No se pudo actualizar el registro: " + causaLegible(e), e);
        }
    }

    public void eliminar(ID id) {
        if (getRepository().find(id) == null) {
            throw new ServiceException("No existe un registro con ese id para eliminar.");
        }
        try {
            getRepository().delete(id);
        } catch (PersistenceException e) {
            throw new ServiceException("No se puede eliminar: el registro esta siendo utilizado por otro dato relacionado.", e);
        }
    }

    public T buscarPorId(ID id) {
        return getRepository().find(id);
    }

    public List<T> listarTodos() {
        return getRepository().findAll();
    }

    protected abstract ID obtenerId(T entidad);

    private String causaLegible(PersistenceException e) {
        Throwable causa = e.getCause();
        return (causa != null && causa.getMessage() != null) ? causa.getMessage() : e.getMessage();
    }
}