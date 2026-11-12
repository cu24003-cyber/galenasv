package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;
import java.io.*;
import java.util.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;

/** Autoriza también las rutas directas y los UUID enviados en cuerpos y filtros. */
@Provider @PreMatching @Priority(1000) @RequestScoped @Transactional
public class PermisosApiFilter implements ContainerRequestFilter {
    @Inject AutenticacionService autenticacion;
    @Inject AccesoSesion acceso;
    @Inject AtencionSesion atencion;
    @PersistenceContext EntityManager em;
    private static final Set<String> CATALOGOS = Set.of("roles", "clinicas", "tipos-documento", "tipos-medio-contacto",
            "tipos-examen", "examenes", "examenes-tipos-examen", "procedimientos", "procedimientos-pasos",
            "procedimientos-pasos-examenes", "procedimientos-pasos-secuencias");
    private static final Set<String> CLINICOS = Set.of("consultas-procedimientos", "consultas-procedimientos-pasos", "ordenes-examen", "examenes-resultados");
    private static final Map<String,String> RELACIONES = Map.of("idPersona", "personas", "idPersonaRol", "personas-roles",
            "idConsulta", "consultas", "idConsultaProcedimiento", "consultas-procedimientos",
            "idConsultaProcedimientoPaso", "consultas-procedimientos-pasos", "idOrdenExamen", "ordenes-examen");

