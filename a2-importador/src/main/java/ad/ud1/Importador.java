package ad.ud1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Importador de clientes_importacion.csv (CE1.6, CE1.2).
 *
 * Lee el CSV línea a línea con codificación declarada, separa válidas de
 * inválidas sin detener la importación por una fila suelta, y escribe
 * salida/clientes_ok.csv y salida/rechazados.csv (línea, contenido,
 * motivo). Al terminar imprime un resumen agrupado por motivo.
 *
 * Ejecución: mvn -q compile exec:java (o -Dexec.args="otra_ruta.csv").
 *
 * Observaciones del fichero (paso 2, con file/wc/head/grep antes de
 * programar): separador real es coma, no ";"; cada línea trae una coma de
 * más al final (quinto campo vacío a descartar); CRLF, readLine() ya lo
 * gestiona solo; UTF-8 real (confirmado con file); desde la línea 142 el
 * texto sale doble-codificado (XoÃ¡n en vez de Xoán) por una exportación
 * posterior, no se corrige aquí porque es la ampliación opcional; la
 * línea 142 es un comentario del exportador, no un cliente.
 *
 * Criterios de validez (paso 4, ver validar()), en este orden -solo se
 * anota el primero que falla-: 4 campos, DNI con 8 cifras y una letra,
 * nombre no vacío, CP de 5 cifras, teléfono numérico (+34 y espacios
 * opcionales), DNI no repetido. Los dos últimos se añaden sobre los
 * cuatro del enunciado por casos reales del fichero que ninguno de esos
 * cuatro detecta ("no disponible" como teléfono; el DNI 10000444K
 * repetido con datos distintos).
 *
 * El 75% de los rechazos es por CP de 4 cifras en vez de 5 (ej. 8597 en
 * vez de 08597): patrón demasiado repetido para ser casualidad, probable
 * pérdida del cero inicial en el sistema de origen. No se repara a
 * ciegas -el enunciado exige 5 cifras y adivinar el dato real es más
 * arriesgado que marcarlo- así que se rechaza y queda en rechazados.csv.
 *
 * Dos límites conocidos: la línea 39 ("Talleres Miño, S.L.") trae una
 * coma dentro de comillas que un split ingenuo no respeta, se rechaza
 * por "campos de más" (parser CSV real fuera de alcance aquí); la línea
 * 48 sí se importa pero conserva las comillas literales en el nombre.
 *
 * Resumen real: 196 leídas, 158 importadas, 37 rechazadas (CP 28, DNI
 * mal formado 3, campos de más 2, nombre vacío 1, teléfono inválido 1,
 * duplicado 1, comentario del exportador 1).
 *
 * Forma de acceso (CE1.2): secuencial, con BufferedReader.readLine(),
 * sin volver nunca atrás. Es lo adecuado porque importar exige mirar
 * todas las filas sin excepción -no hay ninguna a la que convenga "ir
 * directo"-, así que el acceso aleatorio no aporta nada y el coste es
 * O(n) de cualquier forma. Con acceso aleatorio (un índice por DNI)
 * ganaríamos poder consultar o actualizar un cliente suelto sin leer el
 * resto, pero perderíamos justo lo que aquí sobra: simplicidad y campos
 * de longitud variable, que el acceso por posición de byte no soporta
 * bien. Si en vez de importar una vez al mes hubiera que tocar clientes
 * sueltos todo el día, recorrer el CSV entero cada vez sería absurdo: ahí
 * lo natural ya no es un fichero de texto, sino una base de datos con
 * índice por DNI.
 */
public class Importador {

    // registro que modela un cliente ya validado (los cuatro campos que exige el enunciado)
    record Cliente(String dni, String nombre, String telefono, String codigoPostal) {
    }

    public static void main(String[] args) {
        Path entrada = Path.of(args.length > 0 ? args[0] : "clientes_importacion.csv");
        Path carpetaSalida = Path.of("salida");
        Path ficheroOk = carpetaSalida.resolve("clientes_ok.csv");
        Path ficheroRechazados = carpetaSalida.resolve("rechazados.csv");

        if (!Files.exists(entrada)) {
            System.err.println("ERROR: no existe el fichero de entrada " + entrada.toAbsolutePath());
            System.exit(1);
            return;
        }

        try {
            Files.createDirectories(carpetaSalida);
        } catch (IOException e) {
            System.err.println("ERROR: no se pudo crear la carpeta " + carpetaSalida.toAbsolutePath()
                    + ": " + e.getMessage());
            System.exit(1);
            return;
        }

        int leidas = 0;
        int importadas = 0;
        // LinkedHashMap para que el resumen salga en el orden en que se encontró cada motivo por primera vez
        Map<String, Integer> motivos = new LinkedHashMap<>();
        // DNIs ya importados, para detectar clientes repetidos en el propio fichero
        Set<String> dnisImportados = new HashSet<>();

        // los tres recursos (un lector, dos escritores) en el mismo try: si
        // falla a mitad, los tres se cierran igual; charset UTF_8 explícito
        // en los tres, nunca el de la plataforma
        try (
                BufferedReader in = Files.newBufferedReader(entrada, StandardCharsets.UTF_8);
                BufferedWriter ok = Files.newBufferedWriter(ficheroOk, StandardCharsets.UTF_8);
                BufferedWriter rechazados = Files.newBufferedWriter(ficheroRechazados, StandardCharsets.UTF_8)
        ) {
            ok.write("DNI;NOMBRE;TELEFONO;CP");
            ok.newLine();
            rechazados.write("LINEA;CONTENIDO;MOTIVO");
            rechazados.newLine();

            String linea;
            while ((linea = in.readLine()) != null) {
                leidas++;

                if (leidas == 1 && linea.startsWith("DNI,")) {
                    continue; // cabecera: no es un cliente, no cuenta ni como importada ni como rechazada
                }

                Optional<String> motivo = validar(linea, dnisImportados);
                if (motivo.isPresent()) {
                    motivos.merge(motivo.get(), 1, Integer::sum);
                    escribirRechazo(rechazados, leidas, linea, motivo.get());
                    continue;
                }

                escribirCliente(ok, aCliente(linea));
                importadas++;
            }
        } catch (NoSuchFileException e) {
            System.err.println("ERROR: no se encuentra el fichero " + e.getFile());
            System.exit(1);
            return;
        } catch (AccessDeniedException e) {
            System.err.println("ERROR: sin permisos para acceder a " + e.getFile());
            System.exit(1);
            return;
        } catch (IOException e) {
            System.err.println("ERROR: fallo de E/S leyendo o escribiendo los ficheros: " + e.getMessage());
            System.exit(1);
            return;
        }

        imprimirResumen(leidas, importadas, motivos);
    }

