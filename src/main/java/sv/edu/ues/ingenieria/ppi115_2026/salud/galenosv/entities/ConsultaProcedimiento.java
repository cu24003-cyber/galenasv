/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities;

import java.io.Serializable;
import java.util.Collection;
import java.time.OffsetDateTime;
import java.util.UUID;

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
import jakarta.persistence.Table;

/**
 *
 * @author carlo_dev
 */
@Entity
@Table(name = "consulta_procedimiento")
@NamedQueries({
    @NamedQuery(name = "ConsultaProcedimiento.findAll", query = "SELECT c FROM ConsultaProcedimiento c"),
    @NamedQuery(name = "ConsultaProcedimiento.findByFechaInicio", query = "SELECT c FROM ConsultaProcedimiento c WHERE c.fechaInicio = :fechaInicio"),
    @NamedQuery(name = "ConsultaProcedimiento.findByFechaFin", query = "SELECT c FROM ConsultaProcedimiento c WHERE c.fechaFin = :fechaFin"),
    @NamedQuery(name = "ConsultaProcedimiento.findByObservaciones", query = "SELECT c FROM ConsultaProcedimiento c WHERE c.observaciones = :observaciones")})
public class ConsultaProcedimiento implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
        @Column(name = "id_consulta_procedimiento")
    private UUID idConsultaProcedimiento;
    @JoinColumn(name = "id_procedimiento", referencedColumnName = "id_procedimiento")
    @ManyToOne(fetch = FetchType.LAZY)
    private Procedimiento idProcedimiento;
    @Column(name = "fecha_inicio", columnDefinition = "timestamp with time zone")
    private OffsetDateTime fechaInicio;
    @Column(name = "fecha_fin", columnDefinition = "timestamp with time zone")
    private OffsetDateTime fechaFin;
    @Column(name = "observaciones")
    private String observaciones;
    @OneToMany(mappedBy = "idConsultaProcedimiento", fetch = FetchType.LAZY)
    private Collection<ConsultaProcedimientoPaso> consultaProcedimientoPasoCollection;
    @JoinColumn(name = "id_consulta", referencedColumnName = "id_consulta")
    @ManyToOne(fetch = FetchType.LAZY)
    private Consulta idConsulta;

    public ConsultaProcedimiento() {
    }

    public ConsultaProcedimiento(UUID idConsultaProcedimiento) {
        this.idConsultaProcedimiento =  idConsultaProcedimiento;
    }

    public UUID getIdConsultaProcedimiento() {
        return idConsultaProcedimiento;
    }

    public void setIdConsultaProcedimiento(UUID idConsultaProcedimiento) {
        this.idConsultaProcedimiento =  idConsultaProcedimiento;
    }

    public Procedimiento getIdProcedimiento() {
        return idProcedimiento;
    }

    public void setIdProcedimiento(Procedimiento idProcedimiento) {
        this.idProcedimiento = idProcedimiento;
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

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Collection<ConsultaProcedimientoPaso> getConsultaProcedimientoPasoCollection() {
        return consultaProcedimientoPasoCollection;
    }

    public void setConsultaProcedimientoPasoCollection(Collection<ConsultaProcedimientoPaso> consultaProcedimientoPasoCollection) {
        this.consultaProcedimientoPasoCollection = consultaProcedimientoPasoCollection;
    }

    public Consulta getIdConsulta() {
        return idConsulta;
    }

    public void setIdConsulta(Consulta idConsulta) {
        this.idConsulta = idConsulta;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idConsultaProcedimiento != null ? idConsultaProcedimiento.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ConsultaProcedimiento)) {
            return false;
        }
        ConsultaProcedimiento other = (ConsultaProcedimiento) object;
        return this.idConsultaProcedimiento != null && this.idConsultaProcedimiento.equals(other.idConsultaProcedimiento);
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ConsultaProcedimiento[ idConsultaProcedimiento=" + idConsultaProcedimiento + " ]";
    }
    
}
