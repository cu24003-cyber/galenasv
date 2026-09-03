/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;


/**
 *
 * @author carlo_dev
 */
@Entity
@Table(name = "consulta")
@NamedQueries({
    @NamedQuery(name = "Consulta.findAll", query = "SELECT c FROM Consulta c"),
    @NamedQuery(name = "Consulta.findByFechaInicio", query = "SELECT c FROM Consulta c WHERE c.fechaInicio = :fechaInicio"),
    @NamedQuery(name = "Consulta.findByFechaFin", query = "SELECT c FROM Consulta c WHERE c.fechaFin = :fechaFin"),
    @NamedQuery(name = "Consulta.findByReferenciaExterna", query = "SELECT c FROM Consulta c WHERE c.referenciaExterna = :referenciaExterna"),
    @NamedQuery(name = "Consulta.findByObservaciones", query = "SELECT c FROM Consulta c WHERE c.observaciones = :observaciones")})
public class Consulta implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_consulta")
    private UUID idConsulta;
    @Column(name = "fecha_inicio")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaInicio;
    @Column(name = "fecha_fin")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaFin;
    @Column(name = "referencia_externa")
    private String referenciaExterna;
    @Column(name = "observaciones")
    private String observaciones;
    @JoinColumn(name = "id_persona_rol", referencedColumnName = "id_persona_rol")
    @ManyToOne(fetch = FetchType.LAZY)
    private PersonaRol idPersonaRol;
    @OneToMany(mappedBy = "idConsulta", fetch = FetchType.LAZY)
    private Collection<ConsultaProcedimiento> consultaProcedimientoCollection;

    public Consulta() {
    }

    public Consulta(UUID idConsulta) {
        this.idConsulta =  idConsulta;
    }

    public UUID getIdConsulta() {
        return idConsulta;
    }

    public void setIdConsulta(UUID idConsulta) {
        this.idConsulta =  idConsulta;
    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Date getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Date fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna = referenciaExterna;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public PersonaRol getIdPersonaRol() {
        return idPersonaRol;
    }

    public void setIdPersonaRol(PersonaRol idPersonaRol) {
        this.idPersonaRol = idPersonaRol;
    }

    public Collection<ConsultaProcedimiento> getConsultaProcedimientoCollection() {
        return consultaProcedimientoCollection;
    }

    public void setConsultaProcedimientoCollection(Collection<ConsultaProcedimiento> consultaProcedimientoCollection) {
        this.consultaProcedimientoCollection = consultaProcedimientoCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idConsulta != null ? idConsulta.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Consulta)) {
            return false;
        }
        Consulta other = (Consulta) object;
        if ((this.idConsulta == null && other.idConsulta != null) || (this.idConsulta != null && !this.idConsulta.equals(other.idConsulta))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Consulta[ idConsulta=" + idConsulta + " ]";
    }
    
}
