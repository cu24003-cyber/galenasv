package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 *
 * @author 
 */
@Path("jakartaee11")
@Produces(MediaType.APPLICATION_JSON)
public class JakartaEE11Resource {
    
    @GET
    public Response ping(){
        return Response
                .ok(Map.of("message", "ping Jakarta EE"))
                .build();
    }
}