    /**
     * Comprueba si una línea es un cliente válido.
     * Devuelve el motivo del rechazo, o Optional.empty() si la fila es válida.
     * Si hay varios problemas a la vez en la misma fila, se informa solo del
     * primero que se encuentra (el orden de las comprobaciones es una decisión
     * de diseño documentada en la cabecera de la clase).
     */
    static Optional<String> validar(String linea, Set<String> dnisImportados) {
        if (linea.isBlank()) {
            return Optional.of("línea vacía");
        }
        if (linea.strip().startsWith("#")) {
            return Optional.of("línea de comentario del exportador, no es un cliente");
        }

        List<String> campos = camposDeLaLinea(linea);
        if (campos.size() != 4) {
            return Optional.of("esperaba 4 campos, hay " + campos.size());
        }

        String dni = campos.get(0).strip();
        String nombre = campos.get(1).strip();
        String telefono = campos.get(2).strip();
        String codigoPostal = campos.get(3).strip();

        if (!dni.matches("\\d{8}[A-Za-z]")) {
            return Optional.of("DNI con formato incorrecto");
        }
        if (nombre.isEmpty()) {
            return Optional.of("nombre vacío");
        }
        if (!codigoPostal.matches("\\d{5}")) {
            return Optional.of("código postal no son 5 cifras");
        }
        if (!telefono.replace(" ", "").matches("(\\+34)?\\d{9}")) {
            return Optional.of("teléfono no es un número válido");
        }
        if (!dnisImportados.add(dni)) {
            return Optional.of("DNI duplicado");
        }

        return Optional.empty();
    }

    /**
     * Separa una línea en sus campos. El fichero real usa coma como separador
     * (no punto y coma) y además cada línea termina con una coma de más, así
     * que el último campo que deja split siempre es una cadena vacía de
     * sobra: se descarta aquí, antes de validar nada.
     */
    static List<String> camposDeLaLinea(String linea) {
        List<String> campos = new ArrayList<>(Arrays.asList(linea.split(",", -1)));
        if (!campos.isEmpty() && campos.get(campos.size() - 1).isEmpty()) {
            campos.remove(campos.size() - 1);
        }
        return campos;
    }

    // se asume que la línea ya ha pasado validar(): aquí solo se reconstruye el Cliente
    static Cliente aCliente(String linea) {
        List<String> campos = camposDeLaLinea(linea);
        return new Cliente(
                campos.get(0).strip(),
                campos.get(1).strip(),
                campos.get(2).strip().replace(" ", ""),
                campos.get(3).strip());
    }

    static void escribirCliente(BufferedWriter ok, Cliente c) throws IOException {
        ok.write(c.dni() + ";" + c.nombre() + ";" + c.telefono() + ";" + c.codigoPostal());
        ok.newLine();
    }

    static void escribirRechazo(BufferedWriter rechazados, int numeroLinea, String contenido, String motivo)
            throws IOException {
        // el contenido original puede traer ; o " dentro: se entrecomilla y se
        // duplican las comillas internas para no romper el CSV de salida
        String contenidoEscapado = "\"" + contenido.replace("\"", "\"\"") + "\"";
        rechazados.write(numeroLinea + ";" + contenidoEscapado + ";" + motivo);
        rechazados.newLine();
    }

    static void imprimirResumen(int leidas, int importadas, Map<String, Integer> motivos) {
        int rechazadas = motivos.values().stream().mapToInt(Integer::intValue).sum();

        System.out.println("Líneas leídas: " + leidas);
        System.out.println("Importadas: " + importadas);
        System.out.println("Rechazadas: " + rechazadas);
        System.out.println();
        System.out.println("Rechazos por motivo:");
        motivos.forEach((motivo, veces) -> System.out.printf("  %-55s %d%n", motivo, veces));
    }
}
