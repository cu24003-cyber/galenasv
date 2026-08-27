/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities;

import java.io.Serializable;
import java.util.Collection;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author carlo_dev
 */
@Entity
@Table(name = "persona_rol")
@NamedQueries({
    @NamedQuery(name = "PersonaRol.findAll", query = "SELECT p FROM PersonaRol p"),
    @NamedQuery(name = "PersonaRol.findByFechaCreacion", query = "SELECT p FROM PersonaRol p WHERE p.fechaCreacion = :fechaCreacion")})
public class PersonaRol implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_persona_rol")
    private Object idPersonaRol;
    @Column(name = "fecha_creacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;
    @OneToMany(mappedBy = "idPersonaRol", fetch = FetchType.LAZY)
    private Collection<ConsultaProcedimientoPaso> consultaProcedimientoPasoCollection;
    @JoinColumn(name = "id_clinica", referencedColumnName = "id_clinica")
    @ManyToOne(fetch = FetchType.LAZY)
    private Clinica idClinica;
    @JoinColumn(name = "id_persona", referencedColumnName = "id_persona")
    @ManyToOne(fetch = FetchType.LAZY)
    private Persona idPersona;
    @JoinColumn(name = "id_rol", referencedColumnName = "id_rol")
    @ManyToOne(fetch = FetchType.LAZY)
    private Rol idRol;
    @OneToMany(mappedBy = "idPersonaRol", fetch = FetchType.LAZY)
    private Collection<Consulta> consultaCollection;

    public PersonaRol() {
    }

    public PersonaRol(Object idPersonaRol) {
        this.idPersonaRol = idPersonaRol;
    }

    public Object getIdPersonaRol() {
        return idPersonaRol;
    }

    public void setIdPersonaRol(Object idPersonaRol) {
        this.idPersonaRol = idPersonaRol;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Collection<ConsultaProcedimientoPaso> getConsultaProcedimientoPasoCollection() {
        return consultaProcedimientoPasoCollection;
    }

    public void setConsultaProcedimientoPasoCollection(Collection<ConsultaProcedimientoPaso> consultaProcedimientoPasoCollection) {
        this.consultaProcedimientoPasoCollection = consultaProcedimientoPasoCollection;
    }

    public Clinica getIdClinica() {
        return idClinica;
    }

    public void setIdClinica(Clinica idClinica) {
        this.idClinica = idClinica;
    }

    public Persona getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Persona idPersona) {
        this.idPersona = idPersona;
    }

    public Rol getIdRol() {
        return idRol;
    }

    public void setIdRol(Rol idRol) {
        this.idRol = idRol;
    }

    public Collection<Consulta> getConsultaCollection() {
        return consultaCollection;
    }

    public void setConsultaCollection(Collection<Consulta> consultaCollection) {
        this.consultaCollection = consultaCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idPersonaRol != null ? idPersonaRol.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PersonaRol)) {
            return false;
        }
        PersonaRol other = (PersonaRol) object;
        if ((this.idPersonaRol == null && other.idPersonaRol != null) || (this.idPersonaRol != null && !this.idPersonaRol.equals(other.idPersonaRol))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.PersonaRol[ idPersonaRol=" + idPersonaRol + " ]";
    }
    
}
