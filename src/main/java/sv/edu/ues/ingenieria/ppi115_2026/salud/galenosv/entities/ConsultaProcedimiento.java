/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
    @Lob
    @Column(name = "id_consulta_procedimiento")
    private Object idConsultaProcedimiento;
    @Lob
    @Column(name = "id_procedimiento")
    private Object idProcedimiento;
    @Column(name = "fecha_inicio")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaInicio;
    @Column(name = "fecha_fin")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaFin;
    @Column(name = "observaciones")
    private String observaciones;
    @OneToMany(mappedBy = "idConsultaProcedimiento", fetch = FetchType.LAZY)
    private Collection<ConsultaProcedimientoPaso> consultaProcedimientoPasoCollection;
    @JoinColumn(name = "id_consulta", referencedColumnName = "id_consulta")
    @ManyToOne(fetch = FetchType.LAZY)
    private Consulta idConsulta;

    public ConsultaProcedimiento() {
    }

    public ConsultaProcedimiento(Object idConsultaProcedimiento) {
        this.idConsultaProcedimiento = idConsultaProcedimiento;
    }

    public Object getIdConsultaProcedimiento() {
        return idConsultaProcedimiento;
    }

    public void setIdConsultaProcedimiento(Object idConsultaProcedimiento) {
        this.idConsultaProcedimiento = idConsultaProcedimiento;
    }

    public Object getIdProcedimiento() {
        return idProcedimiento;
    }

    public void setIdProcedimiento(Object idProcedimiento) {
        this.idProcedimiento = idProcedimiento;
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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ConsultaProcedimiento)) {
            return false;
        }
        ConsultaProcedimiento other = (ConsultaProcedimiento) object;
        if ((this.idConsultaProcedimiento == null && other.idConsultaProcedimiento != null) || (this.idConsultaProcedimiento != null && !this.idConsultaProcedimiento.equals(other.idConsultaProcedimiento))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ConsultaProcedimiento[ idConsultaProcedimiento=" + idConsultaProcedimiento + " ]";
    }
    
}
