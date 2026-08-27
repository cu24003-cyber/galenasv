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
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 *
 * @author carlo_dev
 */
@Entity
@Table(name = "procedimiento_paso")
@NamedQueries({
    @NamedQuery(name = "ProcedimientoPaso.findAll", query = "SELECT p FROM ProcedimientoPaso p"),
    @NamedQuery(name = "ProcedimientoPaso.findByNombre", query = "SELECT p FROM ProcedimientoPaso p WHERE p.nombre = :nombre"),
    @NamedQuery(name = "ProcedimientoPaso.findByIndicaFin", query = "SELECT p FROM ProcedimientoPaso p WHERE p.indicaFin = :indicaFin")})
public class ProcedimientoPaso implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_procedimiento_paso")
    private Object idProcedimientoPaso;
    @Column(name = "nombre")
    private String nombre;
    @Column(name = "indica_fin")
    private Boolean indicaFin;
    @OneToMany(mappedBy = "idProcedimientoPaso", fetch = FetchType.LAZY)
    private Collection<ProcedimientoPasoSecuencia> procedimientoPasoSecuenciaCollection;
    @OneToMany(mappedBy = "idProcedimientoPaso", fetch = FetchType.LAZY)
    private Collection<ProcedimientoPasoExamen> procedimientoPasoExamenCollection;
    @JoinColumn(name = "id_procedimiento", referencedColumnName = "id_procedimiento")
    @ManyToOne(fetch = FetchType.LAZY)
    private Procedimiento idProcedimiento;
    @JoinColumn(name = "id_rol", referencedColumnName = "id_rol")
    @ManyToOne(fetch = FetchType.LAZY)
    private Rol idRol;

    public ProcedimientoPaso() {
    }

    public ProcedimientoPaso(Object idProcedimientoPaso) {
        this.idProcedimientoPaso = idProcedimientoPaso;
    }

    public Object getIdProcedimientoPaso() {
        return idProcedimientoPaso;
    }

    public void setIdProcedimientoPaso(Object idProcedimientoPaso) {
        this.idProcedimientoPaso = idProcedimientoPaso;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getIndicaFin() {
        return indicaFin;
    }

    public void setIndicaFin(Boolean indicaFin) {
        this.indicaFin = indicaFin;
    }

    public Collection<ProcedimientoPasoSecuencia> getProcedimientoPasoSecuenciaCollection() {
        return procedimientoPasoSecuenciaCollection;
    }

    public void setProcedimientoPasoSecuenciaCollection(Collection<ProcedimientoPasoSecuencia> procedimientoPasoSecuenciaCollection) {
        this.procedimientoPasoSecuenciaCollection = procedimientoPasoSecuenciaCollection;
    }

    public Collection<ProcedimientoPasoExamen> getProcedimientoPasoExamenCollection() {
        return procedimientoPasoExamenCollection;
    }

    public void setProcedimientoPasoExamenCollection(Collection<ProcedimientoPasoExamen> procedimientoPasoExamenCollection) {
        this.procedimientoPasoExamenCollection = procedimientoPasoExamenCollection;
    }

    public Procedimiento getIdProcedimiento() {
        return idProcedimiento;
    }

    public void setIdProcedimiento(Procedimiento idProcedimiento) {
        this.idProcedimiento = idProcedimiento;
    }

    public Rol getIdRol() {
        return idRol;
    }

    public void setIdRol(Rol idRol) {
        this.idRol = idRol;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idProcedimientoPaso != null ? idProcedimientoPaso.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProcedimientoPaso)) {
            return false;
        }
        ProcedimientoPaso other = (ProcedimientoPaso) object;
        if ((this.idProcedimientoPaso == null && other.idProcedimientoPaso != null) || (this.idProcedimientoPaso != null && !this.idProcedimientoPaso.equals(other.idProcedimientoPaso))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPaso[ idProcedimientoPaso=" + idProcedimientoPaso + " ]";
    }
    
}
