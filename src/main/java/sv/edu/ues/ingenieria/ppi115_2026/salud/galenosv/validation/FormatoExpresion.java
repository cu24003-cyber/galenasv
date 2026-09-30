package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.validation;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Convierte únicamente patrones fijos compatibles con las máscaras de PrimeFaces. */
public final class FormatoExpresion {
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
