package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionSesion;

class ConsultaActivaFilterTest {
    private ConsultaActivaFilter filtro(AtencionSesion sesion) throws Exception {
        ConsultaActivaFilter f=new ConsultaActivaFilter();
        var campo=ConsultaActivaFilter.class.getDeclaredField("sesion"); campo.setAccessible(true); campo.set(f,sesion); return f;
    }
    @Test void bloqueaOtraPaginaHastaCerrarConsulta() throws Exception {
        AtencionSesion sesion=new AtencionSesion(); sesion.setConsulta(UUID.randomUUID());
        HttpServletRequest req=mock(HttpServletRequest.class); HttpServletResponse res=mock(HttpServletResponse.class); FilterChain chain=mock(FilterChain.class);
        when(req.getServletPath()).thenReturn("/paginas/procedimiento.xhtml"); when(req.getContextPath()).thenReturn("/galenosv");
        filtro(sesion).doFilter(req,res,chain);
        verify(res).sendRedirect("/galenosv/paginas/consulta.xhtml"); verifyNoInteractions(chain);
        sesion.setConsulta(null); filtro(sesion).doFilter(req,res,chain); verify(chain).doFilter(req,res);
    }
    @Test void permiteGuardarDentroDeConsulta() throws Exception {
        AtencionSesion sesion=new AtencionSesion(); sesion.setConsulta(UUID.randomUUID());
        HttpServletRequest req=mock(HttpServletRequest.class); HttpServletResponse res=mock(HttpServletResponse.class); FilterChain chain=mock(FilterChain.class);
        when(req.getServletPath()).thenReturn("/paginas/consulta.xhtml");
        filtro(sesion).doFilter(req,res,chain); verify(chain).doFilter(req,res); verifyNoInteractions(res);
    }
    @Test void redireccionAjaxUsaRespuestaParcialFaces() throws Exception {
        AtencionSesion sesion=new AtencionSesion(); sesion.setConsulta(UUID.randomUUID());
        HttpServletRequest req=mock(HttpServletRequest.class); HttpServletResponse res=mock(HttpServletResponse.class); FilterChain chain=mock(FilterChain.class);
        when(req.getServletPath()).thenReturn("/paginas/paciente-list.xhtml"); when(req.getContextPath()).thenReturn("/galenosv"); when(req.getHeader("Faces-Request")).thenReturn("partial/ajax");
        StringWriter salida=new StringWriter(); when(res.getWriter()).thenReturn(new PrintWriter(salida));
        filtro(sesion).doFilter(req,res,chain);
        assertTrue(salida.toString().contains("<redirect url=\"/galenosv/paginas/consulta.xhtml\"/>")); verifyNoInteractions(chain);
    }
    @Test void permiteCambiarRolDuranteConsulta() throws Exception {
        AtencionSesion sesion = new AtencionSesion(); sesion.setConsulta(UUID.randomUUID());
        for (String ruta : new String[]{"/paginas/cambiar-rol.xhtml", "/paginas/cambiar-rol.jsf"}) {
            HttpServletRequest req = mock(HttpServletRequest.class); HttpServletResponse res = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class); when(req.getServletPath()).thenReturn(ruta);
            filtro(sesion).doFilter(req, res, chain);
            verify(chain).doFilter(req, res); verifyNoInteractions(res);
        }
    }

}
