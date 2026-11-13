package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources.provider;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity.*;

/**
 * En Liberty (RESTEasy) hasEntity() es true cuando la petición trae Content-Type aunque el cuerpo
 * esté vacío; Jersey, el de las pruebas HTTP, devuelve false. Esta prueba reproduce el caso de
 * Liberty con un ContainerRequestContext simulado.
 */
class PermisosApiFilterCuerpoTest {
    PermisosApiFilter filtro;
    ContainerRequestContext request;
    UUID paso = UUID.randomUUID();

    @BeforeEach void preparar() {
        Rol rol = new Rol(UUID.randomUUID()); rol.setCodigo("DOCTOR"); rol.setActivo(true);
        Clinica clinica = new Clinica(UUID.randomUUID());
        PersonaRol activo = new PersonaRol(UUID.randomUUID()); activo.setIdRol(rol); activo.setIdClinica(clinica);
        PersonaRol paciente = new PersonaRol(UUID.randomUUID()); paciente.setIdClinica(clinica);
        Consulta consulta = new Consulta(UUID.randomUUID()); consulta.setIdPersonaRol(paciente);
        ConsultaProcedimiento cp = new ConsultaProcedimiento(UUID.randomUUID()); cp.setIdConsulta(consulta);
        ConsultaProcedimientoPaso p = new ConsultaProcedimientoPaso(paso); p.setIdConsultaProcedimiento(cp);

        filtro = new PermisosApiFilter();
        filtro.em = mock(EntityManager.class);
        filtro.acceso = mock(AccesoSesion.class);
        filtro.atencion = mock(AtencionSesion.class);
        filtro.autenticacion = mock(AutenticacionService.class);
        when(filtro.atencion.getRolActivo()).thenReturn(activo);
        when(filtro.autenticacion.contexto(any(), any())).thenReturn(new AutenticacionService.Contexto(new Usuario(), activo));
        when(filtro.em.find(ConsultaProcedimientoPaso.class, paso)).thenReturn(p);

        request = mock(ContainerRequestContext.class);
        UriInfo uri = mock(UriInfo.class);
        var segmentos = Arrays.stream(new String[]{"consultas-procedimientos-pasos", paso.toString(), "retroceso"}).map(s -> {
            PathSegment ps = mock(PathSegment.class); when(ps.getPath()).thenReturn(s); return ps;
        }).toList();
        when(uri.getPathSegments()).thenReturn(segmentos);
        when(uri.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(request.getUriInfo()).thenReturn(uri);
        when(request.getMethod()).thenReturn("PUT");
        when(request.hasEntity()).thenReturn(true);
    }

    private byte[] reenviado() throws Exception {
        ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);
        verify(request).setEntityStream(captor.capture());
        return captor.getValue().readAllBytes();
    }

    @Test void cuerpoVacioConHasEntityNoDa400YLlegaComoObjetoVacio() throws Exception {
        when(request.getEntityStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        assertDoesNotThrow(() -> filtro.filter(request));
        assertEquals("{}", new String(reenviado()));
    }

    @Test void cuerpoEnBlancoLlegaComoObjetoVacio() throws Exception {
        when(request.getEntityStream()).thenReturn(new ByteArrayInputStream(" \r\n\t".getBytes()));
        assertDoesNotThrow(() -> filtro.filter(request));
        assertEquals("{}", new String(reenviado()));
    }

    @Test void fueraDeLasAccionesUnCuerpoVacioNoSeReemplaza() {
        UriInfo uri = request.getUriInfo();
        var segmentos = Arrays.stream(new String[]{"consultas-procedimientos-pasos", paso.toString(), "ordenes-examen"}).map(s -> {
            PathSegment ps = mock(PathSegment.class); when(ps.getPath()).thenReturn(s); return ps;
        }).toList();
        when(uri.getPathSegments()).thenReturn(segmentos);
        when(request.getMethod()).thenReturn("POST");
        when(request.getEntityStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        AccesoException e = assertThrows(AccesoException.class, () -> filtro.filter(request));
        assertEquals("json.invalido", e.getMessageKey());
    }

    @Test void cuerpoMalFormadoSigueDando400() {
        when(request.getEntityStream()).thenReturn(new ByteArrayInputStream("x".getBytes()));
        AccesoException e = assertThrows(AccesoException.class, () -> filtro.filter(request));
        assertEquals(400, e.getStatus());
        assertEquals("json.invalido", e.getMessageKey());
    }

    @Test void cuerpoConJsonSeReenviaIntacto() throws Exception {
        when(request.getEntityStream()).thenReturn(new ByteArrayInputStream("{\"valor\":\"x\"}".getBytes()));
        filtro.filter(request);
        assertEquals("{\"valor\":\"x\"}", new String(reenviado()));
    }
}
