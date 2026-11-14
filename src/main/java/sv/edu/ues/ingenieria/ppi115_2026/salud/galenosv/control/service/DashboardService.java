package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.PersonaRol;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.DashboardRepository;

/**
 * HU3: dashboard de consultas de la clínica del rol activo (cualquier rol de empleado).
 *
 * PermisosApiFilter no revisa /atencion, así que aquí se exige sesión vigente, rol activo y
 * que ese rol siga siendo una asignación de empleado activa de la cuenta; la clínica se toma de
 * esa asignación leída de la base, nunca del cliente. No usa AtencionService.historial (que es
 * de JSF): hace su propia consulta por clínica y rango.
 *
 * Sin N+1: consultas, pasos, documentos y secuencias son 4 consultas para todo el rango. El
 * valor de un paso no viaja en el dashboard; se pide con {@link #valor(UUID)}.
 */
@Stateless
public class DashboardService {
    public static final ZoneId ZONA = ZoneId.of("America/El_Salvador");
    static final long MAXIMO_DIAS = 31;

    public record DocumentoPaciente(String tipoDocumento, String numero) {}
    public record Paciente(String nombres, String apellidos, LocalDate fechaNacimiento, Integer edad, List<DocumentoPaciente> documentos) {}
    public record Responsable(String nombre, String rol) {}
    /** B8: rol que atiende el paso: el de su definición o, en un paso histórico, el de su responsable. */
    public record RolPaso(UUID idRol, String nombre) {}
    public record PasoDashboard(UUID idConsultaProcedimientoPaso, String nombre, int orden, int nivel, String estado,
            Responsable responsable, OffsetDateTime fechaInicio, OffsetDateTime fechaFin, boolean tieneValor, RolPaso rolPaso) {}
    public record ProcedimientoDashboard(UUID idConsultaProcedimiento, String nombre, OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin, List<PasoDashboard> pasos) {}
    public record ConsultaDashboard(UUID idConsulta, OffsetDateTime fechaInicio, OffsetDateTime fechaFin,
            boolean pendienteParaMi, Paciente paciente, List<ProcedimientoDashboard> procedimientos) {}
    public record ValorPaso(UUID idConsultaProcedimientoPaso, String nombre, String estado, OffsetDateTime fechaFin, String valor) {}

    @Inject AccesoSesion acceso;
    @Inject AtencionSesion atencion;
    @Inject AutenticacionService autenticacion;
    @Inject DashboardRepository repositorio;
    Clock reloj = Clock.system(ZONA);

    /** desde/hasta: días yyyy-MM-dd de El Salvador, hasta incluido. Sin ninguno, hoy; con uno, ese día. */
    public List<ConsultaDashboard> consultas(String desde, String hasta) {
        UUID idActivo = exigirSesion();
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate d = fecha(desde), h = fecha(hasta);
        if (d == null && h == null) { d = hoy; h = hoy; }
        else if (d == null) d = h;
        else if (h == null) h = d;
        if (h.isBefore(d)) throw new AccesoException(400, "dashboard.rangoInvertido", "La fecha desde debe ser anterior o igual a hasta.");
        if (ChronoUnit.DAYS.between(d, h) + 1 > MAXIMO_DIAS)
            throw new AccesoException(400, "dashboard.rangoExcedido", "El rango de fechas admite como máximo " + MAXIMO_DIAS + " días.");
        PersonaRol activo = asignacion(idActivo);
        List<Object[]> consultas = new ArrayList<>(repositorio.consultas(activo.getIdClinica().getIdClinica(),
                d.atStartOfDay(ZONA).toOffsetDateTime(), h.plusDays(1).atStartOfDay(ZONA).toOffsetDateTime()));
        if (consultas.isEmpty()) return List.of();
        consultas.sort(Comparator.comparing((Object[] f) -> (OffsetDateTime) f[1]).thenComparing(f -> (UUID) f[0]));
        return armar(consultas, activo, hoy);
    }

    /**
     * B6: una consulta de la clínica del rol activo, con la misma forma que su elemento del
     * dashboard y sin límite de fechas. Inexistente o de otra clínica: el mismo 404.
     */
    public ConsultaDashboard consulta(UUID id) {
        PersonaRol activo = asignacion(exigirSesion());
        List<Object[]> filas = repositorio.consulta(id, activo.getIdClinica().getIdClinica());
        if (filas.isEmpty()) throw new AccesoException(404, "consulta.noExiste", "La consulta no existe.");
        return armar(filas, activo, LocalDate.now(reloj)).getFirst();
    }

