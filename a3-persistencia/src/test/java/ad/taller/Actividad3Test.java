package ad.taller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Actividad 3 · tocar. Las pruebas de los dos cambios (RepositorioJson y RespaldoXml).
 * Para ejecutarla: quitad la línea @Disabled y después   mvn test -Dtest=Actividad3Test
 */
class Actividad3Test {

    @TempDir
    Path dir;

    private final List<Reserva> v1 = List.of(new Reserva("R1", "Ana Núñez", LocalDate.of(2026, 9, 20), 2));
    private final List<Reserva> v2 = List.of(new Reserva("R1", "Ana Núñez", LocalDate.of(2026, 9, 20), 2),
            new Reserva("R2", "Luis Pardo", LocalDate.of(2026, 9, 21), 3));

    @Test
    @DisplayName("Cambio 1 · la primera vez no hay copia; la segunda guarda la versión anterior en .bak")
    void copiaDeLaVersionAnterior() {
        Path f = dir.resolve("reservas.json");
        Path bak = dir.resolve("reservas.json.bak");
        var repo = new RepositorioJson<>(f, Reserva.class);

        repo.guardarTodos(v1);
        assertFalse(Files.exists(bak), "la primera vez no hay nada que conservar");

        repo.guardarTodos(v2);
        assertTrue(Files.exists(bak), "la segunda vez hay que conservar la primera versión");
        assertEquals(v1, new RepositorioJson<>(bak, Reserva.class).cargarTodos());
        assertEquals(v2, repo.cargarTodos());
    }

    @Test
    @DisplayName("Cambio 1 · la copia es siempre la versión inmediatamente anterior, no se acumula")
    void laCopiaEsLaUltimaAnterior() {
        Path f = dir.resolve("reservas.json");
        var repo = new RepositorioJson<>(f, Reserva.class);
        repo.guardarTodos(v1);
        repo.guardarTodos(v2);
        repo.guardarTodos(v1);
        assertEquals(v2, new RepositorioJson<>(dir.resolve("reservas.json.bak"), Reserva.class).cargarTodos());
        assertEquals(v1, repo.cargarTodos());
    }

    @Test
    @DisplayName("Cambio 2 · un respaldo con un id repetido no se importa: PersistenciaException que nombra el id")
    void idRepetido() throws Exception {
        Path f = dir.resolve("repetido.xml");
        Files.writeString(f, "<reservas>"
                + "<reserva><id>R1</id><cliente>Ana</cliente><fecha>2026-09-20</fecha><personas>2</personas></reserva>"
                + "<reserva><id>R1</id><cliente>Luis</cliente><fecha>2026-09-21</fecha><personas>3</personas></reserva>"
                + "</reservas>");
        var e = assertThrows(PersistenciaException.class, () -> new RespaldoXml().importar(f));
        assertTrue(e.getMessage().contains("R1"), "el mensaje tiene que decir qué id se repite: " + e.getMessage());
    }

    @Test
    @DisplayName("Cambio 2 · un respaldo sin repetidos sigue importándose")
    void sinRepetidos() {
        Path f = dir.resolve("bien.xml");
        var r = new RespaldoXml();
        r.exportar(v2, f);
        assertEquals(v2, r.importar(f));
    }
}
