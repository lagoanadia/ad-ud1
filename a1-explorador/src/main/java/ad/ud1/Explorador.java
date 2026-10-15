package ad.ud1;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import java.time.Instant;

/**
 * Explorador de directorios por línea de comandos basado en {@code java.nio.file}.
 * <p>
 * Recorre la ruta indicada por argumentos, mostrando para cada elemento sus
 * permisos ({@code rwx}), tamaño en bytes, fecha de última modificación y
 * nombre. Por defecto solo lista un nivel; con el modificador {@code -r}
 * recorre el árbol completo de forma recursiva, con sangría creciente según
 * la profundidad.
 * <p>
 * Un fallo de acceso en un elemento concreto (por ejemplo,
 * {@link java.nio.file.AccessDeniedException}) se trata donde ocurre y no
 * detiene el recorrido: se cuenta en el resumen final y se sigue con el
 * resto de elementos.
 * <p>
 * FORMA DE EJECUCIÓN (las tres son equivalentes):
 * <ul>
 *   <li>Terminal, con el plugin:
 *       {@code mvn -q compile exec:java -Dexec.args="prueba -r"}
 *       (el plugin no se instala: está declarado en el {@code pom.xml} y
 *       Maven lo descarga solo la primera vez que se usa).</li>
 *   <li>Botón «Run» de VS Code, sin plugin: los argumentos van en el campo
 *       {@code args} de {@code .vscode/launch.json}.</li>
 *   <li>Terminal, sin Maven:
 *       {@code java src/main/java/ad/ud1/Explorador.java prueba -r}.</li>
 * </ul>
 * Si no se indica ninguna ruta, se explora el directorio actual.
 */
public class Explorador {
    private static final DateTimeFormatter FECHA = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static int ficheros, directorios, sinAcceso; // (8)
    private static long bytes; // (8)
    private static int codigo = 0;

