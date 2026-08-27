/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 *
 * @author carlo_dev
 */
@Entity
@Table(name = "examen")
@NamedQueries({
    @NamedQuery(name = "Examen.findAll", query = "SELECT e FROM Examen e"),
    @NamedQuery(name = "Examen.findByNombre", query = "SELECT e FROM Examen e WHERE e.nombre = :nombre"),
    @NamedQuery(name = "Examen.findByActivo", query = "SELECT e FROM Examen e WHERE e.activo = :activo"),
    @NamedQuery(name = "Examen.findByObservaciones", query = "SELECT e FROM Examen e WHERE e.observaciones = :observaciones")})
public class Examen implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_examen")
    private Object idExamen;
    @Column(name = "nombre")
    private String nombre;
    @Column(name = "activo")
    private Boolean activo;
    @Column(name = "observaciones")
    private String observaciones;
    @OneToMany(mappedBy = "idExamen", fetch = FetchType.LAZY)
    private Collection<ExamenTipoExamen> examenTipoExamenCollection;
    @OneToMany(mappedBy = "idExamen", fetch = FetchType.LAZY)
    private Collection<ProcedimientoPasoExamen> procedimientoPasoExamenCollection;

    public Examen() {
    }

    public Examen(Object idExamen) {
        this.idExamen = idExamen;
    }

    public Object getIdExamen() {
        return idExamen;
    }

    public void setIdExamen(Object idExamen) {
        this.idExamen = idExamen;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public Collection<ExamenTipoExamen> getExamenTipoExamenCollection() {
        return examenTipoExamenCollection;
    }

    public void setExamenTipoExamenCollection(Collection<ExamenTipoExamen> examenTipoExamenCollection) {
        this.examenTipoExamenCollection = examenTipoExamenCollection;
    }

    public Collection<ProcedimientoPasoExamen> getProcedimientoPasoExamenCollection() {
        return procedimientoPasoExamenCollection;
    }

    public void setProcedimientoPasoExamenCollection(Collection<ProcedimientoPasoExamen> procedimientoPasoExamenCollection) {
        this.procedimientoPasoExamenCollection = procedimientoPasoExamenCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idExamen != null ? idExamen.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Examen)) {
            return false;
        }
        Examen other = (Examen) object;
        if ((this.idExamen == null && other.idExamen != null) || (this.idExamen != null && !this.idExamen.equals(other.idExamen))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Examen[ idExamen=" + idExamen + " ]";
    }
    
}
