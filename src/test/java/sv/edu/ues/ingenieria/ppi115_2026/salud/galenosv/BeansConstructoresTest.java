package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ejb.MessageDriven;
import jakarta.ejb.Singleton;
import jakarta.ejb.Stateful;
import jakarta.ejb.Stateless;
import jakarta.enterprise.context.NormalScope;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * El contenedor (Open Liberty) crea los EJB y los proxies de los beans CDI con alcance normal sin
 * argumentos. Si una de estas clases pierde su constructor sin parámetros, la compilación y las
 * pruebas unitarias (que usan {@code new} o Mockito) pasan, pero el WAR no despliega
 * (CNTR5007E / CWWKZ0002E). Esta prueba recorre las clases de producción por reflexión para
 * detectarlo antes de desplegar.
 */
class BeansConstructoresTest {
    private static final List<Class<? extends Annotation>> EJB =
            List.of(Stateless.class, Stateful.class, Singleton.class, MessageDriven.class);

    private static List<Class<?>> clases;

    @BeforeAll
    static void cargarClasesDeProduccion() throws IOException, URISyntaxException {
        // target/classes: donde está compilada una clase de producción conocida.
        Path raiz = Path.of(SchemaCheck.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        ClassLoader cargador = BeansConstructoresTest.class.getClassLoader();
        clases = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(raiz)) {
            for (Path archivo : (Iterable<Path>) archivos.filter(p -> p.toString().endsWith(".class"))::iterator) {
                String nombre = raiz.relativize(archivo).toString()
                        .replace(archivo.getFileSystem().getSeparator(), ".")
                        .replaceAll("\\.class$", "");
                try {
                    clases.add(Class.forName(nombre, false, cargador));
                } catch (ClassNotFoundException | LinkageError e) {
                    throw new AssertionError("No se pudo cargar " + nombre, e);
                }
            }
        }
    }

    @Test
    void cadaEjbTieneConstructorPublicoSinParametros() {
        List<Class<?>> ejbs = clases.stream().filter(BeansConstructoresTest::esEjb).toList();
        assertTrue(ejbs.contains(sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.TicketCitaService.class),
                "El recorrido no encontró los EJB de producción: " + ejbs);

        List<String> fallas = ejbs.stream()
                .filter(c -> constructorSinParametros(c) == null
                        || !Modifier.isPublic(constructorSinParametros(c).getModifiers()))
                .map(Class::getName)
                .toList();
        assertTrue(fallas.isEmpty(), "EJB sin constructor público sin parámetros: " + fallas);
    }

    @Test
    void cadaBeanCdiConAlcanceNormalTieneConstructorNoPrivadoSinParametros() {
        List<Class<?>> beans = clases.stream().filter(BeansConstructoresTest::tieneAlcanceNormal).toList();
        assertTrue(beans.stream().anyMatch(c -> c.getSimpleName().equals("PublicoResource")),
                "El recorrido no encontró los beans CDI de producción: " + beans);

        List<String> fallas = beans.stream()
                .filter(c -> constructorSinParametros(c) == null
                        || Modifier.isPrivate(constructorSinParametros(c).getModifiers()))
                .map(Class::getName)
                .toList();
        assertTrue(fallas.isEmpty(), "Beans CDI con alcance sin constructor no privado sin parámetros: " + fallas);
    }

    private static boolean esEjb(Class<?> c) {
        return EJB.stream().anyMatch(c::isAnnotationPresent);
    }

    /** @RequestScoped, @SessionScoped, @ApplicationScoped, @ViewScoped… (todas llevan @NormalScope). */
    private static boolean tieneAlcanceNormal(Class<?> c) {
        for (Annotation a : c.getDeclaredAnnotations()) {
            if (a.annotationType().isAnnotationPresent(NormalScope.class)) return true;
        }
        return false;
    }

    private static Constructor<?> constructorSinParametros(Class<?> c) {
        try {
            return c.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