    @Override public void filter(ContainerRequestContext request) throws IOException {
        List<String> partes = request.getUriInfo().getPathSegments().stream().map(p -> p.getPath()).toList();
        if (partes.isEmpty()) throw denegado();
        // Sin esto "publico/../consultas/{id}" saltaría las comprobaciones con la raíz "publico".
        if (partes.stream().anyMatch(p -> p.equals(".") || p.equals(".."))) throw new AccesoException(400, "ruta.invalida", "La ruta no es válida.");
        String raiz = partes.getFirst();
        // publico: rutas sin sesión (HU1/HU2); AutenticacionFilter también las excluye.
        if (Set.of("auth", "atencion", "publico").contains(raiz)) return;
        var actual = atencion.getRolActivo();
        var contexto = autenticacion.contexto(acceso.getIdUsuario(), actual == null ? null : actual.getIdPersonaRol());
        PersonaRol activo = contexto.asignacion();
        String rol = activo.getIdRol().getCodigo();
        boolean lectura = Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
        if (!Set.of("ADMINISTRACION", "DOCTOR", "PACIENTE").contains(rol)) throw denegado();
        if (raiz.equals("recepcion")) {
            if (!rol.equals("ADMINISTRACION")) throw denegado();
            return;
        }
        if (rol.equals("PACIENTE")) {
            boolean expediente = (raiz.equals("personas") && partes.size() >= 2 && uuid(partes.get(1)).equals(contexto.usuario().getPersona().getIdPersona()))
                    || (raiz.equals("personas-roles") && partes.size() >= 2 && uuid(partes.get(1)).equals(activo.getIdPersonaRol()));
            boolean catalogo = raiz.equals("tipos-documento") && partes.size() <= 2
                    || raiz.equals("clinicas") && partes.size() == 2 && uuid(partes.get(1)).equals(activo.getIdClinica().getIdClinica());
            boolean documento = request.getMethod().equals("POST") && raiz.equals("personas") && partes.size() == 3 && partes.get(2).equals("documentos");
            if (!(lectura && (expediente || catalogo) || documento)) throw denegado();
        }
        String recurso = raiz;
        boolean propietario = false;
        boolean accion = false;
        for (int i = 0; i < partes.size(); i += 2) {
            if (i > 0) {
                String siguiente = partes.get(i);
                if (i == partes.size()-1 && Set.of("cierre", "sesion", "responsable", "finalizacion", "examenes").contains(siguiente)
                        && (recurso.equals("consultas") || recurso.equals("consultas-procedimientos-pasos"))) { accion = true; break; }
                recurso = switch (recurso + "/" + siguiente) {
                    case "consultas/procedimientos" -> "consultas-procedimientos";
                    case "consultas-procedimientos/pasos" -> "consultas-procedimientos-pasos";
                    case "procedimientos/pasos" -> "procedimientos-pasos";
                    case "ordenes-examen/resultados" -> "examenes-resultados";
                    case "procedimientos-pasos/examenes-configurados" -> "procedimientos-pasos-examenes";
                    case "procedimientos-pasos/secuencias" -> "procedimientos-pasos-secuencias";
                    case "examenes/clasificaciones", "tipos-examen/clasificaciones" -> "examenes-tipos-examen";
                    default -> siguiente;
                };
            }
            if (recurso.equals("clinicas") && i + 2 < partes.size()
                    && Set.of("personas", "personas-roles", "consultas").contains(partes.get(i+2))) mismaClinica(uuid(partes.get(i+1)), activo);
            if (i + 1 < partes.size()) propietario |= verificar(recurso, uuid(partes.get(i+1)), activo, rol);
        }
        boolean coleccion = partes.size() % 2 == 1 && !accion;
        if (CATALOGOS.contains(recurso)) {
            if (!lectura && !rol.equals("ADMINISTRACION")) throw denegado();
        } else if (Set.of("personas", "personas-roles", "documentos", "medios-contacto").contains(recurso)) {
            if (!lectura && !rol.equals("ADMINISTRACION") && !rol.equals("PACIENTE")) throw denegado();
            // Las personas nuevas se registran junto con su asignación en una sola transacción.
            if (!lectura && recurso.equals("personas") && coleccion) throw denegado();
        } else if (recurso.equals("consultas")) {
            if (lectura && rol.equals("ADMINISTRACION")) throw denegado();
            if (!lectura && (request.getMethod().equals("DELETE") || request.getMethod().equals("PUT") && !accion)) throw denegado();
            if (!lectura && !rol.equals("DOCTOR") && !(rol.equals("ADMINISTRACION") && request.getMethod().equals("POST") && coleccion && partes.size() == 3 && raiz.equals("personas-roles"))) throw denegado();
            if (!lectura && coleccion && partes.size() == 1) throw denegado();
        } else if (CLINICOS.contains(recurso)) {
            if (rol.equals("ADMINISTRACION") || !lectura && !rol.equals("DOCTOR")) throw denegado();
            // Los POST clínicos deben pasar por las acciones y colecciones del flujo.
            if (!lectura && !accion && (partes.size() == 1 || request.getMethod().equals("PUT") || request.getMethod().equals("DELETE"))) throw denegado();
        } else throw denegado();

        if (lectura && coleccion) {
            if (Set.of("personas", "personas-roles", "consultas").contains(recurso)) {
                limitar(request, "idClinica", activo.getIdClinica().getIdClinica());
                if (rol.equals("PACIENTE") && recurso.equals("consultas")) limitar(request, "idPersonaRol", activo.getIdPersonaRol());
            } else if ((CLINICOS.contains(recurso) || Set.of("documentos", "medios-contacto").contains(recurso)) && !propietario) {
                String campo = switch (recurso) {
                    case "documentos", "medios-contacto" -> "idPersona";
                    case "consultas-procedimientos" -> "idConsulta";
                    case "consultas-procedimientos-pasos" -> "idConsultaProcedimiento";
                    case "ordenes-examen" -> "idConsultaProcedimientoPaso";
                    default -> "idOrdenExamen";
                };
                String id = request.getUriInfo().getQueryParameters().getFirst(campo);
                if (id == null) throw denegado();
                verificar(RELACIONES.get(campo), uuid(id), activo, rol);
            }
        }
        for (var entry : request.getUriInfo().getQueryParameters().entrySet()) {
            if (RELACIONES.containsKey(entry.getKey())) for (String id : entry.getValue()) verificar(RELACIONES.get(entry.getKey()), uuid(id), activo, rol);
            if (entry.getKey().equals("idClinica")) for (String id : entry.getValue()) mismaClinica(uuid(id), activo);
        }
        if (!lectura && request.hasEntity()) {
            byte[] bytes = request.getEntityStream().readNBytes(1_048_577);
            request.setEntityStream(new ByteArrayInputStream(bytes));
            if (bytes.length > 1_048_576) throw new AccesoException(413, "cuerpo.grande", "La solicitud es demasiado grande.");
            try (var reader = Json.createReader(new ByteArrayInputStream(bytes))) {
                JsonObject body = reader.readObject();
                if (body.containsKey("idClinica") && !body.isNull("idClinica")) mismaClinica(uuid(body.getString("idClinica")), activo);
                for (var relacion : RELACIONES.entrySet()) if (body.containsKey(relacion.getKey()) && !body.isNull(relacion.getKey())) verificar(relacion.getValue(), uuid(body.getString(relacion.getKey())), activo, rol);
            } catch (JsonException | ClassCastException e) { throw new AccesoException(400, "json.invalido", "El cuerpo debe ser un objeto JSON válido."); }
        }
    }
    private void limitar(ContainerRequestContext request, String campo, UUID id) {
        var valores = request.getUriInfo().getQueryParameters().get(campo);
        if (valores != null && (valores.size() != 1 || !uuid(valores.getFirst()).equals(id))) throw denegado();
        request.setRequestUri(request.getUriInfo().getRequestUriBuilder().replaceQueryParam(campo, id).build());
    }
    private boolean verificar(String recurso, UUID id, PersonaRol activo, String rol) {
        if (recurso.equals("personas")) {
            if (rol.equals("PACIENTE")) { if (!id.equals(activo.getIdPersona().getIdPersona())) throw denegado(); }
            else if (em.createQuery("SELECT COUNT(r) FROM PersonaRol r WHERE r.idPersona.idPersona=:persona AND r.idClinica.idClinica=:clinica", Long.class)
                    .setParameter("persona", id).setParameter("clinica", activo.getIdClinica().getIdClinica()).getSingleResult() == 0) throw denegado();
            return true;
        }
        if (recurso.equals("clinicas")) { if (!rol.equals("ADMINISTRACION")) mismaClinica(id, activo); return false; }
        PersonaRol propietario = switch (recurso) {
            case "personas-roles" -> existente(PersonaRol.class, id);
            case "consultas" -> existente(Consulta.class, id).getIdPersonaRol();
            case "consultas-procedimientos" -> existente(ConsultaProcedimiento.class, id).getIdConsulta().getIdPersonaRol();
            case "consultas-procedimientos-pasos" -> existente(ConsultaProcedimientoPaso.class, id).getIdConsultaProcedimiento().getIdConsulta().getIdPersonaRol();
            case "ordenes-examen" -> existente(OrdenExamen.class, id).getIdConsultaProcedimientoPaso().getIdConsultaProcedimiento().getIdConsulta().getIdPersonaRol();
            case "examenes-resultados" -> existente(ExamenResultado.class, id).getIdOrdenExamen().getIdConsultaProcedimientoPaso().getIdConsultaProcedimiento().getIdConsulta().getIdPersonaRol();
            case "documentos" -> { verificar("personas", existente(Documento.class,id).getIdPersona().getIdPersona(), activo, rol); yield null; }
            case "medios-contacto" -> { verificar("personas", existente(MedioContacto.class,id).getIdPersona().getIdPersona(), activo, rol); yield null; }
            default -> { if (!CATALOGOS.contains(recurso)) throw denegado(); yield null; }
        };
        if (propietario != null) {
            if (propietario.getIdClinica() == null) throw denegado();
            mismaClinica(propietario.getIdClinica().getIdClinica(), activo);
            if (rol.equals("PACIENTE") && !propietario.getIdPersonaRol().equals(activo.getIdPersonaRol())) throw denegado();
            return true;
        }
        return false;
    }
    private <T> T existente(Class<T> tipo, UUID id) { T valor = em.find(tipo, id); if (valor == null) throw new AccesoException(404, "recurso.noExiste", "El registro no existe."); return valor; }
    private void mismaClinica(UUID id, PersonaRol activo) { if (!id.equals(activo.getIdClinica().getIdClinica())) throw denegado(); }
    private static UUID uuid(String id) { try { UUID valor = UUID.fromString(id); if (!valor.toString().equalsIgnoreCase(id)) throw new IllegalArgumentException(); return valor; } catch (IllegalArgumentException e) { throw new AccesoException(400, "id.invalido", "El identificador debe ser un UUID válido."); } }
    private static AccesoException denegado() { return new AccesoException(403, "auth.permiso", "Tu perfil no tiene permiso para esta operación."); }
}
