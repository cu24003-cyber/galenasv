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
@Table(name = "procedimiento")
@NamedQueries({
    @NamedQuery(name = "Procedimiento.findAll", query = "SELECT p FROM Procedimiento p"),
    @NamedQuery(name = "Procedimiento.findByNombre", query = "SELECT p FROM Procedimiento p WHERE p.nombre = :nombre"),
    @NamedQuery(name = "Procedimiento.findByActivo", query = "SELECT p FROM Procedimiento p WHERE p.activo = :activo"),
    @NamedQuery(name = "Procedimiento.findByObservaciones", query = "SELECT p FROM Procedimiento p WHERE p.observaciones = :observaciones")})
public class Procedimiento implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_procedimiento")
    private Object idProcedimiento;
    @Column(name = "nombre")
    private String nombre;
    @Column(name = "activo")
    private Boolean activo;
    @Column(name = "observaciones")
    private String observaciones;
    @OneToMany(mappedBy = "idProcedimiento", fetch = FetchType.LAZY)
    private Collection<ProcedimientoPaso> procedimientoPasoCollection;

    public Procedimiento() {
    }

    public Procedimiento(Object idProcedimiento) {
        this.idProcedimiento = idProcedimiento;
    }

    public Object getIdProcedimiento() {
        return idProcedimiento;
    }

    public void setIdProcedimiento(Object idProcedimiento) {
        this.idProcedimiento = idProcedimiento;
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

    public Collection<ProcedimientoPaso> getProcedimientoPasoCollection() {
        return procedimientoPasoCollection;
    }

    public void setProcedimientoPasoCollection(Collection<ProcedimientoPaso> procedimientoPasoCollection) {
        this.procedimientoPasoCollection = procedimientoPasoCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idProcedimiento != null ? idProcedimiento.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Procedimiento)) {
            return false;
        }
        Procedimiento other = (Procedimiento) object;
        if ((this.idProcedimiento == null && other.idProcedimiento != null) || (this.idProcedimiento != null && !this.idProcedimiento.equals(other.idProcedimiento))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Procedimiento[ idProcedimiento=" + idProcedimiento + " ]";
    }
    
}
