import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * AnalitzadorPrincipal: el PROCESO PADRE.
 *
 * 1. Lanza dos procesos hijo (FiltreLog) que funcionan a la vez:
 *      - Hijo A busca la palabra ERROR
 *      - Hijo B busca la palabra WARNING
 * 2. Envía el mismo texto a los dos hijos al mismo tiempo.
 * 3. Lee la respuesta de cada hijo y comprueba cómo ha terminado.
 * 4. Imprime como última línea:  RESULTAT: ERRORS=3 | WARNINGS=3 | EXIT_CODE=0
 */
public class AnalitzadorPrincipal {

    // Fichero donde se guardan los mensajes de error (System.err) de los hijos
    static final String FICHERO_ERRORES = "errors_filtre.log";

    // Texto de prueba: 3 líneas con ERROR y 3 líneas con WARNING
    static final String TEXTO_PRUEBA =
            "2026-11-12 08:00:01 INFO    Server web-01 started\n"
          + "2026-11-12 08:00:05 WARNING CPU usage above 80%\n"
          + "2026-11-12 08:01:12 ERROR   Database connection refused\n"
          + "2026-11-12 08:01:30 INFO    User admin logged in\n"
          + "2026-11-12 08:02:44 WARNING SSL certificate expires in 7 days\n"
          + "2026-11-12 08:03:10 INFO    Cache cleared on db-02\n"
          + "2026-11-12 08:04:02 ERROR   Timeout reading /var/log/auth.log\n"
          + "2026-11-12 08:05:17 INFO    Backup completed\n"
          + "2026-11-12 08:06:23 WARNING Disk /dev/sda1 at 90%\n"
          + "2026-11-12 08:07:45 ERROR   Unauthorized access attempt from 10.0.0.66\n"
          + "2026-11-12 08:08:00 INFO    Health check passed\n"
          + "2026-11-12 08:09:12 INFO    Scheduled task finished\n";

    public static void main(String[] args) throws IOException, InterruptedException {

        // 1) Lanzar los DOS hijos. A partir de aquí ya se están ejecutando a la vez.
        Process hijoA = crearHijo("ERROR");
        Process hijoB = crearHijo("WARNING");

        // 2) Enviar el mismo texto a los dos hijos A LA VEZ: cada envío va en su propio hilo.
        //    Así el padre no se queda bloqueado escribiendo y puede ponerse a leer enseguida.
        Thread envioA = new Thread(() -> enviarTexto(hijoA, TEXTO_PRUEBA));
        Thread envioB = new Thread(() -> enviarTexto(hijoB, TEXTO_PRUEBA));
        envioA.start();
        envioB.start();

        // 3) Mientras tanto, leer la respuesta de cada hijo y mostrarla por pantalla
        String respuestaA = leerRespuesta(hijoA);
        String respuestaB = leerRespuesta(hijoB);
        System.out.println("Hijo A (FiltreLog ERROR)   -> respuesta: " + respuestaA);
        System.out.println("Hijo B (FiltreLog WARNING) -> respuesta: " + respuestaB);

        // 4) Esperar a que todo termine y recoger el código de salida de cada hijo
        envioA.join();     // esperar a que acaben los dos hilos de envío
        envioB.join();
        hijoA.waitFor();   // esperar a que acaben los dos hijos
        hijoB.waitFor();
        int codigoA = hijoA.exitValue();   // 0 = todo bien, 1 = error (texto vacío)
        int codigoB = hijoB.exitValue();
        mostrarEstado("Hijo A (ERROR)", codigoA);
        mostrarEstado("Hijo B (WARNING)", codigoB);

        // 5) Resultado final: si los dos hijos han ido bien, el código final es 0
        int errores = convertirANumero(respuestaA);
        int warnings = convertirANumero(respuestaB);
        int codigoFinal = 0;
        if (codigoA != 0 || codigoB != 0) {
            codigoFinal = 1;
        }
        // Formato exacto que pide el enunciado (lo comprueba el test): SIEMPRE la última línea
        System.out.println("RESULTAT: ERRORS=" + errores + " | WARNINGS=" + warnings
                + " | EXIT_CODE=" + codigoFinal);
    }

    // Crea y arranca un hijo. Es como escribir en la terminal: java -cp <classpath> FiltreLog <palabra>
    static Process crearHijo(String palabraClave) throws IOException {
        String classpath = System.getProperty("java.class.path");   // carpeta donde están los .class
        ProcessBuilder pb = new ProcessBuilder("java", "-cp", classpath, "FiltreLog", palabraClave);
        pb.redirectError(new File(FICHERO_ERRORES));   // NIVEL 2: System.err del hijo -> errors_filtre.log
        return pb.start();
    }

    // Escribe el texto en la entrada del hijo. Lo que el padre escribe en este OutputStream,
    // el hijo lo lee por su System.in.
    // Lleva try/catch porque se ejecuta dentro de un hilo, y un hilo no puede lanzar IOException.
    static void enviarTexto(Process hijo, String texto) {
        try {
            OutputStream out = hijo.getOutputStream();
            out.write(texto.getBytes());
            out.flush();   // vaciar el buffer: obliga a que los datos lleguen YA al hijo
            out.close();   // cerrar = "fin de datos": el readLine() del hijo devuelve null y acaba
        } catch (IOException e) {
            System.err.println("No se ha podido enviar el texto al hijo: " + e.getMessage());
        }
    }

    // Lee lo que el hijo escribe por su System.out. Para el padre es un InputStream.
    static String leerRespuesta(Process hijo) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(hijo.getInputStream()));
        String respuesta = "";
        String linea;
        while ((linea = in.readLine()) != null) {   // null = el hijo ha terminado de escribir
            respuesta = respuesta + linea;
        }
        in.close();
        return respuesta.trim();
    }

    // NIVEL 2: imprime cómo ha terminado un hijo según su código de salida
    static void mostrarEstado(String nombreHijo, int codigo) {
        if (codigo == 0) {
            System.out.println("Estado " + nombreHijo + ": OK (codigo de salida 0)");
        } else {
            System.out.println("Estado " + nombreHijo + ": ERROR (codigo de salida " + codigo
                    + ") -> mira el fichero " + FICHERO_ERRORES);
        }
    }

    // Pasa la respuesta del hijo a número. Si el hijo falló y no respondió nada, devuelve 0.
    static int convertirANumero(String respuesta) {
        if (respuesta.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(respuesta);
    }
}
