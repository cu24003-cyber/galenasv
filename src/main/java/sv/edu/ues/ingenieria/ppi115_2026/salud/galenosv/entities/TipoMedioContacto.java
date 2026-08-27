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
@Table(name = "tipo_medio_contacto")
@NamedQueries({
    @NamedQuery(name = "TipoMedioContacto.findAll", query = "SELECT t FROM TipoMedioContacto t"),
    @NamedQuery(name = "TipoMedioContacto.findByNombre", query = "SELECT t FROM TipoMedioContacto t WHERE t.nombre = :nombre"),
    @NamedQuery(name = "TipoMedioContacto.findByIndicaciones", query = "SELECT t FROM TipoMedioContacto t WHERE t.indicaciones = :indicaciones"),
    @NamedQuery(name = "TipoMedioContacto.findByExpresionRegular", query = "SELECT t FROM TipoMedioContacto t WHERE t.expresionRegular = :expresionRegular"),
    @NamedQuery(name = "TipoMedioContacto.findByActivo", query = "SELECT t FROM TipoMedioContacto t WHERE t.activo = :activo")})
public class TipoMedioContacto implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_tipo_medio_contacto")
    private Object idTipoMedioContacto;
    @Column(name = "nombre")
    private String nombre;
    @Column(name = "indicaciones")
    private String indicaciones;
    @Column(name = "expresion_regular")
    private String expresionRegular;
    @Column(name = "activo")
    private Boolean activo;
    @OneToMany(mappedBy = "idTipoMedioContacto", fetch = FetchType.LAZY)
    private Collection<MedioContacto> medioContactoCollection;

    public TipoMedioContacto() {
    }

    public TipoMedioContacto(Object idTipoMedioContacto) {
        this.idTipoMedioContacto = idTipoMedioContacto;
    }

    public Object getIdTipoMedioContacto() {
        return idTipoMedioContacto;
    }

    public void setIdTipoMedioContacto(Object idTipoMedioContacto) {
        this.idTipoMedioContacto = idTipoMedioContacto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIndicaciones() {
        return indicaciones;
    }

    public void setIndicaciones(String indicaciones) {
        this.indicaciones = indicaciones;
    }

    public String getExpresionRegular() {
        return expresionRegular;
    }

    public void setExpresionRegular(String expresionRegular) {
        this.expresionRegular = expresionRegular;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Collection<MedioContacto> getMedioContactoCollection() {
        return medioContactoCollection;
    }

    public void setMedioContactoCollection(Collection<MedioContacto> medioContactoCollection) {
        this.medioContactoCollection = medioContactoCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idTipoMedioContacto != null ? idTipoMedioContacto.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TipoMedioContacto)) {
            return false;
        }
        TipoMedioContacto other = (TipoMedioContacto) object;
        if ((this.idTipoMedioContacto == null && other.idTipoMedioContacto != null) || (this.idTipoMedioContacto != null && !this.idTipoMedioContacto.equals(other.idTipoMedioContacto))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.TipoMedioContacto[ idTipoMedioContacto=" + idTipoMedioContacto + " ]";
    }
    
}
