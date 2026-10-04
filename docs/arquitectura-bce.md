# Organización BCE y MVC con JSF

El paquete base es `sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv`:

```text
boundary/
  model/       Beans JSF, presentación, conversión y validación de campos
  web/         Filtro de navegación de la consulta activa
  resources/   Recursos HTTP
control/
  service/     Reglas de negocio, transacciones y sesión del flujo de atención
entity/
               Entidades JPA
  repository/  Acceso a datos y consultas de repositorios
```

En MVC, las entidades representan los datos del modelo; las páginas XHTML de `src/main/webapp/paginas` y las plantillas de `WEB-INF/templates` son las vistas. Los beans `*Model` de Boundary coordinan las acciones de las vistas y delegan el negocio a los servicios de Control. Se mantienen los nombres de los beans EL y las rutas públicas de las páginas.

Los conversores están como clases estáticas dentro del Model de su catálogo. Los validadores de valores de documentos/contactos y del rango de consultas están dentro de `RegistroPersonaModel` e `HistorialConsultaModel`. Las funciones compartidas de mensajes, expresiones y cancelación pertenecen a `AbstractModel`. JSF descubre los validadores y conversores por sus anotaciones; las referencias explícitas de `faces-config.xml` usan el nombre binario de las clases internas (`Model$Converter`).

La cancelación utiliza `process="@form" immediate="true"`: JSF decodifica los campos y ejecuta la acción antes de convertir, validar o actualizar el modelo. `AbstractModel.formularioVacio()` inspecciona los valores enviados del editor correspondiente. Un formulario con datos se limpia; uno vacío se oculta. No depende de un archivo JavaScript propio ni de un indicador calculado en el navegador.

`AtencionSesion` es un bean de Control porque conserva el contexto del flujo de atención que utilizan los servicios y el filtro. Las entidades JPA, servicios EJB, repositorios y excepciones de negocio mantienen clases propias porque tienen identidad, ciclo de vida o contratos independientes.

Después de cambiar paquetes, ejecutar `mvn clean package` con JDK 21 para evitar clases antiguas en el WAR. La unidad de persistencia y los conversores explícitos apuntan a los paquetes BCE. `refactor_script.sh` comprueba las referencias; los generadores crean solamente archivos faltantes y respetan los existentes.
