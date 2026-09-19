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
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 *
 * @author carlo_dev
 */
@Entity
@Table(name = "examen_resultado")
@NamedQueries({
    @NamedQuery(name = "ExamenResultado.findAll", query = "SELECT e FROM ExamenResultado e"),
    @NamedQuery(name = "ExamenResultado.findByFechaCreacion", query = "SELECT e FROM ExamenResultado e WHERE e.fechaCreacion = :fechaCreacion"),
    @NamedQuery(name = "ExamenResultado.findByResultado", query = "SELECT e FROM ExamenResultado e WHERE e.resultado = :resultado"),
    @NamedQuery(name = "ExamenResultado.findByInterpretacion", query = "SELECT e FROM ExamenResultado e WHERE e.interpretacion = :interpretacion"),
    @NamedQuery(name = "ExamenResultado.findByRutaAtestado", query = "SELECT e FROM ExamenResultado e WHERE e.rutaAtestado = :rutaAtestado")})
public class ExamenResultado implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
        @Column(name = "id_examen_resultado")
    private UUID idExamenResultado;
    @Column(name = "fecha_creacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;
    @Column(name = "resultado")
    private String resultado;
    @Column(name = "interpretacion")
    private String interpretacion;
    @Column(name = "ruta_atestado")
    private String rutaAtestado;
    @JoinColumn(name = "id_orden_examen", referencedColumnName = "id_orden_examen")
    @ManyToOne(fetch = FetchType.LAZY)
    private OrdenExamen idOrdenExamen;

    public ExamenResultado() {
    }

    public ExamenResultado(UUID idExamenResultado) {
        this.idExamenResultado = idExamenResultado;
    }

    public UUID getIdExamenResultado() {
        return idExamenResultado;
    }

    public void setIdExamenResultado(UUID idExamenResultado) {
        this.idExamenResultado = idExamenResultado;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getInterpretacion() {
        return interpretacion;
    }

    public void setInterpretacion(String interpretacion) {
        this.interpretacion = interpretacion;
    }

    public String getRutaAtestado() {
        return rutaAtestado;
    }

    public void setRutaAtestado(String rutaAtestado) {
        this.rutaAtestado = rutaAtestado;
    }

    public OrdenExamen getIdOrdenExamen() {
        return idOrdenExamen;
    }

    public void setIdOrdenExamen(OrdenExamen idOrdenExamen) {
        this.idOrdenExamen = idOrdenExamen;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idExamenResultado != null ? idExamenResultado.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ExamenResultado)) {
            return false;
        }
        ExamenResultado other = (ExamenResultado) object;
        return this.idExamenResultado != null && this.idExamenResultado.equals(other.idExamenResultado);
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ExamenResultado[ idExamenResultado=" + idExamenResultado + " ]";
    }
    
}
