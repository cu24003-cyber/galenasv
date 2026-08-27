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
@Table(name = "examen_resultado")
@NamedQueries({
    @NamedQuery(name = "ExamenResultado.findAll", query = "SELECT e FROM ExamenResultado e"),
    @NamedQuery(name = "ExamenResultado.findByFechaCreacion", query = "SELECT e FROM ExamenResultado e WHERE e.fechaCreacion = :fechaCreacion"),
    @NamedQuery(name = "ExamenResultado.findByResultado", query = "SELECT e FROM ExamenResultado e WHERE e.resultado = :resultado"),
    @NamedQuery(name = "ExamenResultado.findByInterpretacion", query = "SELECT e FROM ExamenResultado e WHERE e.interpretacion = :interpretacion"),
    @NamedQuery(name = "ExamenResultado.findByRutaAtestado", query = "SELECT e FROM ExamenResultado e WHERE e.rutaAtestado = :rutaAtestado")})
public class ExamenResultado implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_examen_resultado")
    private Object idExamenResultado;
    @Column(name = "fecha_creacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;
    @Column(name = "resultado")
    private String resultado;
    @Column(name = "interpretacion")
    private String interpretacion;
    @Column(name = "ruta_atestado")
    private String rutaAtestado;
    @JoinColumn(name = "id_orden_examen", referencedColumnName = "id_orden_examen")
    @ManyToOne(fetch = FetchType.LAZY)
    private OrdenExamen idOrdenExamen;

    public ExamenResultado() {
    }

    public ExamenResultado(Object idExamenResultado) {
        this.idExamenResultado = idExamenResultado;
    }

    public Object getIdExamenResultado() {
        return idExamenResultado;
    }

    public void setIdExamenResultado(Object idExamenResultado) {
        this.idExamenResultado = idExamenResultado;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getInterpretacion() {
        return interpretacion;
    }

    public void setInterpretacion(String interpretacion) {
        this.interpretacion = interpretacion;
    }

    public String getRutaAtestado() {
        return rutaAtestado;
    }

    public void setRutaAtestado(String rutaAtestado) {
        this.rutaAtestado = rutaAtestado;
    }

    public OrdenExamen getIdOrdenExamen() {
        return idOrdenExamen;
    }

    public void setIdOrdenExamen(OrdenExamen idOrdenExamen) {
        this.idOrdenExamen = idOrdenExamen;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idExamenResultado != null ? idExamenResultado.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ExamenResultado)) {
            return false;
        }
        ExamenResultado other = (ExamenResultado) object;
        if ((this.idExamenResultado == null && other.idExamenResultado != null) || (this.idExamenResultado != null && !this.idExamenResultado.equals(other.idExamenResultado))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenResultado[ idExamenResultado=" + idExamenResultado + " ]";
    }
    
}
