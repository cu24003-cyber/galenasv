package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** Activa Jakarta REST y el descubrimiento de recursos y proveedores en el WAR. */
@ApplicationPath("api")
public class AppConfig extends Application {
}
