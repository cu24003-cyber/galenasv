/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities;

import java.io.Serializable;
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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author carlo_dev
 */
@Entity
@Table(name = "examen_tipo_examen")
@NamedQueries({
    @NamedQuery(name = "ExamenTipoExamen.findAll", query = "SELECT e FROM ExamenTipoExamen e"),
    @NamedQuery(name = "ExamenTipoExamen.findByFechaCreacion", query = "SELECT e FROM ExamenTipoExamen e WHERE e.fechaCreacion = :fechaCreacion"),
    @NamedQuery(name = "ExamenTipoExamen.findByObservaciones", query = "SELECT e FROM ExamenTipoExamen e WHERE e.observaciones = :observaciones")})
public class ExamenTipoExamen implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_examen_tipo_examen")
    private Object idExamenTipoExamen;
    @Column(name = "fecha_creacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;
    @Column(name = "observaciones")
    private String observaciones;
    @JoinColumn(name = "id_examen", referencedColumnName = "id_examen")
    @ManyToOne(fetch = FetchType.LAZY)
    private Examen idExamen;
    @JoinColumn(name = "id_tipo_examen", referencedColumnName = "id_tipo_examen")
    @ManyToOne(fetch = FetchType.LAZY)
    private TipoExamen idTipoExamen;

    public ExamenTipoExamen() {
    }

    public ExamenTipoExamen(Object idExamenTipoExamen) {
        this.idExamenTipoExamen = idExamenTipoExamen;
    }

    public Object getIdExamenTipoExamen() {
        return idExamenTipoExamen;
    }

    public void setIdExamenTipoExamen(Object idExamenTipoExamen) {
        this.idExamenTipoExamen = idExamenTipoExamen;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Examen getIdExamen() {
        return idExamen;
    }

    public void setIdExamen(Examen idExamen) {
        this.idExamen = idExamen;
    }

    public TipoExamen getIdTipoExamen() {
        return idTipoExamen;
    }

    public void setIdTipoExamen(TipoExamen idTipoExamen) {
        this.idTipoExamen = idTipoExamen;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idExamenTipoExamen != null ? idExamenTipoExamen.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ExamenTipoExamen)) {
            return false;
        }
        ExamenTipoExamen other = (ExamenTipoExamen) object;
        if ((this.idExamenTipoExamen == null && other.idExamenTipoExamen != null) || (this.idExamenTipoExamen != null && !this.idExamenTipoExamen.equals(other.idExamenTipoExamen))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenTipoExamen[ idExamenTipoExamen=" + idExamenTipoExamen + " ]";
    }
    
}
