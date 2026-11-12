package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoDocumentoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento;

/**
 * Rutas sin sesión. AutenticacionFilter y PermisosApiFilter excluyen /api/publico/.
 * Nunca devuelven notas, valores clínicos ni resultados.
 */
@Path("publico")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class PublicoResource {
    @Inject TipoDocumentoService tipos;

    /** HU2: catálogo para el selector de tipo de documento de la pantalla de login. */
    @GET @Path("tipos-documento")
    public Response tiposDocumento() {
        var arreglo = Json.createArrayBuilder();
        tipos.listarActivos().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .forEach(t -> arreglo.add(tipo(t)));
        return Response.ok(arreglo.build().toString()).build();
    }

    private static JsonObjectBuilder tipo(TipoDocumento t) {
        var json = Json.createObjectBuilder()
                .add("idTipoDocumento", t.getIdTipoDocumento().toString())
                .add("nombre", t.getNombre());
        opcional(json, "indicaciones", t.getIndicaciones());
        opcional(json, "expresionRegular", t.getExpresionRegular());
        return json;
    }

    private static void opcional(JsonObjectBuilder json, String campo, String valor) {
        if (valor == null || valor.isBlank()) json.addNull(campo); else json.add(campo, valor);
    }
}
