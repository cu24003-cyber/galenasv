package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.model;

import java.io.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entities.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.service.ServiceException;
import static org.junit.jupiter.api.Assertions.*;

class AtencionSesionTest {
    private PersonaRol asignacion(Clinica clinica, String nombre) {
        PersonaRol pr = new PersonaRol(UUID.randomUUID());
        Rol rol = new Rol(UUID.randomUUID()); rol.setNombre(nombre); rol.setActivo(true);
        pr.setIdRol(rol); pr.setIdClinica(clinica); pr.setIdPersona(new Persona(UUID.randomUUID()));
        return pr;
    }

    @Test void contextoSeConservaEnSesionSinAfectarOtraSesion() throws Exception {
        AtencionSesion primera = new AtencionSesion(); AtencionSesion segunda = new AtencionSesion();
        PersonaRol doctor = asignacion(new Clinica(UUID.randomUUID()), "Médico"); primera.cambiarRol(doctor);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(primera); }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            AtencionSesion restaurada = (AtencionSesion) input.readObject();
            assertEquals(doctor.getIdPersonaRol(), restaurada.getRolActivo().getIdPersonaRol());
            assertEquals(doctor.getIdClinica(), restaurada.getRolActivo().getIdClinica());
        }
        assertNull(segunda.getRolActivo());
    }

    @Test void impideCambiarIdentidadDuranteConsulta() {
        AtencionSesion sesion = new AtencionSesion(); Clinica clinica = new Clinica(UUID.randomUUID());
        PersonaRol medico = asignacion(clinica, "Médico"); sesion.cambiarRol(medico);
        UUID consulta = UUID.randomUUID(); sesion.setConsulta(consulta);
        PersonaRol enfermera = asignacion(clinica, "Enfermería");
        assertThrows(ServiceException.class, () -> sesion.cambiarRol(enfermera));
        assertThrows(ServiceException.class, () -> sesion.cambiarRol(asignacion(new Clinica(UUID.randomUUID()), "Médico")));
        assertSame(medico, sesion.getRolActivo()); assertEquals(consulta, sesion.getConsulta());
    }

    @Test void permiteOtraClinicaTrasCerrarConsultaYRechazaRolesInactivos() {
        AtencionSesion sesion = new AtencionSesion(); sesion.cambiarRol(asignacion(new Clinica(UUID.randomUUID()), "Médico"));
        sesion.setConsulta(UUID.randomUUID()); sesion.setConsulta(null);
        PersonaRol otro = asignacion(new Clinica(UUID.randomUUID()), "Administración"); sesion.cambiarRol(otro);
        assertSame(otro, sesion.getRolActivo());
        PersonaRol inactivo = asignacion(otro.getIdClinica(), "Inactivo"); inactivo.getIdRol().setActivo(false);
        assertThrows(ServiceException.class, () -> sesion.cambiarRol(inactivo)); assertSame(otro, sesion.getRolActivo());
    }
}
