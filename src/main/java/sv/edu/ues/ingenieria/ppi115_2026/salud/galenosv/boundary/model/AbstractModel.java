package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.AbstractService;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException;

/**
 * Clase base para los Managed Beans (Model) de la capa de presentación JSF.
 * Centraliza el ciclo CRUD estándar; delega la lógica de negocio a AbstractService.
 *
 * @param <T>  Tipo de la Entidad
 * @param <ID> Tipo del identificador de la Entidad
 */
public abstract class AbstractModel<T, ID> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Cancelar se ejecuta en Apply Request Values: lee los campos enviados sin convertir ni validar. */
    public static boolean formularioVacio() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null || context.getExternalContext() == null || context.getViewRoot() == null) return true;
        String source = context.getExternalContext().getRequestParameterMap().get("jakarta.faces.source");
        UIComponent boton = source == null ? UIComponent.getCurrentComponent(context)
                : context.getViewRoot().findComponent(":" + source);
        for (UIComponent editor = boton; editor != null; editor = editor.getParent()) {
            Object clase = editor.getAttributes().get("styleClass");
            if ((clase != null && clase.toString().contains("galeno-formulario"))
                    || editor instanceof jakarta.faces.component.UIForm) {
                return !contieneDatos(editor, context);
            }
        }
        return true;
    }

    private static boolean contieneDatos(UIComponent componente, FacesContext context) {
        if (!componente.isRendered()) return false;
        if (componente instanceof jakarta.faces.component.UIInput campo && !Boolean.TRUE.equals(campo.getAttributes().get("disabled"))) {
            Object enviado = campo.getSubmittedValue();
            Object valor = enviado == null ? campo.getValue() : enviado;
            if (campo instanceof jakarta.faces.component.UISelectBoolean) {
                if (enviado != null && !java.util.Objects.equals(enviado, campo.getValue())) return true;
            } else {
                // AutoComplete envía por separado el texto visible y el identificador seleccionado.
                String texto = context.getExternalContext().getRequestParameterMap().get(campo.getClientId(context) + "_input");
                if (texto != null && !texto.isBlank()) return true;
                if (valor instanceof String textoValor) {
                    if (campo.getClass().getSimpleName().equals("InputMask")) textoValor = textoValor.replaceAll("[_\\s\\-/().]", "");
                    if (!textoValor.isBlank()) return true;
                } else if (valor instanceof Object[] valores) {
                    if (java.util.Arrays.stream(valores).anyMatch(v -> v != null && !v.toString().isBlank())) return true;
                } else if (valor instanceof java.util.Collection<?> valores) {
                    if (!valores.isEmpty()) return true;
                } else if (valor != null) return true;
            }
        }
        java.util.Iterator<UIComponent> hijos = componente.getFacetsAndChildren();
        while (hijos.hasNext()) if (contieneDatos(hijos.next(), context)) return true;
        return false;
    }

    protected T seleccionada;
    protected List<T> registros = new ArrayList<>();

    protected abstract AbstractService<T, ID> getService();
    protected abstract T nuevaInstancia();
    protected abstract ID obtenerId(T entidad);

    @PostConstruct
    public void init() {
        cargarRegistros();
    }

    protected void cargarRegistros() {
        try {
            registros = getService().listarTodos();
        } catch (ServiceException | jakarta.ejb.EJBException e) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR,
                    Mensajes.texto("error.general"), Mensajes.detalle(e));
        }
    }

    public void nuevo() {
        seleccionada = nuevaInstancia();
    }

    public void editar(T entidad) {
        seleccionada = entidad;
    }

    public void guardar() {
        if (seleccionada == null) {
            marcarValidacionFallida();
            agregarMensaje(FacesMessage.SEVERITY_WARN, "Sin selección",
                    "Selecciona un registro o crea uno nuevo.");
            return;
        }
        try {
            if (obtenerId(seleccionada) == null) {
                getService().crear(seleccionada);
            } else {
                getService().actualizar(seleccionada);
            }
            cargarRegistros();
            seleccionada = null;
        } catch (ServiceException e) {
            marcarValidacionFallida();
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Error al guardar", e.getMessage());
        }
    }

    public void eliminar(T entidad) {
        try {
            getService().eliminar(obtenerId(entidad));
            cargarRegistros();
            if (seleccionada != null && java.util.Objects.equals(obtenerId(seleccionada), obtenerId(entidad))) seleccionada = null;
        } catch (ServiceException | jakarta.ejb.EJBException e) {
            marcarValidacionFallida();
            agregarMensaje(FacesMessage.SEVERITY_ERROR, Mensajes.texto("error.eliminar"), Mensajes.detalle(e));
        }
    }

    protected void marcarValidacionFallida() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ctx.validationFailed();
        }
    }

    protected void agregarMensaje(FacesMessage.Severity severidad, String resumen, String detalle) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ctx.addMessage(null, new FacesMessage(severidad, resumen, detalle));
        }
    }

    public T getSeleccionada() {
        return seleccionada;
    }

    public void setSeleccionada(T seleccionada) {
        this.seleccionada = seleccionada;
    }

    public List<T> getRegistros() {
        return registros;
    }

    /** Acceso al mismo bundle y locale utilizados por las vistas. */
    public static final class Mensajes {
        private Mensajes() { }

        public static String texto(String clave) {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null && context.getApplication() != null) {
                return context.getApplication().getResourceBundle(context, "msg").getString(clave);
            }
            return java.util.ResourceBundle.getBundle("i18n.Messages", java.util.Locale.forLanguageTag("es")).getString(clave);
        }

        public static String mensaje(ServiceException exception) {
            if ("error.general".equals(exception.getMessageKey())) return exception.getMessage();
            return java.text.MessageFormat.format(texto(exception.getMessageKey()), exception.getMessageArguments());
        }

        public static String detalle(RuntimeException exception) {
            java.util.logging.Logger.getLogger(Mensajes.class.getName())
                    .log(java.util.logging.Level.WARNING, "Error de servicio en la vista", exception);
            return texto(exception instanceof ServiceException service
                    ? service.getMessageKey() : "error.general");
        }
    }

    /** Convierte únicamente patrones fijos compatibles con las máscaras de PrimeFaces. */
    public static final class FormatoExpresion {
        private FormatoExpresion() { }

        public static boolean sinRestriccion(String expresion) {
            return expresion == null || expresion.isBlank() || ".".equals(expresion);
        }

        public static boolean coincide(String valor, String expresion) {
            return sinRestriccion(expresion) || Pattern.matches(expresion, valor.trim());
        }

        /** null mantiene el campo libre cuando la expresión no admite una máscara segura. */
        public static String mascara(String expresion) {
            if (sinRestriccion(expresion)) return null;
            try {
                Pattern.compile(expresion);
            } catch (PatternSyntaxException e) {
                return null;
            }
            String patron = expresion;
            if (patron.startsWith("^")) patron = patron.substring(1);
            // Solo quitar un ancla final, nunca un dólar escapado.
            if (patron.endsWith("$") && !patron.endsWith("\\$")) {
                patron = patron.substring(0, patron.length() - 1);
            }
            StringBuilder resultado = new StringBuilder();
            int posiciones = 0;
            for (int i = 0; i < patron.length();) {
                String token;
                boolean editable = true;
                if (patron.startsWith("[0-9]", i)) {
                    token = "9"; i += 5;
                } else if (patron.startsWith("\\d", i)) {
                    token = "9"; i += 2;
                } else if (patron.startsWith("[A-Za-z]", i) || patron.startsWith("[a-zA-Z]", i)) {
                    token = "a"; i += 8;
                } else if (patron.startsWith("[A-Z]", i)) {
                    token = "A"; i += 5;
                } else if (patron.startsWith("[A-Za-z0-9]", i) || patron.startsWith("[a-zA-Z0-9]", i)) {
                    token = "*"; i += 11;
                } else {
                    editable = false;
                    char literal = patron.charAt(i++);
                    if (literal == '\\') {
                        if (i == patron.length()) return null;
                        literal = patron.charAt(i++);
                        // No interpretar clases, escapes Unicode ni referencias como texto.
                        if (Character.isLetterOrDigit(literal)) return null;
                    } else if (".^$|?*+()[]{}".indexOf(literal) >= 0) {
                        return null;
                    }
                    // Estos caracteres tienen significado propio en Inputmask.
                    if ("[]{}()|?*+\\9aA&#".indexOf(literal) >= 0) return null;
                    token = String.valueOf(literal);
                }
                int repeticiones = 1;
                if (i < patron.length() && patron.charAt(i) == '{') {
                    int cierre = patron.indexOf('}', i);
                    if (cierre < 0) return null;
                    String cantidad = patron.substring(i + 1, cierre);
                    if (!cantidad.matches("[0-9]{1,3}")) return null;
                    repeticiones = Integer.parseInt(cantidad);
                    if (repeticiones < 1) return null;
                    i = cierre + 1;
                }
                if (resultado.length() + token.length() * repeticiones > 255) return null;
                resultado.append(token.repeat(repeticiones));
                if (editable) posiciones += repeticiones;
            }
            return posiciones == 0 ? null : resultado.toString();
        }
    }

    @FacesValidator("expresionRegularValidator")
    public static class ExpresionRegularValidator implements Validator<String> {
        @Override
        public void validate(FacesContext context, UIComponent component, String value) {
            if (FormatoExpresion.sinRestriccion(value)) return;
            try {
                Pattern.compile(value);
            } catch (PatternSyntaxException e) {
                throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        Mensajes.texto("registro.patronInvalido"), null));
            }
        }
    }
}
