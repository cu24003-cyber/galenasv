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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;


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
        @Column(name = "id_examen")
    private UUID idExamen;
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

    public Examen(UUID idExamen) {
        this.idExamen = idExamen;
    }

        @PrePersist
    protected void prePersist() {
        if (activo == null) {
            activo = Boolean.TRUE;
        }
    }

    public UUID getIdExamen() {
        return idExamen;
    }

    public void setIdExamen(UUID idExamen) {
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
        if (this == object) {
            return true;
        }
        if (!(object instanceof Examen)) {
            return false;
        }
        Examen other = (Examen) object;
        return this.idExamen != null && this.idExamen.equals(other.idExamen);
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Examen[ idExamen=" + idExamen + " ]";
    }
    
}
