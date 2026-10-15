package ad.a4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Actividad 4 · tocar. Las pruebas de los dos cambios en ad/a4/tocar/ConversorPedidos.java.
 * Para ejecutarla: quitad la línea @Disabled y después   mvn test -Dtest=Actividad4Test
 */
class Actividad4Test {

    @TempDir
    Path dir;

    private static void convertir(Path xml, Path csv, Path json) throws Exception {
        ad.a4.tocar.ConversorPedidos.convertir(xml.toString(), csv.toString(), json.toString());
    }

    @Test
    @DisplayName("Cambio 1 · fichero inexistente: se avisa con una excepción y no se crean salidas")
    void inexistente() {
        Path csv = dir.resolve("p.csv"), json = dir.resolve("p.json");
        assertThrows(Exception.class, () -> convertir(dir.resolve("no-existe.xml"), csv, json));
        assertFalse(Files.exists(csv), "no debe quedar un CSV vacío");
        assertFalse(Files.exists(json), "no debe quedar un JSON vacío");
    }

    @Test
    @DisplayName("Cambio 1 · XML mal formado: se avisa y no se crean salidas")
    void malFormado() throws Exception {
        Path xml = dir.resolve("roto.xml");
        Files.copy(Path.of("src/test/resources/pedidos_mal_formado.xml"), xml);
        Path csv = dir.resolve("p.csv"), json = dir.resolve("p.json");
        assertThrows(Exception.class, () -> convertir(xml, csv, json));
        assertFalse(Files.exists(csv));
        assertFalse(Files.exists(json));
    }

    @Test
    @DisplayName("Cambio 2 · una coma o una comilla dentro de un campo no descuadra el CSV")
    void csvConComasYComillas() throws Exception {
        Path xml = dir.resolve("uno.xml");
        Files.writeString(xml, "<pedidos><pedido id=\"P1\" fecha=\"2026-09-10\">"
                + "<cliente>Talleres \"O Rápido\", S.L.</cliente><lineas>"
                + "<linea producto=\"Tornillería\" cantidad=\"10\" precio=\"0.35\"/></lineas></pedido></pedidos>", StandardCharsets.UTF_8);
        Path csv = dir.resolve("p.csv");
        convertir(xml, csv, dir.resolve("p.json"));
        List<String> filas = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals(2, filas.size(), "cabecera y una fila");
        List<String> campos = ConversorContrato.campos(filas.get(1));
        assertEquals(6, campos.size(), "seis campos en: " + filas.get(1));
        assertEquals("Talleres \"O Rápido\", S.L.", campos.get(2));
    }
}
