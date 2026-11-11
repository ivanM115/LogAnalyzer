import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * FiltreLog: el PROCESO HIJO.
 *
 * Lee un texto línea a línea por su entrada estándar (System.in),
 * cuenta cuántas líneas contienen la palabra clave y envía ese número
 * por su salida estándar (System.out), que es lo que lee el padre.
 *
 * La palabra clave llega como argumento (args[0]). Si no llega ninguna, busca "ERROR".
 */
public class FiltreLog {

    public static void main(String[] args) throws IOException {

        // NIVEL 3: la palabra a buscar viene en args[0]. Si no hay argumento, se usa "ERROR".
        String palabraClave = "ERROR";
        if (args.length > 0) {
            palabraClave = args[0];
        }

        // NIVEL 1: leer el texto que manda el padre, línea a línea, desde System.in
        BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in));

        int contador = 0;          // cuántas líneas contienen la palabra clave
        boolean hayTexto = false;  // sirve para saber si el texto ha llegado vacío

        String linea;
        // readLine() devuelve null cuando el padre cierra el canal con out.close()
        while ((linea = entrada.readLine()) != null) {
            if (!linea.isBlank()) {
                hayTexto = true;
            }
            if (linea.contains(palabraClave)) {
                contador++;
            }
        }

        // NIVEL 2: texto vacío -> mensaje por System.err y salir con código 1
        if (!hayTexto) {
            System.err.println("Error: Text buit");   // mensaje exacto que pide el enunciado
            System.exit(1);
        }

        // NIVEL 1: mandar el resultado al padre en una sola línea por System.out
        System.out.println(contador);

        // NIVEL 2: todo correcto -> salir con código 0
        System.exit(0);
    }
}