    /** Valor completo de un paso de una consulta de la clínica del rol activo; si no, 404. */
    public ValorPaso valor(UUID idPaso) {
        PersonaRol activo = asignacion(exigirSesion());
        return repositorio.valorPaso(idPaso, activo.getIdClinica().getIdClinica()).stream().findFirst()
                .map(f -> new ValorPaso((UUID) f[0], (String) f[1], (String) f[2], local((OffsetDateTime) f[3]), (String) f[4]))
                .orElseThrow(() -> new AccesoException(404, "consulta.pasoNoExiste", "El paso no existe."));
    }

    private UUID exigirSesion() {
        if (!acceso.vigente()) throw new AccesoException(401, "auth.sesion", "Vuelva a iniciar sesión.");
        PersonaRol rol = atencion.getRolActivo();
        if (rol == null || rol.getIdPersonaRol() == null)
            throw new AccesoException(403, "auth.rolActivo", "Seleccione un rol y una clínica.");
        return rol.getIdPersonaRol();
    }

    /** Relee la asignación: 401 si el usuario ya no está activo, 403 si el rol ya no es suyo o no es de empleado. */
    private PersonaRol asignacion(UUID idActivo) {
        return autenticacion.contexto(acceso.getIdUsuario(), idActivo).asignacion();
    }

    private static LocalDate fecha(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try { return LocalDate.parse(valor.strip()); }
        catch (DateTimeParseException e) { throw new AccesoException(400, "dashboard.fecha", "Las fechas deben tener el formato yyyy-MM-dd."); }
    }

    private List<ConsultaDashboard> armar(List<Object[]> consultas, PersonaRol activo, LocalDate hoy) {
        UUID rolActivo = activo.getIdRol().getIdRol();
        List<UUID> ids = consultas.stream().map(f -> (UUID) f[0]).toList();
        List<UUID> personas = consultas.stream().map(f -> (UUID) f[3]).distinct().toList();
        List<Object[]> pasos = repositorio.pasos(ids);
        List<UUID> catalogos = pasos.stream().map(f -> (UUID) f[2]).filter(Objects::nonNull).distinct().toList();

        Map<UUID, List<DocumentoPaciente>> documentos = new HashMap<>();
        for (Object[] f : repositorio.documentos(personas))
            documentos.computeIfAbsent((UUID) f[0], k -> new ArrayList<>()).add(new DocumentoPaciente((String) f[1], (String) f[2]));
        Map<UUID, Integer> niveles = niveles(repositorio.secuencias(catalogos));

        // Procedimientos en el orden de la consulta (fechaInicio, id) y sus pasos.
        Map<UUID, Map<UUID, Object[]>> procedimientos = new HashMap<>();
        Map<UUID, List<Object[]>> pasosPorProcedimiento = new HashMap<>();
        // B8 pendienteParaMi: un paso PENDIENTE de mi rol, o (consulta abierta, sin pasos
        // pendientes) el último paso de finalización completado lo completé yo: me falta cerrarla.
        Map<UUID, Boolean> pendiente = new HashMap<>();
        Set<UUID> conPendientes = new HashSet<>();
        Map<UUID, Object[]> ultimoFinal = new HashMap<>();
        for (Object[] f : pasos) {
            procedimientos.computeIfAbsent((UUID) f[0], k -> new LinkedHashMap<>()).putIfAbsent((UUID) f[1], f);
            List<Object[]> lista = pasosPorProcedimiento.computeIfAbsent((UUID) f[1], k -> new ArrayList<>());
            if (f[6] == null) continue;
            lista.add(f);
            UUID rolPaso = f[15] != null ? (UUID) f[15] : (UUID) f[16];
            if (f[11] == null) conPendientes.add((UUID) f[0]);
            if ("PENDIENTE".equals(f[9]) && rolActivo.equals(rolPaso)) pendiente.put((UUID) f[0], true);
            if (Boolean.TRUE.equals(f[19]) && f[11] != null)
                ultimoFinal.merge((UUID) f[0], f, (a, b) -> ((OffsetDateTime) b[11]).isAfter((OffsetDateTime) a[11]) ? b : a);
        }
        for (Object[] c : consultas) {
            UUID idConsulta = (UUID) c[0];
            Object[] fin = ultimoFinal.get(idConsulta);
            if (c[2] == null && !conPendientes.contains(idConsulta) && fin != null && activo.getIdPersonaRol().equals(fin[18]))
                pendiente.put(idConsulta, true);
        }

        List<ConsultaDashboard> resultado = new ArrayList<>();
        for (Object[] c : consultas) {
            UUID id = (UUID) c[0];
            LocalDate nacimiento = dia(c[6]);
            Paciente paciente = new Paciente((String) c[4], (String) c[5], nacimiento, edad(nacimiento, hoy),
                    List.copyOf(documentos.getOrDefault((UUID) c[3], List.of())));
            List<ProcedimientoDashboard> procs = new ArrayList<>();
            for (Object[] p : procedimientos.getOrDefault(id, Map.of()).values()) {
                procs.add(new ProcedimientoDashboard((UUID) p[1], (String) p[3], local((OffsetDateTime) p[4]), local((OffsetDateTime) p[5]),
                        ordenar(pasosPorProcedimiento.getOrDefault((UUID) p[1], List.of()), niveles)));
            }
            resultado.add(new ConsultaDashboard(id, local((OffsetDateTime) c[1]), local((OffsetDateTime) c[2]),
                    pendiente.getOrDefault(id, false), paciente, List.copyOf(procs)));
        }
        return List.copyOf(resultado);
    }

