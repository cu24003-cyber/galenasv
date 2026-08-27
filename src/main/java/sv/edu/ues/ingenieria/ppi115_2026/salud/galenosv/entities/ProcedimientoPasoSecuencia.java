/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities;

import java.io.Serializable;
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
import javax.persistence.Table;

/**
 *
 * @author carlo_dev
 */
@Entity
@Table(name = "procedimiento_paso_secuencia")
@NamedQueries({
    @NamedQuery(name = "ProcedimientoPasoSecuencia.findAll", query = "SELECT p FROM ProcedimientoPasoSecuencia p"),
    @NamedQuery(name = "ProcedimientoPasoSecuencia.findByTipoSecuencia", query = "SELECT p FROM ProcedimientoPasoSecuencia p WHERE p.tipoSecuencia = :tipoSecuencia")})
public class ProcedimientoPasoSecuencia implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Lob
    @Column(name = "id_procedimiento_paso_secuencia")
    private Object idProcedimientoPasoSecuencia;
    @Lob
    @Column(name = "id_procedimiento_paso_referencia")
    private Object idProcedimientoPasoReferencia;
    @Column(name = "tipo_secuencia")
    private String tipoSecuencia;
    @JoinColumn(name = "id_procedimiento_paso", referencedColumnName = "id_procedimiento_paso")
    @ManyToOne(fetch = FetchType.LAZY)
    private ProcedimientoPaso idProcedimientoPaso;

    public ProcedimientoPasoSecuencia() {
    }

    public ProcedimientoPasoSecuencia(Object idProcedimientoPasoSecuencia) {
        this.idProcedimientoPasoSecuencia = idProcedimientoPasoSecuencia;
    }

    public Object getIdProcedimientoPasoSecuencia() {
        return idProcedimientoPasoSecuencia;
    }

    public void setIdProcedimientoPasoSecuencia(Object idProcedimientoPasoSecuencia) {
        this.idProcedimientoPasoSecuencia = idProcedimientoPasoSecuencia;
    }

    public Object getIdProcedimientoPasoReferencia() {
        return idProcedimientoPasoReferencia;
    }

    public void setIdProcedimientoPasoReferencia(Object idProcedimientoPasoReferencia) {
        this.idProcedimientoPasoReferencia = idProcedimientoPasoReferencia;
    }

    public String getTipoSecuencia() {
        return tipoSecuencia;
    }

    public void setTipoSecuencia(String tipoSecuencia) {
        this.tipoSecuencia = tipoSecuencia;
    }

    public ProcedimientoPaso getIdProcedimientoPaso() {
        return idProcedimientoPaso;
    }

    public void setIdProcedimientoPaso(ProcedimientoPaso idProcedimientoPaso) {
        this.idProcedimientoPaso = idProcedimientoPaso;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idProcedimientoPasoSecuencia != null ? idProcedimientoPasoSecuencia.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProcedimientoPasoSecuencia)) {
            return false;
        }
        ProcedimientoPasoSecuencia other = (ProcedimientoPasoSecuencia) object;
        if ((this.idProcedimientoPasoSecuencia == null && other.idProcedimientoPasoSecuencia != null) || (this.idProcedimientoPasoSecuencia != null && !this.idProcedimientoPasoSecuencia.equals(other.idProcedimientoPasoSecuencia))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.ProcedimientoPasoSecuencia[ idProcedimientoPasoSecuencia=" + idProcedimientoPasoSecuencia + " ]";
    }
    
}
