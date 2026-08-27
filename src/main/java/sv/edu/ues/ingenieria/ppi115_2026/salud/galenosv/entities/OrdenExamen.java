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
@Table(name = "orden_examen")
@NamedQueries({
    @NamedQuery(name = "OrdenExamen.findAll", query = "SELECT o FROM OrdenExamen o"),
    @NamedQuery(name = "OrdenExamen.findByFechaCreacion", query = "SELECT o FROM OrdenExamen o WHERE o.fechaCreacion = :fechaCreacion"),
    @NamedQuery(name = "OrdenExamen.findByIndicaciones", query = "SELECT o FROM OrdenExamen o WHERE o.indicaciones = :indicaciones")})
public class OrdenExamen implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_orden_examen")
    private Object idOrdenExamen;
    @Column(name = "fecha_creacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;
    @Column(name = "indicaciones")
    private String indicaciones;
    @OneToMany(mappedBy = "idOrdenExamen", fetch = FetchType.LAZY)
    private Collection<ExamenResultado> examenResultadoCollection;
    @JoinColumn(name = "id_consulta_procedimiento_paso", referencedColumnName = "id_consulta_procedimiento_paso")
    @ManyToOne(fetch = FetchType.LAZY)
    private ConsultaProcedimientoPaso idConsultaProcedimientoPaso;

    public OrdenExamen() {
    }

    public OrdenExamen(Object idOrdenExamen) {
        this.idOrdenExamen = idOrdenExamen;
    }

    public Object getIdOrdenExamen() {
        return idOrdenExamen;
    }

    public void setIdOrdenExamen(Object idOrdenExamen) {
        this.idOrdenExamen = idOrdenExamen;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getIndicaciones() {
        return indicaciones;
    }

    public void setIndicaciones(String indicaciones) {
        this.indicaciones = indicaciones;
    }

    public Collection<ExamenResultado> getExamenResultadoCollection() {
        return examenResultadoCollection;
    }

    public void setExamenResultadoCollection(Collection<ExamenResultado> examenResultadoCollection) {
        this.examenResultadoCollection = examenResultadoCollection;
    }

    public ConsultaProcedimientoPaso getIdConsultaProcedimientoPaso() {
        return idConsultaProcedimientoPaso;
    }

    public void setIdConsultaProcedimientoPaso(ConsultaProcedimientoPaso idConsultaProcedimientoPaso) {
        this.idConsultaProcedimientoPaso = idConsultaProcedimientoPaso;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idOrdenExamen != null ? idOrdenExamen.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof OrdenExamen)) {
            return false;
        }
        OrdenExamen other = (OrdenExamen) object;
        if ((this.idOrdenExamen == null && other.idOrdenExamen != null) || (this.idOrdenExamen != null && !this.idOrdenExamen.equals(other.idOrdenExamen))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.OrdenExamen[ idOrdenExamen=" + idOrdenExamen + " ]";
    }
    
}
