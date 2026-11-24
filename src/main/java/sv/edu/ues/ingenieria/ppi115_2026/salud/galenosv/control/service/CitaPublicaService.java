package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository.CitaPublicaRepository;

/**
 * HU1: búsqueda pública de citas. Solo expone id, fecha de inicio, clínica, nombre completo del
 * paciente (decisión del equipo: lo ve quien conoce el documento) y nombres de procedimientos. "Documento inexistente" y "documento sin datos" producen el mismo
 * resultado vacío.
 */
@Stateless
public class CitaPublicaService {
    public static final ZoneId ZONA = ZoneId.of("America/El_Salvador");
    static final long MAXIMO_DIAS = 366;

    public record ClinicaPublica(UUID idClinica, String nombre) {}
    /** nombrePaciente es "nombres apellidos" sin espacios sobrantes; null si la persona no tiene ninguno. */
    public record CitaPublica(UUID idConsulta, OffsetDateTime fechaInicio, String nombreClinica, String nombrePaciente,
            List<String> procedimientos) {}

    @Inject CitaPublicaRepository repositorio;

    public List<ClinicaPublica> clinicas(UUID tipo, String numero) {
        return repositorio.clinicas(tipo, numero).stream()
                .map(f -> new ClinicaPublica((UUID) f[0], (String) f[1])).toList();
    }

    /** desde y hasta son días locales de El Salvador; hasta se incluye completo. */
    public List<CitaPublica> citas(UUID tipo, String numero, UUID clinica, LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde) || ChronoUnit.DAYS.between(desde, hasta) > MAXIMO_DIAS) {
            throw new AccesoException(400, "citas.rango", "El rango de fechas no es válido (desde ≤ hasta, máximo 366 días).");
        }
        return completar(repositorio.citas(tipo, numero, clinica,
                desde.atStartOfDay(ZONA).toOffsetDateTime(), hasta.plusDays(1).atStartOfDay(ZONA).toOffsetDateTime()));
    }

    /** La cita solo si pertenece al documento; vacío en cualquier otro caso. */
    public Optional<CitaPublica> cita(UUID tipo, String numero, UUID idConsulta) {
        return completar(repositorio.cita(tipo, numero, idConsulta)).stream().findFirst();
    }

    private List<CitaPublica> completar(List<Object[]> filas) {
        if (filas.isEmpty()) return List.of();
        List<UUID> ids = filas.stream().map(f -> (UUID) f[0]).toList();
        Map<UUID, LinkedHashSet<String>> nombres = new HashMap<>();
        for (Object[] p : repositorio.procedimientos(ids)) {
            nombres.computeIfAbsent((UUID) p[0], k -> new LinkedHashSet<>()).add((String) p[1]);
        }
        return filas.stream().map(f -> new CitaPublica((UUID) f[0], (OffsetDateTime) f[1], (String) f[2],
                nombreCompleto((String) f[3], (String) f[4]),
                List.copyOf(nombres.getOrDefault((UUID) f[0], new LinkedHashSet<>())))).toList();
    }

    /** "nombres apellidos" sin espacios sobrantes; null si los dos faltan o están en blanco (las columnas aceptan nulos). */
    static String nombreCompleto(String nombres, String apellidos) {
        String completo = Stream.of(nombres, apellidos).filter(Objects::nonNull).map(String::strip)
                .filter(s -> !s.isEmpty()).collect(Collectors.joining(" "));
        return completo.isEmpty() ? null : completo;
    }
}
