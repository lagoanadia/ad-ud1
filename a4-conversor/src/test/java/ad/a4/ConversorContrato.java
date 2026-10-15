package ad.a4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas comunes. Cada subclase las ejecuta contra una versión del conversor.
 * Con la original fallan; con la corregida pasan.
 */
abstract class ConversorContrato {

    @TempDir
    Path dir;

    /** Ejecuta la conversión. Debe lanzar excepción si la entrada no se puede convertir. */
    abstract void convertir(Path xml, Path csv, Path json) throws Exception;

    Path recurso(String nombre) throws Exception {
        Path destino = dir.resolve(nombre);
        Files.copy(Path.of("src/test/resources", nombre), destino);
        return destino;
    }

    @Test
    @DisplayName("F1 · el JSON generado es JSON válido")
    void jsonValido() throws Exception {
        Path csv = dir.resolve("p.csv"), json = dir.resolve("p.json");
        convertir(recurso("pedidos.xml"), csv, json);
        JsonNode raiz = new ObjectMapper().readTree(json.toFile());
        assertEquals(3, raiz.size());
        assertEquals(3, raiz.get(2).get("lineas").size());
    }

    @Test
    @DisplayName("F2 · una coma dentro de un campo no descuadra el CSV")
    void csvConComas() throws Exception {
        Path csv = dir.resolve("p.csv"), json = dir.resolve("p.json");
        try { convertir(recurso("pedidos_dificiles.xml"), csv, json); } catch (Exception esperable) { }
        Path ok = dir.resolve("ok.csv");
        convertir(soloPrimerPedido(), ok, dir.resolve("ok.json"));
        for (String linea : Files.readAllLines(ok, StandardCharsets.UTF_8))
            assertEquals(6, campos(linea).size(), "campos en: " + linea);
    }

    @Test
    @DisplayName("F3 · comillas y & en los datos: JSON válido y texto intacto")
    void comillasEnJson() throws Exception {
        Path json = dir.resolve("ok.json");
        convertir(soloPrimerPedido(), dir.resolve("ok.csv"), json);
        JsonNode p = new ObjectMapper().readTree(json.toFile()).get(0);
        assertEquals("Talleres \"O Rápido\", S.L.", p.get("cliente").asText());
        assertEquals("Tornillería & juntas", p.get("lineas").get(1).get("producto").asText());
    }

    @Test
    @DisplayName("F4 · fichero inexistente: se informa y no se crean salidas")
    void inexistente() {
        Path csv = dir.resolve("p.csv"), json = dir.resolve("p.json");
        assertThrows(Exception.class, () -> convertir(dir.resolve("no-existe.xml"), csv, json));
        assertFalse(Files.exists(csv), "no debe quedar un CSV vacío");
        assertFalse(Files.exists(json), "no debe quedar un JSON vacío");
    }

    @Test
    @DisplayName("F5 · XML mal formado: se informa y no se crean salidas")
    void malFormado() throws Exception {
        Path xml = recurso("pedidos_mal_formado.xml");
        Path csv = dir.resolve("p.csv"), json = dir.resolve("p.json");
        assertThrows(Exception.class, () -> convertir(xml, csv, json));
        assertFalse(Files.exists(csv));
        assertFalse(Files.exists(json));
    }

    @Test
    @DisplayName("F6 · pedido sin <cliente>: se informa del error, no se ignora")
    void pedidoSinCliente() throws Exception {
        Path xml = recurso("pedidos_dificiles.xml");
        assertThrows(Exception.class, () -> convertir(xml, dir.resolve("p.csv"), dir.resolve("p.json")));
    }

    @Test
    @DisplayName("F7 · un fallo no deja a medias un fichero bueno anterior")
    void noPisaSalidaAnterior() throws Exception {
        Path csv = dir.resolve("p.csv"), json = dir.resolve("p.json");
        convertir(recurso("pedidos.xml"), csv, json);
        String antes = Files.readString(csv);
        try { convertir(recurso("pedidos_dificiles.xml"), csv, json); } catch (Exception esperable) { }
        assertEquals(antes, Files.readString(csv));
    }

    // ---------- utilidades ----------

    Path soloPrimerPedido() throws Exception {
        String xml = Files.readString(Path.of("src/test/resources/pedidos_dificiles.xml"));
        int corte = xml.indexOf("<pedido id=\"P102\"");
        Path f = dir.resolve("uno.xml");
        Files.writeString(f, xml.substring(0, corte) + "</pedidos>\n");
        return f;
    }

    /** Analizador CSV RFC 4180 mínimo, solo para comprobar. */
    static List<String> campos(String linea) {
        List<String> r = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (comillas) {
                if (c == '"' && i + 1 < linea.length() && linea.charAt(i + 1) == '"') { b.append('"'); i++; }
                else if (c == '"') comillas = false;
                else b.append(c);
            } else if (c == '"') comillas = true;
            else if (c == ',') { r.add(b.toString()); b.setLength(0); }
            else b.append(c);
        }
        r.add(b.toString());
        return r;
    }
}
