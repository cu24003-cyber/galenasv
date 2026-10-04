/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;

/**
 *
 * @author carlo_dev
 */
@Entity
@Table(name = "consulta_procedimiento_paso")
@NamedQueries({
    @NamedQuery(name = "ConsultaProcedimientoPaso.findAll", query = "SELECT c FROM ConsultaProcedimientoPaso c"),
    @NamedQuery(name = "ConsultaProcedimientoPaso.findByFechaInicio", query = "SELECT c FROM ConsultaProcedimientoPaso c WHERE c.fechaInicio = :fechaInicio"),
    @NamedQuery(name = "ConsultaProcedimientoPaso.findByFechaFin", query = "SELECT c FROM ConsultaProcedimientoPaso c WHERE c.fechaFin = :fechaFin"),
    @NamedQuery(name = "ConsultaProcedimientoPaso.findByEstado", query = "SELECT c FROM ConsultaProcedimientoPaso c WHERE c.estado = :estado")})
public class ConsultaProcedimientoPaso implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
        @Column(name = "id_consulta_procedimiento_paso")
    private UUID idConsultaProcedimientoPaso;
    @Column(name = "fecha_inicio", columnDefinition = "timestamp with time zone")
    private OffsetDateTime fechaInicio;
    @Column(name = "fecha_fin", columnDefinition = "timestamp with time zone")
    private OffsetDateTime fechaFin;
    @Column(name = "estado" , length = 20)
    private String estado;
    @Column(name = "valor")
    private String valor;
    @JoinColumn(name = "id_consulta_procedimiento", referencedColumnName = "id_consulta_procedimiento")
    @ManyToOne(fetch = FetchType.LAZY)
    private ConsultaProcedimiento idConsultaProcedimiento;
    @JoinColumn(name = "id_persona_rol", referencedColumnName = "id_persona_rol")
    @ManyToOne(fetch = FetchType.LAZY)
    private PersonaRol idPersonaRol;
    @OneToMany(mappedBy = "idConsultaProcedimientoPaso", fetch = FetchType.LAZY)
    private Collection<OrdenExamen> ordenExamenCollection;

    @PrePersist
    protected void prePersist() {
        if (fechaInicio == null) {
            fechaInicio = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        }
    }

    @JoinColumn(name = "id_procedimiento_paso", referencedColumnName = "id_procedimiento_paso")
    @ManyToOne(fetch = FetchType.LAZY)
    private ProcedimientoPaso idProcedimientoPaso;

    public ProcedimientoPaso getIdProcedimientoPaso() { return idProcedimientoPaso; }
    public void setIdProcedimientoPaso(ProcedimientoPaso paso) { this.idProcedimientoPaso = paso; }

    public ConsultaProcedimientoPaso() {
    }

    public ConsultaProcedimientoPaso(UUID idConsultaProcedimientoPaso) {
        this.idConsultaProcedimientoPaso =  idConsultaProcedimientoPaso;
    }

    public UUID getIdConsultaProcedimientoPaso() {
        return idConsultaProcedimientoPaso;
    }

    public void setIdConsultaProcedimientoPaso(UUID idConsultaProcedimientoPaso) {
        this.idConsultaProcedimientoPaso =  idConsultaProcedimientoPaso;
    }

    public OffsetDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(OffsetDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public OffsetDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(OffsetDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public ConsultaProcedimiento getIdConsultaProcedimiento() {
        return idConsultaProcedimiento;
    }

    public void setIdConsultaProcedimiento(ConsultaProcedimiento idConsultaProcedimiento) {
        this.idConsultaProcedimiento = idConsultaProcedimiento;
    }

    public PersonaRol getIdPersonaRol() {
        return idPersonaRol;
    }

    public void setIdPersonaRol(PersonaRol idPersonaRol) {
        this.idPersonaRol = idPersonaRol;
    }

    public Collection<OrdenExamen> getOrdenExamenCollection() {
        return ordenExamenCollection;
    }

    public void setOrdenExamenCollection(Collection<OrdenExamen> ordenExamenCollection) {
        this.ordenExamenCollection = ordenExamenCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idConsultaProcedimientoPaso != null ? idConsultaProcedimientoPaso.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ConsultaProcedimientoPaso)) {
            return false;
        }
        ConsultaProcedimientoPaso other = (ConsultaProcedimientoPaso) object;
        return this.idConsultaProcedimientoPaso != null && this.idConsultaProcedimientoPaso.equals(other.idConsultaProcedimientoPaso);
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.ConsultaProcedimientoPaso[ idConsultaProcedimientoPaso=" + idConsultaProcedimientoPaso + " ]";
    }
    
}
