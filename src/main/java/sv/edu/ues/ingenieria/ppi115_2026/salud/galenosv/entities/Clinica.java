/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
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
@Table(name = "clinica")
@NamedQueries({
    @NamedQuery(name = "Clinica.findAll", query = "SELECT c FROM Clinica c"),
    @NamedQuery(name = "Clinica.findByNombre", query = "SELECT c FROM Clinica c WHERE c.nombre = :nombre"),
    @NamedQuery(name = "Clinica.findByActivo", query = "SELECT c FROM Clinica c WHERE c.activo = :activo"),
    @NamedQuery(name = "Clinica.findByTipo", query = "SELECT c FROM Clinica c WHERE c.tipo = :tipo"),
    @NamedQuery(name = "Clinica.findByComentarios", query = "SELECT c FROM Clinica c WHERE c.comentarios = :comentarios")})
public class Clinica implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
        @Column(name = "id_clinica")
    private UUID idClinica;
    @Basic(optional = false)
    @Column(name = "nombre")
    private String nombre;
    @Column(name = "activo")
    private Boolean activo;
    @Column(name = "tipo")
    private String tipo;
    @Column(name = "comentarios")
    private String comentarios;
    @OneToMany(mappedBy = "idClinica", fetch = FetchType.LAZY)
    private Collection<PersonaRol> personaRolCollection;

    public Clinica() {
    }

    public Clinica(UUID idClinica) {
        this.idClinica =  idClinica;
    }

    public Clinica(UUID idClinica, String nombre) {
        this.idClinica = idClinica;
        this.nombre = nombre;
    }

    public UUID getIdClinica() {
        return idClinica;
    }

    public void setIdClinica(UUID idClinica) {
        this.idClinica =  idClinica;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getComentarios() {
        return comentarios;
    }

    public void setComentarios(String comentarios) {
        this.comentarios = comentarios;
    }

    public Collection<PersonaRol> getPersonaRolCollection() {
        return personaRolCollection;
    }

    public void setPersonaRolCollection(Collection<PersonaRol> personaRolCollection) {
        this.personaRolCollection = personaRolCollection;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idClinica);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Clinica)) {
            return false;
        }
        Clinica other = (Clinica) object;
        return this.idClinica != null && this.idClinica.equals(other.idClinica);
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.Clinica[ idClinica=" + idClinica + " ]";
    }

}
