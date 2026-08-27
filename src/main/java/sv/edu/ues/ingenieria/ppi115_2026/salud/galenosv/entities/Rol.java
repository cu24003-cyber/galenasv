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
@Table(name = "rol")
@NamedQueries({
    @NamedQuery(name = "Rol.findAll", query = "SELECT r FROM Rol r"),
    @NamedQuery(name = "Rol.findByNombre", query = "SELECT r FROM Rol r WHERE r.nombre = :nombre"),
    @NamedQuery(name = "Rol.findByActivo", query = "SELECT r FROM Rol r WHERE r.activo = :activo"),
    @NamedQuery(name = "Rol.findByObservaciones", query = "SELECT r FROM Rol r WHERE r.observaciones = :observaciones")})
public class Rol implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_rol")
    private Object idRol;
    @Column(name = "nombre")
    private String nombre;
    @Column(name = "activo")
    private Boolean activo;
    @Column(name = "observaciones")
    private String observaciones;
    @OneToMany(mappedBy = "idRol", fetch = FetchType.LAZY)
    private Collection<PersonaRol> personaRolCollection;
    @OneToMany(mappedBy = "idRol", fetch = FetchType.LAZY)
    private Collection<ProcedimientoPaso> procedimientoPasoCollection;

    public Rol() {
    }

    public Rol(Object idRol) {
        this.idRol = idRol;
    }

    public Object getIdRol() {
        return idRol;
    }

    public void setIdRol(Object idRol) {
        this.idRol = idRol;
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

    public Collection<PersonaRol> getPersonaRolCollection() {
        return personaRolCollection;
    }

    public void setPersonaRolCollection(Collection<PersonaRol> personaRolCollection) {
        this.personaRolCollection = personaRolCollection;
    }

    public Collection<ProcedimientoPaso> getProcedimientoPasoCollection() {
        return procedimientoPasoCollection;
    }

    public void setProcedimientoPasoCollection(Collection<ProcedimientoPaso> procedimientoPasoCollection) {
        this.procedimientoPasoCollection = procedimientoPasoCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idRol != null ? idRol.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Rol)) {
            return false;
        }
        Rol other = (Rol) object;
        if ((this.idRol == null && other.idRol != null) || (this.idRol != null && !this.idRol.equals(other.idRol))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Rol[ idRol=" + idRol + " ]";
    }
    
}
