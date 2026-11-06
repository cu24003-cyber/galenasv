package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service;

import com.password4j.Argon2Function;
import com.password4j.types.Argon2;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Pattern;

@ApplicationScoped
public class ContrasenaService {
    private static final Argon2Function ARGON2 = Argon2Function.getInstance(19456, 2, 1, 32, Argon2.ID, 19);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DUMMY = generar("No corresponde a una cuenta de usuario");
    private static final Pattern PHC = Pattern.compile("\\$argon2id\\$v=19\\$m=([0-9]{1,6}),t=([0-9]{1,2}),p=([0-9]{1,2})\\$([A-Za-z0-9+/]{22,86})\\$([A-Za-z0-9+/]{43,86})");

    public static String generar(CharSequence password) {
        if (password == null || password.length() < 12 || password.length() > 1024 || password.toString().isBlank()) {
            throw new IllegalArgumentException("La contraseña debe tener entre 12 y 1024 caracteres.");
        }
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return ARGON2.hash(password.toString().getBytes(StandardCharsets.UTF_8), salt).getResult();
    }

    public boolean verificar(String password, String hash) {
        if (password == null || password.length() > 1024 || hash == null || hash.length() > 256) return false;
        try {
            // Validar antes de construir Argon2: la biblioteca reserva memoria al crear la función.
            var formato = PHC.matcher(hash);
            if (!formato.matches()) return false;
            int memoria = Integer.parseInt(formato.group(1));
            int iteraciones = Integer.parseInt(formato.group(2));
            int paralelismo = Integer.parseInt(formato.group(3));
            int sal = Base64.getDecoder().decode(formato.group(4)).length;
            int longitud = Base64.getDecoder().decode(formato.group(5)).length;
            if (memoria < 19456 || memoria > 131072 || iteraciones < 2 || iteraciones > 6
                    || paralelismo < 1 || paralelismo > 4 || sal < 16 || sal > 64 || longitud < 32 || longitud > 64) return false;
            return Argon2Function.getInstanceFromHash(hash).check(password, hash);
        } catch (RuntimeException e) { return false; }
    }

    public String hashAusente() { return DUMMY; }

    /** Herramienta local: genera un hash sin enviar la contraseña como argumento. */
    public static void main(String[] args) throws Exception {
        char[] password;
        if (args.length == 1 && "--stdin".equals(args[0])) {
            String linea = new java.io.BufferedReader(new java.io.InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
            password = linea == null ? new char[0] : linea.toCharArray();
        } else {
            var console = System.console();
            if (console == null) throw new IllegalStateException("Ejecute desde una terminal interactiva.");
            password = console.readPassword("Contraseña nueva (mínimo 12 caracteres): ");
        }
        try { System.out.println(generar(new String(password))); }
        finally { Arrays.fill(password, '\0'); }
    }
}
