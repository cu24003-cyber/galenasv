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
@Table(name = "procedimiento_paso_examen")
@NamedQueries({
    @NamedQuery(name = "ProcedimientoPasoExamen.findAll", query = "SELECT p FROM ProcedimientoPasoExamen p"),
    @NamedQuery(name = "ProcedimientoPasoExamen.findByFechaCreacion", query = "SELECT p FROM ProcedimientoPasoExamen p WHERE p.fechaCreacion = :fechaCreacion"),
    @NamedQuery(name = "ProcedimientoPasoExamen.findByActivo", query = "SELECT p FROM ProcedimientoPasoExamen p WHERE p.activo = :activo"),
    @NamedQuery(name = "ProcedimientoPasoExamen.findByObservaciones", query = "SELECT p FROM ProcedimientoPasoExamen p WHERE p.observaciones = :observaciones")})
public class ProcedimientoPasoExamen implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_procedimiento_paso_examen")
    private Object idProcedimientoPasoExamen;
    @Column(name = "fecha_creacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;
    @Column(name = "activo")
    private Boolean activo;
    @Column(name = "observaciones")
    private String observaciones;
    @JoinColumn(name = "id_examen", referencedColumnName = "id_examen")
    @ManyToOne(fetch = FetchType.LAZY)
    private Examen idExamen;
    @JoinColumn(name = "id_procedimiento_paso", referencedColumnName = "id_procedimiento_paso")
    @ManyToOne(fetch = FetchType.LAZY)
    private ProcedimientoPaso idProcedimientoPaso;

    public ProcedimientoPasoExamen() {
    }

    public ProcedimientoPasoExamen(Object idProcedimientoPasoExamen) {
        this.idProcedimientoPasoExamen = idProcedimientoPasoExamen;
    }

    public Object getIdProcedimientoPasoExamen() {
        return idProcedimientoPasoExamen;
    }

    public void setIdProcedimientoPasoExamen(Object idProcedimientoPasoExamen) {
        this.idProcedimientoPasoExamen = idProcedimientoPasoExamen;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
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

    public ProcedimientoPaso getIdProcedimientoPaso() {
        return idProcedimientoPaso;
    }

    public void setIdProcedimientoPaso(ProcedimientoPaso idProcedimientoPaso) {
        this.idProcedimientoPaso = idProcedimientoPaso;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idProcedimientoPasoExamen != null ? idProcedimientoPasoExamen.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProcedimientoPasoExamen)) {
            return false;
        }
        ProcedimientoPasoExamen other = (ProcedimientoPasoExamen) object;
        if ((this.idProcedimientoPasoExamen == null && other.idProcedimientoPasoExamen != null) || (this.idProcedimientoPasoExamen != null && !this.idProcedimientoPasoExamen.equals(other.idProcedimientoPasoExamen))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoExamen[ idProcedimientoPasoExamen=" + idProcedimientoPasoExamen + " ]";
    }
    
}
