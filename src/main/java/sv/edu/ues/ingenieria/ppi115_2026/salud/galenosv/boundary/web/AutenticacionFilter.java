package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.web;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AccesoSesion;

@WebFilter(urlPatterns = "/api/*")
public class AutenticacionFilter implements Filter {
    @Inject AccesoSesion acceso;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        var res = (HttpServletResponse) response;
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Pragma", "no-cache");
        res.setHeader("X-Content-Type-Options", "nosniff");
        boolean escritura = !java.util.Set.of("GET", "HEAD", "OPTIONS").contains(req.getMethod());
        if (escritura && !origenPermitido(req)) {
            error(req, res, 403, "auth.origen", "Origen de la solicitud no autorizado."); return;
        }
        if (req.getRequestURI().equals(req.getContextPath() + "/api/auth/login") && "POST".equals(req.getMethod())) {
            chain.doFilter(request, response); return;
        }
        if (rutaPublica(req)) {
            chain.doFilter(request, response); return;
        }
        var sesion = req.getSession(false);
        if (sesion == null || !acceso.vigente()) {
            if (sesion != null) sesion.invalidate();
            error(req, res, 401, "auth.sesion", "Vuelva a iniciar sesión."); return;
        }
        if (escritura && !tokenValido(req.getHeader("X-CSRF-Token"), acceso.getCsrfToken())) {
            error(req, res, 403, "auth.csrf", "No se pudo verificar la solicitud."); return;
        }
        chain.doFilter(request, response);
    }

    /**
     * /api/publico/* no requiere sesión. Se usa pathInfo, que el contenedor decodifica y
     * normaliza, para que "/api/publico/../personas" no herede la exclusión.
     */
    static boolean rutaPublica(HttpServletRequest request) {
        String ruta = request.getPathInfo();
        return "/api".equals(request.getServletPath()) && ruta != null && ruta.startsWith("/publico/");
    }

    static boolean tokenValido(String recibido, String esperado) {
        return recibido != null && esperado != null && MessageDigest.isEqual(
                recibido.getBytes(StandardCharsets.UTF_8), esperado.getBytes(StandardCharsets.UTF_8));
    }

    static boolean origenPermitido(HttpServletRequest request) {
        String origen = request.getHeader("Origin");
        try {
            if (origen == null && request.getHeader("Referer") != null) {
                URI referer = URI.create(request.getHeader("Referer"));
                origen = referer.getScheme() + "://" + referer.getRawAuthority();
            }
            if (origen == null || "null".equals(origen)) return false;
            URI propia = URI.create(request.getRequestURL().toString());
            if (origen.equals(propia.getScheme() + "://" + propia.getRawAuthority())) return true;
            String permitidos = System.getenv().getOrDefault("CHIVOMED_ORIGINS", "http://127.0.0.1:3000,http://localhost:3000");
            String recibido = origen;
            return Arrays.stream(permitidos.split(",")).map(String::strip).anyMatch(recibido::equals);
        } catch (IllegalArgumentException e) { return false; }
    }

    private void error(HttpServletRequest request, HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(Json.createObjectBuilder().add("status", status).add("code", code).add("message", message)
                .add("path", request.getRequestURI()).add("timestamp", java.time.Instant.now().toString())
                .add("details", Json.createArrayBuilder()).build().toString());
    }
}
