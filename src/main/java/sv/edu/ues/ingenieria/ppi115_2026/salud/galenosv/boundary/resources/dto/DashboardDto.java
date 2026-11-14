package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.dto;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.DashboardService;

/**
 * JSON del dashboard (HU3). Se arma con JSON-P y no con JSON-B para conservar las llaves con
 * null (fechaFin de una consulta abierta, responsable sin asignar, valor de un paso pendiente).
 * Fechas con el offset de El Salvador (-06:00).
 */
public final class DashboardDto {
    private DashboardDto() {}

    public static JsonArray consultas(List<DashboardService.ConsultaDashboard> consultas) {
        JsonArrayBuilder arreglo = Json.createArrayBuilder();
        consultas.forEach(c -> arreglo.add(consulta(c)));
        return arreglo.build();
    }

    public static JsonObject consulta(DashboardService.ConsultaDashboard c) {
        JsonArrayBuilder procedimientos = Json.createArrayBuilder();
        c.procedimientos().forEach(p -> procedimientos.add(procedimiento(p)));
        JsonObjectBuilder json = Json.createObjectBuilder().add("idConsulta", c.idConsulta().toString());
        fecha(json, "fechaInicio", c.fechaInicio());
        fecha(json, "fechaFin", c.fechaFin());
        return json.add("pendienteParaMi", c.pendienteParaMi())
                .add("paciente", paciente(c.paciente()))
                .add("procedimientos", procedimientos).build();
    }

    static JsonObject paciente(DashboardService.Paciente p) {
        JsonArrayBuilder documentos = Json.createArrayBuilder();
        p.documentos().forEach(d -> documentos.add(Json.createObjectBuilder()
                .add("tipoDocumento", d.tipoDocumento()).add("numero", d.numero())));
        JsonObjectBuilder json = Json.createObjectBuilder();
        texto(json, "nombres", p.nombres());
        texto(json, "apellidos", p.apellidos());
        texto(json, "fechaNacimiento", p.fechaNacimiento() == null ? null : p.fechaNacimiento().toString());
        if (p.edad() == null) json.addNull("edad"); else json.add("edad", p.edad());
        return json.add("documentos", documentos).build();
    }

    static JsonObject procedimiento(DashboardService.ProcedimientoDashboard p) {
        JsonArrayBuilder pasos = Json.createArrayBuilder();
        p.pasos().forEach(s -> pasos.add(paso(s)));
        JsonObjectBuilder json = Json.createObjectBuilder().add("idConsultaProcedimiento", p.idConsultaProcedimiento().toString());
        texto(json, "nombre", p.nombre());
        fecha(json, "fechaInicio", p.fechaInicio());
        fecha(json, "fechaFin", p.fechaFin());
        return json.add("pasos", pasos).build();
    }

    static JsonObject paso(DashboardService.PasoDashboard s) {
        JsonObjectBuilder json = Json.createObjectBuilder().add("idConsultaProcedimientoPaso", s.idConsultaProcedimientoPaso().toString());
        texto(json, "nombre", s.nombre());
        json.add("orden", s.orden()).add("nivel", s.nivel());
        texto(json, "estado", s.estado());
        if (s.responsable() == null) json.addNull("responsable");
        else {
            JsonObjectBuilder responsable = Json.createObjectBuilder();
            texto(responsable, "nombre", s.responsable().nombre());
            texto(responsable, "rol", s.responsable().rol());
            json.add("responsable", responsable);
        }
        if (s.rolPaso() == null) json.addNull("rolPaso");
        else {
            JsonObjectBuilder rol = Json.createObjectBuilder().add("idRol", s.rolPaso().idRol().toString());
            texto(rol, "nombre", s.rolPaso().nombre());
            json.add("rolPaso", rol);
        }
        fecha(json, "fechaInicio", s.fechaInicio());
        fecha(json, "fechaFin", s.fechaFin());
        return json.add("tieneValor", s.tieneValor()).build();
    }

    public static JsonObject valor(DashboardService.ValorPaso v) {
        JsonObjectBuilder json = Json.createObjectBuilder().add("idConsultaProcedimientoPaso", v.idConsultaProcedimientoPaso().toString());
        texto(json, "nombre", v.nombre());
        texto(json, "estado", v.estado());
        fecha(json, "fechaFin", v.fechaFin());
        texto(json, "valor", v.valor());
        return json.build();
    }

    private static void texto(JsonObjectBuilder json, String llave, String valor) {
        if (valor == null) json.addNull(llave); else json.add(llave, valor);
    }

    private static void fecha(JsonObjectBuilder json, String llave, OffsetDateTime valor) {
        texto(json, llave, valor == null ? null
                : valor.atZoneSameInstant(DashboardService.ZONA).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
}