    /**
     * Orden por secuencia: nivel = camino más largo desde un paso inicial del catálogo, así que
     * ordenar por nivel respeta cada arista. Empates por fechaInicio e id. Un paso sin
     * definición (histórico) va al final con nivel 0.
     */
    private static List<PasoDashboard> ordenar(List<Object[]> filas, Map<UUID, Integer> niveles) {
        Comparator<Object[]> porNivel = Comparator.comparing((Object[] f) -> f[7] == null ? 1 : 0)
                .thenComparing(f -> f[7] == null ? 0 : niveles.getOrDefault((UUID) f[7], 0))
                .thenComparing(f -> (OffsetDateTime) f[10], Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(f -> (UUID) f[6]);
        List<Object[]> ordenadas = filas.stream().sorted(porNivel).toList();
        List<PasoDashboard> pasos = new ArrayList<>();
        for (int i = 0; i < ordenadas.size(); i++) {
            Object[] f = ordenadas.get(i);
            Responsable responsable = f[12] == null && f[13] == null ? null
                    : new Responsable(nombre((String) f[12], (String) f[13]), (String) f[14]);
            pasos.add(new PasoDashboard((UUID) f[6], (String) f[8], i + 1,
                    f[7] == null ? 0 : niveles.getOrDefault((UUID) f[7], 0), (String) f[9], responsable,
                    local((OffsetDateTime) f[10]), local((OffsetDateTime) f[11]), Boolean.TRUE.equals(f[17]), rolPaso(f)));
        }
        return List.copyOf(pasos);
    }

    private static RolPaso rolPaso(Object[] f) {
        if (f[15] != null) return new RolPaso((UUID) f[15], (String) f[20]);
        if (f[16] != null) return new RolPaso((UUID) f[16], (String) f[14]);
        return null;
    }

    /** Camino más largo desde un nodo sin predecesores (Kahn). Los nodos en un ciclo quedan en 0. */
    static Map<UUID, Integer> niveles(List<Object[]> secuencias) {
        Map<UUID, List<UUID>> siguientes = new HashMap<>();
        Map<UUID, Integer> entradas = new HashMap<>();
        for (Object[] s : secuencias) {
            UUID origen = (UUID) s[0], destino = (UUID) s[1];
            siguientes.computeIfAbsent(origen, k -> new ArrayList<>()).add(destino);
            entradas.putIfAbsent(origen, 0);
            entradas.merge(destino, 1, Integer::sum);
        }
        Map<UUID, Integer> nivel = new HashMap<>();
        Deque<UUID> cola = new ArrayDeque<>();
        entradas.forEach((n, e) -> { if (e == 0) { cola.add(n); nivel.put(n, 0); } });
        while (!cola.isEmpty()) {
            UUID n = cola.poll();
            for (UUID m : siguientes.getOrDefault(n, List.of())) {
                nivel.merge(m, nivel.get(n) + 1, Math::max);
                if (entradas.merge(m, -1, Integer::sum) == 0) cola.add(m);
            }
        }
        entradas.keySet().forEach(n -> nivel.putIfAbsent(n, 0));
        return nivel;
    }

    static Integer edad(LocalDate nacimiento, LocalDate hoy) {
        return nacimiento == null ? null : Period.between(nacimiento, hoy).getYears();
    }

    /**
     * persona.fecha_nacimiento es timestamptz; la app la captura y la muestra en El Salvador
     * (p:datePicker y f:convertDateTime con timeZone America/El_Salvador), así que el día se
     * toma en esa zona y no en la de la JVM.
     */
    static LocalDate dia(Object valor) {
        if (valor instanceof java.sql.Date d) return d.toLocalDate();
        if (valor instanceof Date d) return Instant.ofEpochMilli(d.getTime()).atZone(ZONA).toLocalDate();
        if (valor instanceof OffsetDateTime d) return d.atZoneSameInstant(ZONA).toLocalDate();
        if (valor instanceof LocalDate d) return d;
        return null;
    }

    private static OffsetDateTime local(OffsetDateTime valor) {
        return valor == null ? null : valor.atZoneSameInstant(ZONA).toOffsetDateTime();
    }

    private static String nombre(String nombres, String apellidos) {
        return ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos)).strip();
    }
}
