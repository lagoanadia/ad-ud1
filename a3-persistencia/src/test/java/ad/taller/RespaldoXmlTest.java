package ad.taller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RespaldoXmlTest {

    @TempDir
    Path dir;

    private final List<Reserva> datos = List.of(
            new Reserva("R1", "Ana Núñez", LocalDate.of(2026, 9, 20), 2),
            new Reserva("R2", "Talleres \"O Rápido\" & Cía", LocalDate.of(2026, 10, 3), 4));

    @Test
    @DisplayName("5 · ida y vuelta XML: exportar e importar da los mismos datos")
    void idaYVueltaXml() {
        Path f = dir.resolve("respaldo/reservas.xml");
        var r = new RespaldoXml();
        r.exportar(datos, f);
        assertEquals(datos, r.importar(f));
    }

    @Test
    @DisplayName("6 · estructura: raíz <reservas> y un <reserva> por registro")
    void estructuraXml() throws Exception {
        Path f = dir.resolve("reservas.xml");
        new RespaldoXml().exportar(datos, f);
        String xml = Files.readString(f);
        assertTrue(xml.startsWith("<reservas>"), xml);
        assertEquals(2, xml.split("<reserva>", -1).length - 1);
        assertTrue(xml.contains("&amp;"), "el & debe ir escapado");
    }

    @Test
    @DisplayName("7 · XML mal formado: PersistenciaException")
    void xmlMalFormado() throws Exception {
        Path f = dir.resolve("roto.xml");
        Files.writeString(f, "<reservas><reserva><id>R1</id></reservas>");
        assertThrows(PersistenciaException.class, () -> new RespaldoXml().importar(f));
    }

    @Test
    @DisplayName("8 · respaldo vacío: lista vacía")
    void respaldoVacio() throws Exception {
        Path f = dir.resolve("vacio.xml");
        Files.writeString(f, "<reservas/>");
        assertTrue(new RespaldoXml().importar(f).isEmpty());
    }
}
