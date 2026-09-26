/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 *
 * @author carlo_dev
 */
@Entity
@Table(name = "medio_contacto")
@NamedQueries({
    @NamedQuery(name = "MedioContacto.findAll", query = "SELECT m FROM MedioContacto m"),
    @NamedQuery(name = "MedioContacto.findByValor", query = "SELECT m FROM MedioContacto m WHERE m.valor = :valor"),
    @NamedQuery(name = "MedioContacto.findByFechaCreacion", query = "SELECT m FROM MedioContacto m WHERE m.fechaCreacion = :fechaCreacion")})
public class MedioContacto implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
        @Column(name = "id_medio_contacto")
    private UUID idMedioContacto;
    @Column(name = "valor")
    private String valor;
    @Column(name = "fecha_creacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;
    @JoinColumn(name = "id_persona", referencedColumnName = "id_persona")
    @ManyToOne(fetch = FetchType.LAZY)
    private Persona idPersona;
    @JoinColumn(name = "id_tipo_medio_contacto", referencedColumnName = "id_tipo_medio_contacto")
    @ManyToOne(fetch = FetchType.LAZY)
    private TipoMedioContacto idTipoMedioContacto;

    @PrePersist
    protected void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = new Date();
        }
    }

    public MedioContacto() {
    }

    public MedioContacto(UUID idMedioContacto) {
        this.idMedioContacto = idMedioContacto;
    }

    public UUID getIdMedioContacto() {
        return idMedioContacto;
    }

    public void setIdMedioContacto(UUID idMedioContacto) {
        this.idMedioContacto = idMedioContacto;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Persona getIdPersona() {
        return idPersona;
    }

    public void setIdPersona(Persona idPersona) {
        this.idPersona = idPersona;
    }

    public TipoMedioContacto getIdTipoMedioContacto() {
        return idTipoMedioContacto;
    }

    public void setIdTipoMedioContacto(TipoMedioContacto idTipoMedioContacto) {
        this.idTipoMedioContacto = idTipoMedioContacto;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idMedioContacto != null ? idMedioContacto.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MedioContacto)) {
            return false;
        }
        MedioContacto other = (MedioContacto) object;
        return this.idMedioContacto != null && this.idMedioContacto.equals(other.idMedioContacto);
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.MedioContacto[ idMedioContacto=" + idMedioContacto + " ]";
    }
    
}
