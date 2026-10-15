package ad.taller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioJsonTest {

    @TempDir
    Path dir;

    private final List<Reserva> datos = List.of(
            new Reserva("R1", "Ana Núñez", LocalDate.of(2026, 9, 20), 2),
            new Reserva("R2", "Xoán Pombo", LocalDate.of(2026, 10, 3), 4));

    @Test
    @DisplayName("1 · ida y vuelta JSON: se recupera exactamente lo guardado")
    void idaYVueltaJson() {
        var repo = new RepositorioJson<>(dir.resolve("datos/reservas.json"), Reserva.class);
        repo.guardarTodos(datos);
        assertEquals(datos, repo.cargarTodos());
        assertEquals("Xoán Pombo", repo.buscar("R2").orElseThrow().cliente());
    }

    @Test
    @DisplayName("2 · fichero inexistente: lista vacía, sin excepción")
    void ficheroInexistente() {
        var repo = new RepositorioJson<>(dir.resolve("no-existe.json"), Reserva.class);
        assertTrue(repo.cargarTodos().isEmpty());
    }

    @Test
    @DisplayName("3 · JSON corrupto: PersistenciaException con el nombre del fichero")
    void jsonCorrupto() throws Exception {
        Path f = dir.resolve("reservas.json");
        Files.writeString(f, "[ { \"id\": \"R1\", \"cliente\": ");       // cortado a mitad
        var repo = new RepositorioJson<>(f, Reserva.class);
        var e = assertThrows(PersistenciaException.class, repo::cargarTodos);
        assertTrue(e.getMessage().contains("reservas.json"), e.getMessage());
    }

    @Test
    @DisplayName("4 · escritura segura: no quedan temporales y el fichero es válido")
    void escrituraSegura() throws Exception {
        Path f = dir.resolve("reservas.json");
        var repo = new RepositorioJson<>(f, Reserva.class);
        repo.guardarTodos(datos);
        repo.guardarTodos(datos.subList(0, 1));
        try (var s = Files.list(dir)) {
            assertTrue(s.noneMatch(x -> x.getFileName().toString().endsWith(".tmp")), "no debe quedar ningún .tmp");
        }
        assertEquals(1, repo.cargarTodos().size());
    }
}
