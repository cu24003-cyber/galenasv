package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.Objects;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RolRepository;

@Stateless(name = "RolService")
@LocalBean
public class RolService extends AbstractService<Rol, UUID> {

    @Inject
    private RolRepository rolRepository;

    @Override
    protected RepositoryInterface<Rol, UUID> getRepository() {
        return rolRepository;
    }

    @Override
    protected UUID obtenerId(Rol entidad) {
        return entidad.getIdRol();
    }

    /** Conservar la clasificación evita cambiar permisos por un simple renombrado. */
    public static void validarCambioNombre(Rol anterior, String nombre) {
        Rol nuevo = new Rol();
        nuevo.setNombre(nombre);
        String antes = anterior.getCodigo(), despues = nuevo.getCodigo();
        if (!Objects.equals(antes, despues) && (especial(antes) || especial(despues))) {
            throw ServiceException.localizada("rol.nombreReservado",
                    "Conserve los nombres Paciente y Atención al público o Administración para sus roles originales.");
        }
    }

    private static boolean especial(String codigo) {
        return "PACIENTE".equals(codigo) || "ADMINISTRACION".equals(codigo);
    }

    @Override
    public void actualizar(Rol entidad) {
        validar(entidad);
        Rol anterior = rolRepository.find(entidad.getIdRol());
        if (anterior != null) validarCambioNombre(anterior, entidad.getNombre());
        super.actualizar(entidad);
    }

    @Override
    public void crear(Rol entidad) {
        if (entidad.getIdRol() == null) {
            entidad.setIdRol(UUID.randomUUID());
        }
        super.crear(entidad);
    }

}
