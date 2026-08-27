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
@Table(name = "tipo_examen")
@NamedQueries({
    @NamedQuery(name = "TipoExamen.findAll", query = "SELECT t FROM TipoExamen t"),
    @NamedQuery(name = "TipoExamen.findByNombre", query = "SELECT t FROM TipoExamen t WHERE t.nombre = :nombre"),
    @NamedQuery(name = "TipoExamen.findByActivo", query = "SELECT t FROM TipoExamen t WHERE t.activo = :activo"),
    @NamedQuery(name = "TipoExamen.findByObservaciones", query = "SELECT t FROM TipoExamen t WHERE t.observaciones = :observaciones")})
public class TipoExamen implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_tipo_examen")
    private Object idTipoExamen;
    @Column(name = "nombre")
    private String nombre;
    @Column(name = "activo")
    private Boolean activo;
    @Column(name = "observaciones")
    private String observaciones;
    @OneToMany(mappedBy = "idTipoExamen", fetch = FetchType.LAZY)
    private Collection<ExamenTipoExamen> examenTipoExamenCollection;

    public TipoExamen() {
    }

    public TipoExamen(Object idTipoExamen) {
        this.idTipoExamen = idTipoExamen;
    }

    public Object getIdTipoExamen() {
        return idTipoExamen;
    }

    public void setIdTipoExamen(Object idTipoExamen) {
        this.idTipoExamen = idTipoExamen;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idTipoExamen != null ? idTipoExamen.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TipoExamen)) {
            return false;
        }
        TipoExamen other = (TipoExamen) object;
        if ((this.idTipoExamen == null && other.idTipoExamen != null) || (this.idTipoExamen != null && !this.idTipoExamen.equals(other.idTipoExamen))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoExamen[ idTipoExamen=" + idTipoExamen + " ]";
    }
    
}
