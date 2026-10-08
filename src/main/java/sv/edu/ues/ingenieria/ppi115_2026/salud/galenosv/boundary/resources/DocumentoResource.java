package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto.DocumentoDto;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.DocumentoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.PersonaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoDocumentoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Documento;

@Path("documentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class DocumentoResource extends AbstractResource<Documento, DocumentoDto> {

    @Inject
    private DocumentoService service;

    @Inject
    private PersonaService personaService;

    @Inject
    private TipoDocumentoService tipoDocumentoService;

    @Override
    protected DocumentoService getService() {
        return service;
    }

    @Override
    protected Documento nuevaEntidad(UUID id) {
        return new Documento(id);
    }

    @Override
    protected DocumentoDto toDto(Documento entidad) {
        DocumentoDto dto = new DocumentoDto();
        dto.setIdDocumento(entidad.getIdDocumento());
        dto.setValor(entidad.getValor());
        dto.setRutaFisica(entidad.getRutaFisica());
        dto.setIdPersona(entidad.getIdPersona() == null ? null : entidad.getIdPersona().getIdPersona());
        dto.setIdTipoDocumento(entidad.getIdTipoDocumento() == null ? null : entidad.getIdTipoDocumento().getIdTipoDocumento());
        return dto;
    }

    @Override
    protected void aplicar(DocumentoDto dto, Documento entidad) {
        entidad.setValor(dto.getValor());
        entidad.setRutaFisica(dto.getRutaFisica());
        entidad.setIdPersona(relacion(personaService, dto.getIdPersona(), "idPersona"));
        entidad.setIdTipoDocumento(relacion(tipoDocumentoService, dto.getIdTipoDocumento(), "idTipoDocumento"));
    }
}
