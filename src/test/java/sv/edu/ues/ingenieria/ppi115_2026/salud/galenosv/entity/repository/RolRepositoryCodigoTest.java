package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;

class RolRepositoryCodigoTest {
    private static Rol rol(String nombre) { Rol r = new Rol(UUID.randomUUID()); r.setNombre(nombre); r.setActivo(true); return r; }

    @SuppressWarnings("unchecked")
    private static EntityManager con(List<Rol> roles) {
        EntityManager em = mock(EntityManager.class);
        TypedQuery<Rol> q = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Rol.class))).thenReturn(q);
        when(q.getResultList()).thenReturn(roles);
        return em;
    }

    @Test
    void encuentraElPacienteConLaMismaNormalizacionQueGetCodigo() {
        Rol paciente = rol("  PACIÉNTE ");
        assertSame(paciente, RolRepository.buscarPorCodigo(con(List.of(rol("Doctor"), paciente)), "PACIENTE"));
    }

    @Test
    void devuelveNullSiNoExisteNingunRolConEseCodigo() {
        assertNull(RolRepository.buscarPorCodigo(con(List.of(rol("Doctor"), rol("Pacientes"))), "PACIENTE"));
    }
}
