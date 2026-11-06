package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id @Column(name = "id_usuario") private UUID idUsuario;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_persona", nullable = false, unique = true) private Persona persona;
    @Column(name = "nombre_usuario", nullable = false, length = 155) private String nombreUsuario;
    @Column(name = "correo", length = 254) private String correo;
    @JsonbTransient @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(name = "activo", nullable = false) private Boolean activo = false;
    @Column(name = "fecha_creacion", nullable = false) private OffsetDateTime fechaCreacion;

    @PrePersist void iniciar() {
        if (idUsuario == null) idUsuario = UUID.randomUUID();
        if (fechaCreacion == null) fechaCreacion = OffsetDateTime.now();
    }

    public UUID getIdUsuario() { return idUsuario; }
    public void setIdUsuario(UUID idUsuario) { this.idUsuario = idUsuario; }
    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    @JsonbTransient public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(OffsetDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