    /**
     * Punto de entrada del programa.
     * <p>
     * Interpreta los argumentos de la línea de comandos (una ruta opcional y
     * el modificador {@code -r}), valida que la ruta exista, y lanza el
     * listado de un nivel o el recorrido recursivo según corresponda.
     * Termina imprimiendo un resumen con el número de ficheros, directorios,
     * bytes totales y elementos sin acceso.
     *
     * @param args argumentos de la línea de comandos: una ruta (si se omite,
     *             se usa el directorio actual) y, opcionalmente, {@code -r}
     *             para recorrer el árbol de forma recursiva. El orden entre
     *             ambos no importa.
     */
    public static void main(String[] args) {
        // ejecuta con java Explorador por la terminal !

        // TODO (2) leer ruta y opción -r; construir raiz con Path.of

        String txtRuta = ".";
        boolean recursivo = false;

        for (String a : args) // recorre cada objeto de los argumentos introducidos por terminal
        {
            if (a.equals("-r")) {
                recursivo = true;
                // flag por si encuentra "-r" por ejemplo: java Explorador -r
            } else {
                txtRuta = a;
                // guardamos todo lo que no sea "-r" como ruta
                System.out.println(a);
            }
        }
        Path root = Path.of(txtRuta).toAbsolutePath().normalize();
        // convertimos el string ya identificado como nuestra ruta en un objeto Path
        // toAbsolutePath completa la ruta desde la raiz del disco es decir añade C:\
        // el metodo normalize elimina . .. para obtener la ruta completa limpia
        System.out.println("Explorando " + root);

    // TODO (3) validar: no existe → exit 1; es fichero → una línea y fin

        if (!Files.exists(root)) {
            System.out.print("No existe");
            System.exit(1);
            // comprobamos si la ruta existe en el disco
        } else {
            // si existe en el disco comprobamos si es un directorio

            if (!Files.isDirectory(root)) {
                System.out.println(linea(root, root));

                return;
            }
        }
       // TODO (4)(6) try-with-resources con Files.list o Files.walk

        if (recursivo) {
            try (Stream<Path> hijos = Files.walk(root).sorted().skip(1)) {
                hijos.forEach(hijo -> System.out.println(linea(hijo, root)));
            } catch (NoSuchFileException e) { // la ruta desapareció
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (NotDirectoryException e) { // no se puede listar un fichero
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (AccessDeniedException e) { // sin permiso para la raíz
                System.err.println("ERROR: " + e.getMessage());
                sinAcceso++;
                codigo = 1;
            } catch (UncheckedIOException e) { // walk falló a mitad: e.getCause()
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (IOException e) { // cualquier otro fallo de E/S
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            }
        } else {
            try (Stream<Path> hijos = Files.list(root).sorted();)
            // try () se cierra solo.
            {
                hijos.forEach(hijo -> System.out.println(linea(hijo, root)));
                // bucle for each para un stream<path> !

            } catch (NoSuchFileException e) { // la ruta desapareció
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (NotDirectoryException e) { // no se puede listar un fichero
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (AccessDeniedException e) { // sin permiso para la raíz
                System.err.println("ERROR: " + e.getMessage());
                sinAcceso++;
                codigo = 1;
            } catch (UncheckedIOException e) { // walk falló a mitad: e.getCause()
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            } catch (IOException e) { // cualquier otro fallo de E/S
                System.err.println("ERROR: " + e.getMessage());
                codigo = 1;
            }

        }

        // TODO (7) catch de las excepciones, de la más concreta a la más general
        // TODO (8) imprimir el resumen y System.exit(codigo)
        System.out.printf("%n%d ficheros · %d directorios · %d bytes · %d sin acceso%n",
                ficheros, directorios, bytes, sinAcceso);
        System.exit(codigo);
    }

    /**
     * Construye la línea de salida de un elemento del árbol: permisos,
     * tamaño, fecha de última modificación y nombre, con sangría según la
     * profundidad respecto a {@code base}.
     * <p>
     * Incrementa los contadores estáticos del resumen final
     * ({@code ficheros}/{@code directorios}/{@code bytes}/{@code sinAcceso})
     * según corresponda. Si falla la lectura del tamaño o de la fecha, el
     * fallo se captura aquí mismo (no se propaga, porque este método se
     * invoca desde dentro de un {@code forEach}, que no admite excepciones
     * comprobadas) y se muestra el motivo en lugar del dato que no se pudo
     * leer.
     *
     * @param p    la ruta del elemento a describir.
     * @param base la raíz desde la que se está explorando, usada para
     *             calcular el nivel de profundidad y, por tanto, la sangría.
     * @return la línea formateada lista para imprimir.
     */
    static String linea(Path p, Path base) {
        // TODO (5) permisos rwx, tamaño y fecha
        String permisos = (Files.isReadable(p) ? "r" : "-") + (Files.isWritable(p) ? "w" : "-")
                + (Files.isExecutable(p) ? "x" : "-");
        boolean esDirectorio = Files.isDirectory(p);
        if (esDirectorio) {
            directorios++;
        } else {
            ficheros++;
        }
        long sizeLong;
        String sizeString = "";

        FileTime timeNoFormat;
        Instant aux;
        String time;

        try {
            sizeLong = Files.size(p);
            sizeString = Long.toString(sizeLong);
            bytes += sizeLong;
        } catch (NoSuchFileException e) { // la ruta desapareció
            sizeString = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (NotDirectoryException e) { // no se puede listar un fichero
            sizeString = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (AccessDeniedException e) { // sin permiso para la raíz
            sizeString = "ERROR: " + e.getMessage();
            sinAcceso++;
            codigo = 1;
        } catch (UncheckedIOException e) { // walk falló a mitad: e.getCause()
            sizeString = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (IOException e) { // cualquier otro fallo de E/S
            sizeString = "ERROR: " + e.getMessage();
            codigo = 1;
        }

        try {
            timeNoFormat = Files.getLastModifiedTime(p);
            aux = timeNoFormat.toInstant();
            time = FECHA.format(aux);
        } catch (NoSuchFileException e) { // la ruta desapareció
            time = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (NotDirectoryException e) { // no se puede listar un fichero
            time = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (AccessDeniedException e) { // sin permiso para la raíz
            time = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (UncheckedIOException e) { // walk falló a mitad: e.getCause()
            time = "ERROR: " + e.getMessage();
            codigo = 1;
        } catch (IOException e) { // cualquier otro fallo de E/S
            time = "ERROR: " + e.getMessage();
            codigo = 1;
        }

        // TODO (6) sangría según el nivel respecto a base
        int nivel = base.relativize(p).getNameCount();
        String sangria = " ".repeat(nivel - 1);

        // TODO (7) si falla la lectura de atributos, devolver la ruta con el motivo
        String nombre = p.getFileName() + (esDirectorio ? "/" : "");
        String datos = String.format("%s%-15s %5s  %s  %s", sangria, permisos, sizeString, time, nombre);
        return datos;
    }
}

// ---------------------------------------------------------------------------
// PASO 9 · las cuatro pruebas
//
// Carpeta de prueba, creada así:
//   mkdir -p prueba/docs/sub prueba/secreta
//   echo hola > prueba/a.txt
//   echo "más texto" > prueba/docs/b.md
//   echo x > prueba/docs/sub/c.txt
//   chmod 000 prueba/secreta
//
// 1) Un nivel (sin -r):
//   $ mvn -q compile exec:java -Dexec.args="prueba"
//   prueba
//   Explorando /home/user/ADUD1/a1-explorador/prueba
//   rw-                 5  2026-10-13 12:24  a.txt
//   rwx              4096  2026-10-13 12:24  docs/
//   rwx              4096  2026-10-13 12:24  secreta/
//
//   1 ficheros · 2 directorios · 8197 bytes · 0 sin acceso
//
// 2) Recursivo (-r):
//   $ mvn -q compile exec:java -Dexec.args="prueba -r"
//   prueba
//   Explorando /home/user/ADUD1/a1-explorador/prueba
//   rw-                 5  2026-10-13 12:24  a.txt
//   rwx              4096  2026-10-13 12:24  docs/
//    rw-                11  2026-10-13 12:24  b.md
//    rwx              4096  2026-10-13 12:24  sub/
//     rw-                2  2026-10-13 12:24  c.txt
//   rwx              4096  2026-10-13 12:24  secreta/
//
//   3 ficheros · 3 directorios · 12306 bytes · 0 sin acceso
//
// 3) Ruta inexistente:
//   $ mvn -q compile exec:java -Dexec.args="carpeta-que-no-existe"
//   carpeta-que-no-existe
//   Explorando /home/user/ADUD1/a1-explorador/carpeta-que-no-existe
//   No existe
//   (código de salida 1, confirmado con echo $?, sin intentar recorrer nada)
//
// 4) Un fichero en vez de un directorio:
//   $ mvn -q compile exec:java -Dexec.args="prueba/a.txt"
//   prueba/a.txt
//   Explorando /home/user/ADUD1/a1-explorador/prueba/a.txt
//   rw-                 5  2026-10-13 12:24  a.txt
//   (solo esa línea, sin intentar listar nada más)
//
// Sobre los tamaños de "docs/" y "secreta/" (4096 bytes): en Linux,
// Files.size() sobre un directorio devuelve el tamaño del bloque que ocupa
// en el disco (ext4), no 0 como en Windows/NTFS; por eso el recuento de
// bytes del resumen incluye ese tamaño de bloque de cada carpeta, además
// del contenido real de los ficheros.
//
// Sobre "secreta" (chmod 000): en este contenedor, ejecutando como usuario
// con privilegios elevados, el chmod 000 no llega a bloquear el acceso
// real, así que "secreta" se sigue leyendo con normalidad (permisos "---"
// pero el directorio no cuenta como "sin acceso"). El código sí captura
// AccessDeniedException (incrementando sinAcceso) para cuando se ejecute
// donde el permiso sí se respete (por ejemplo, un servidor Linux real sin
// privilegios elevados, el escenario del contexto profesional del
// enunciado).
// ---------------------------------------------------------------------------
