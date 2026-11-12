package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoSesion;

@ExtendWith(MockitoExtension.class)
class AutenticacionFilterTest {
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock HttpSession sesion;
    @Mock FilterChain chain;
    @Mock AccesoSesion acceso;
    @InjectMocks AutenticacionFilter filtro;
    private final StringWriter cuerpo = new StringWriter();

    @BeforeEach void preparar() throws Exception {
        lenient().when(request.getContextPath()).thenReturn("/galenosv");
        lenient().when(request.getRequestURI()).thenReturn("/galenosv/api/auth/me");
        lenient().when(request.getServletPath()).thenReturn("/api");
        lenient().when(request.getPathInfo()).thenReturn("/auth/me");
        lenient().when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:9080/galenosv/api/auth/me"));
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(cuerpo));
    }
    @Test void anonimoNoCreaUnaSesionNiObtieneElContexto() throws Exception {
        filtro.doFilter(request, response, chain);
        verify(response).setStatus(401); verifyNoInteractions(chain, acceso);
        verify(request, never()).getSession(true); assertTrue(cuerpo.toString().contains("auth.sesion"));
        var json = jakarta.json.Json.createReader(new StringReader(cuerpo.toString())).readObject();
        assertEquals("/galenosv/api/auth/me", json.getString("path"));
        assertNotNull(json.getString("timestamp")); assertTrue(json.getJsonArray("details").isEmpty());
    }
    @Test void loginDesdeOtroOrigenNoLlegaAlServicio() throws Exception {
        when(request.getMethod()).thenReturn("POST"); when(request.getHeader("Origin")).thenReturn("https://ajeno.example");
        filtro.doFilter(request, response, chain);
        verify(response).setStatus(403); verifyNoInteractions(chain, acceso);
    }
    @Test void loginDesdeElOrigenPropioNoExigeCookiePrevia() throws Exception {
        when(request.getMethod()).thenReturn("POST"); when(request.getHeader("Origin")).thenReturn("http://localhost:9080");
        when(request.getRequestURI()).thenReturn("/galenosv/api/auth/login");
        filtro.doFilter(request, response, chain);
        verify(chain).doFilter(request, response); verifyNoInteractions(acceso);
        verify(response).setHeader("Cache-Control", "no-store");
    }
    @Test void cookieSinIdentidadValidaSeInvalida() throws Exception {
        when(request.getSession(false)).thenReturn(sesion);
        filtro.doFilter(request, response, chain);
        verify(sesion).invalidate(); verify(response).setStatus(401); verifyNoInteractions(chain);
    }
    @Test void logoutSinCsrfSeRechazaAunqueTengaCookie() throws Exception {
        when(request.getMethod()).thenReturn("POST"); when(request.getHeader("Origin")).thenReturn("http://localhost:9080");
        when(request.getSession(false)).thenReturn(sesion); when(acceso.vigente()).thenReturn(true);
        when(acceso.getCsrfToken()).thenReturn("secreto");
        filtro.doFilter(request, response, chain);
        verify(response).setStatus(403); verifyNoInteractions(chain);
    }
    @Test void escrituraConOrigenCookieYCsrfValidosContinua() throws Exception {
        when(request.getMethod()).thenReturn("POST"); when(request.getHeader("Origin")).thenReturn("http://localhost:9080");
        when(request.getHeader("X-CSRF-Token")).thenReturn("secreto"); when(request.getSession(false)).thenReturn(sesion);
        when(acceso.vigente()).thenReturn(true); when(acceso.getCsrfToken()).thenReturn("secreto");
        filtro.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
    @Test void tokenIncorrectoYOriginNullNoSeAceptan() {
        assertFalse(AutenticacionFilter.tokenValido("a", "b")); assertFalse(AutenticacionFilter.tokenValido(null, "a"));
        when(request.getHeader("Origin")).thenReturn("null"); assertFalse(AutenticacionFilter.origenPermitido(request));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"/api/personas", "/api/consultas", "/api/ordenes-examen", "/api/recepcion/pacientes"})
    void recursosClinicosNoAdmitenAnonimos(String ruta) throws Exception {
        when(request.getRequestURI()).thenReturn("/galenosv"+ruta);
        filtro.doFilter(request,response,chain);
        verify(response).setStatus(401); verifyNoInteractions(chain);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"/api/personas/uuid/documentos", "/api/consultas/uuid", "/api/atencion/rol-activo", "/api/recepcion/pacientes"})
    void todaEscrituraDeLaApiExigeCsrf(String ruta) throws Exception {
        when(request.getRequestURI()).thenReturn("/galenosv"+ruta); when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Origin")).thenReturn("http://localhost:9080");
        when(request.getSession(false)).thenReturn(sesion); when(acceso.vigente()).thenReturn(true); when(acceso.getCsrfToken()).thenReturn("secreto");
        filtro.doFilter(request,response,chain);
        verify(response).setStatus(403); verifyNoInteractions(chain);
    }
    @Test void rutaPublicaNoExigeSesionNiLaCrea() throws Exception {
        when(request.getRequestURI()).thenReturn("/galenosv/api/publico/tipos-documento");
        when(request.getPathInfo()).thenReturn("/publico/tipos-documento");
        filtro.doFilter(request, response, chain);
        verify(chain).doFilter(request, response); verifyNoInteractions(acceso);
        verify(request, never()).getSession(anyBoolean()); verify(request, never()).getSession();
        verify(response).setHeader("Cache-Control", "no-store"); verify(response).setHeader("X-Content-Type-Options", "nosniff");
    }
    @Test void segmentosPuntoNoSaltanLaSesionDesdeLaRutaPublica() throws Exception {
        // El contenedor normaliza pathInfo; la URI cruda conserva los "..".
        when(request.getRequestURI()).thenReturn("/galenosv/api/publico/../personas");
        when(request.getPathInfo()).thenReturn("/personas");
        filtro.doFilter(request, response, chain);
        verify(response).setStatus(401); verifyNoInteractions(chain);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"/publicoX/tipos-documento", "/publico", "/personas/publico/x"})
    void soloElPrefijoPublicoExactoQuedaExcluido(String ruta) throws Exception {
        when(request.getRequestURI()).thenReturn("/galenosv/api"+ruta); when(request.getPathInfo()).thenReturn(ruta);
        filtro.doFilter(request, response, chain);
        verify(response).setStatus(401); verifyNoInteractions(chain);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"/publico/clinicas", "/publico/citas"})
    void busquedaPublicaPorPostNoExigeSesionNiCsrfPeroSiOrigen(String ruta) throws Exception {
        when(request.getRequestURI()).thenReturn("/galenosv/api"+ruta); when(request.getPathInfo()).thenReturn(ruta);
        when(request.getMethod()).thenReturn("POST"); when(request.getHeader("Origin")).thenReturn("http://localhost:9080");
        filtro.doFilter(request, response, chain);
        verify(chain).doFilter(request, response); verifyNoInteractions(acceso);
    }
    @Test void busquedaPublicaDesdeOtroOrigenSeRechaza() throws Exception {
        // El origen se rechaza antes de mirar la ruta: la exclusión pública no lo salta.
        when(request.getRequestURI()).thenReturn("/galenosv/api/publico/citas");
        when(request.getMethod()).thenReturn("POST"); when(request.getHeader("Origin")).thenReturn("https://ajeno.example");
        filtro.doFilter(request, response, chain);
        verify(response).setStatus(403); verifyNoInteractions(chain);
    }
    @Test void filtroCubreTodaLaApi() {
        assertArrayEquals(new String[]{"/api/*"},AutenticacionFilter.class.getAnnotation(jakarta.servlet.annotation.WebFilter.class).urlPatterns());
    }
}
