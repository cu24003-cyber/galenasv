package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.CitaPublicaService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.IntentosAcceso;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TipoDocumentoService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.TipoDocumento;

/**
 * Rutas sin sesión. AutenticacionFilter y PermisosApiFilter excluyen /api/publico/.
 * Nunca devuelven notas, valores clínicos ni resultados. Las búsquedas por documento son POST
 * para que el número nunca viaje en la URL.
 */
@Path("publico")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class PublicoResource {
    @Inject TipoDocumentoService tipos;
    @Inject CitaPublicaService citas;
    @Inject IntentosAcceso intentos;
    @Inject Validator validador;
    @Context HttpServletRequest request;

    /** Documento que identifica al paciente; mismas reglas que el login. */
    public static class BusquedaDocumento {
        @NotNull public UUID idTipoDocumento;
        @NotBlank @Size(max = 50) @Pattern(regexp = "[^\\r\\n]*") public String numeroDocumento;
    }

    /** HU1: documento, clínica y rango de días locales (hasta incluido). */
    public static class BusquedaCitas extends BusquedaDocumento {
        @NotNull public UUID idClinica;
        @NotNull public LocalDate desde;
        @NotNull public LocalDate hasta;
    }

    /** HU2: catálogo para el selector de tipo de documento de la pantalla de login. */
    @GET @Path("tipos-documento")
    public Response tiposDocumento() {
        var arreglo = Json.createArrayBuilder();
        tipos.listarActivos().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .forEach(t -> arreglo.add(tipo(t)));
        return Response.ok(arreglo.build().toString()).build();
    }

    /** HU1: clínicas donde el dueño del documento es paciente; [] si no existe o no tiene. */
    @POST @Path("clinicas") @Consumes(MediaType.APPLICATION_JSON)
    public Response clinicas(BusquedaDocumento datos) {
        validar(datos);
        var arreglo = Json.createArrayBuilder();
        citas.clinicas(datos.idTipoDocumento, datos.numeroDocumento).forEach(c -> arreglo.add(Json.createObjectBuilder()
                .add("idClinica", c.idClinica().toString())
                .add("nombre", c.nombre())));
        return Response.ok(arreglo.build().toString()).build();
    }

    /** HU1: citas del documento en la clínica y el rango; [] si no existe o no tiene. */
    @POST @Path("citas") @Consumes(MediaType.APPLICATION_JSON)
    public Response citas(BusquedaCitas datos) {
        validar(datos);
        var arreglo = Json.createArrayBuilder();
        citas.citas(datos.idTipoDocumento, datos.numeroDocumento, datos.idClinica, datos.desde, datos.hasta)
                .forEach(c -> arreglo.add(cita(c)));
        return Response.ok(arreglo.build().toString()).build();
    }

    static JsonObjectBuilder cita(CitaPublicaService.CitaPublica c) {
        var procedimientos = Json.createArrayBuilder();
        c.procedimientos().forEach(procedimientos::add);
        return Json.createObjectBuilder()
                .add("idConsulta", c.idConsulta().toString())
                .add("fechaInicio", c.fechaInicio().atZoneSameInstant(CitaPublicaService.ZONA).toOffsetDateTime()
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .add("nombreClinica", c.nombreClinica())
                .add("procedimientos", procedimientos);
    }

    /** Cuenta el intento antes de validar o consultar: los cuerpos inválidos también consumen el límite. */
    void validar(Object datos) {
        intentos.registrar(IntentosAcceso.Ambito.BUSQUEDA_PUBLICA, request.getRemoteAddr());
        if (datos == null) throw new AccesoException(400, "json.invalido", "El cuerpo debe ser un objeto JSON válido.");
        var errores = validador.validate(datos);
        if (!errores.isEmpty()) throw new ConstraintViolationException(errores);
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
