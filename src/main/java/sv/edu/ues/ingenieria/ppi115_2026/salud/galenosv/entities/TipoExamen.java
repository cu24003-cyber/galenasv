/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities;

import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
        @Column(name = "id_tipo_examen")
    private UUID idTipoExamen;
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

    public TipoExamen(UUID idTipoExamen) {
        this.idTipoExamen = idTipoExamen;
    }

    public UUID getIdTipoExamen() {
        return idTipoExamen;
    }

    public void setIdTipoExamen(UUID idTipoExamen) {
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
        if (this == object) {
            return true;
        }
        if (!(object instanceof TipoExamen)) {
            return false;
        }
        TipoExamen other = (TipoExamen) object;
        return this.idTipoExamen != null && this.idTipoExamen.equals(other.idTipoExamen);
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoExamen[ idTipoExamen=" + idTipoExamen + " ]";
    }
    
}
