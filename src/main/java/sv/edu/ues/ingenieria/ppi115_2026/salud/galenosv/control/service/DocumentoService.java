package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Documento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.DocumentoRepository;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.RepositoryInterface;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.TipoDocumentoRepository;

@Stateless
public class DocumentoService extends AbstractService<Documento, UUID> {

    @Inject
    private DocumentoRepository documentoRepository;

    @Inject
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Override
    protected RepositoryInterface<Documento, UUID> getRepository() {
        return documentoRepository;
    }

    @Override
    protected UUID obtenerId(Documento entidad) {
        return entidad.getIdDocumento();
    }

    @Override
    protected void validar(Documento entidad) {
        super.validar(entidad);
        TipoDocumento seleccionado = entidad.getIdTipoDocumento();
        if (seleccionado == null) return;
        TipoDocumento tipo = seleccionado.getIdTipoDocumento() == null ? null
                : tipoDocumentoRepository.find(seleccionado.getIdTipoDocumento());
        if (tipo == null) {
            throw ServiceException.localizada("registro.seleccionInvalida", "Seleccione un tipo de documento válido.");
        }
        String nombre = tipo.getNombre() == null ? "" : Normalizer.normalize(tipo.getNombre().trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (!nombre.equals("dui") && !nombre.equals("documento unico de identidad")) return;
        UUID persona = entidad.getIdPersona() == null ? null : entidad.getIdPersona().getIdPersona();
        if (persona == null || documentoRepository.bloquearPersona(persona) == null) {
            throw ServiceException.localizada("registro.guardarPersonaPrimero", "Guarde la persona antes de agregar documentos.");
        }
        if (documentoRepository.existeDuiPorPersona(persona, entidad.getIdDocumento())) {
            throw ServiceException.localizada("documento.duiUnico", "Una persona no puede tener más de un DUI.");
        }
    }

    @Override
    public void crear(Documento entidad) {
        if (entidad.getIdDocumento() == null) {
            entidad.setIdDocumento(UUID.randomUUID());
        }
        super.crear(entidad);
    }

    public java.util.List<Documento> listarPorPersona(UUID idPersona) {
        return documentoRepository.listarPorPersona(idPersona);
    }

    public void eliminarDePersona(UUID id, UUID persona) {
        Documento documento = id == null ? null : documentoRepository.find(id);
        if (documento == null || persona == null || documento.getIdPersona() == null
                || !persona.equals(documento.getIdPersona().getIdPersona())) {
            throw ServiceException.localizada("registro.relacionAjena", "El registro no pertenece a la persona seleccionada.");
        }
        super.eliminar(id);
    }
}
