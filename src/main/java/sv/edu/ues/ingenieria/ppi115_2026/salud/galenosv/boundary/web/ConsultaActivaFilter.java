package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.web;

import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AtencionSesion;
@WebFilter(urlPatterns={"/paginas/*"})
public class ConsultaActivaFilter implements Filter {
    @Inject private AtencionSesion sesion;
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req=(HttpServletRequest)request; HttpServletResponse res=(HttpServletResponse)response;
        String path=req.getServletPath();
        if(sesion.getConsulta()!=null && !path.equals("/paginas/consulta.xhtml") && !path.equals("/paginas/consulta.jsf")
                && !path.equals("/paginas/cambiar-rol.xhtml") && !path.equals("/paginas/cambiar-rol.jsf")) {
            String destino=req.getContextPath()+"/paginas/consulta.xhtml";
            if("partial/ajax".equals(req.getHeader("Faces-Request"))) { res.setContentType("text/xml;charset=UTF-8"); res.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><partial-response><redirect url=\""+destino+"\"/></partial-response>"); }
            else res.sendRedirect(destino);
            return;
        }
        chain.doFilter(request,response);
    }
}
