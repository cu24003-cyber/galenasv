package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Procedimiento;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.Rol;

@ExtendWith(MockitoExtension.class)
class ConfiguracionProcedimientoServiceTest {
    @Mock EntityManager em;
    @Mock TypedQuery<Rol> consultaRoles;
    @InjectMocks ConfiguracionProcedimientoService servicio;

    private static Rol rol(String nombre) {
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre(nombre); rol.setActivo(true);
        return rol;
    }

    @Test
    void elSelectorDeRolesDelPasoNoOfreceAlPaciente() {
        Rol doctor = rol("Doctor"), paciente = rol("Paciente"), enfermera = rol("Enfermera");
        when(em.createQuery(anyString(), eq(Rol.class))).thenReturn(consultaRoles);
        when(consultaRoles.getResultList()).thenReturn(List.of(doctor, paciente, enfermera));
        assertEquals(List.of(doctor, enfermera), servicio.roles());
    }

    @Test
    void pasoRechazaElRolPaciente() {
        Procedimiento procedimiento = new Procedimiento(UUID.randomUUID());
        Rol paciente = rol("PACIENTE");
        when(em.find(Procedimiento.class, procedimiento.getIdProcedimiento(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(procedimiento);
        when(em.find(Rol.class, paciente.getIdRol())).thenReturn(paciente);
        ServiceException error = assertThrows(ServiceException.class, () -> servicio.paso(
                procedimiento.getIdProcedimiento(), "Revision", false, paciente.getIdRol(), null));
        assertEquals("procedimiento.pasoRolPaciente", error.getMessageKey());
        verify(em, never()).persist(any());
    }
}
