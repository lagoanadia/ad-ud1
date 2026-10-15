package ad.taller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/** Persistencia en un fichero JSON con escritura atómica. */
public class RepositorioJson<T extends Identificable> implements Repositorio<T> {

    private final Path fichero;
    private final JavaType tipoLista;
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public RepositorioJson(Path fichero, Class<T> tipo) {
        this.fichero = fichero;
        this.tipoLista = json.getTypeFactory().constructCollectionType(List.class, tipo);
    }

    @Override
    public List<T> cargarTodos() {
        if (!Files.exists(fichero)) return List.of();
        try {
            return json.readValue(fichero.toFile(), tipoLista);
        } catch (JsonProcessingException e) {
            throw new PersistenciaException("interpretar el JSON de", fichero, e);
        } catch (IOException e) {
            throw new PersistenciaException("leer", fichero, e);
        }
    }

    @Override
    public void guardarTodos(List<T> elementos) {
        Path tmp = null;
        try {
            Path dir = fichero.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            tmp = Files.createTempFile(dir, fichero.getFileName().toString(), ".tmp");
            json.writeValue(tmp.toFile(), elementos);
            // Cambio 1: antes de sustituir, si ya había una versión, se conserva en .bak.
            // La copia va ANTES del move: si fuera después, el fichero ya sería el nuevo
            // y la copia guardaría la versión que se acaba de escribir, no la anterior.
            if (Files.exists(fichero)) {
                Path bak = fichero.resolveSibling(fichero.getFileName() + ".bak");
                Files.copy(fichero, bak, REPLACE_EXISTING);
            }
            Files.move(tmp, fichero, REPLACE_EXISTING, ATOMIC_MOVE);
        } catch (IOException e) {
            throw new PersistenciaException("guardar", fichero, e);
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignorada) { /* ya no existe */ }
            }
        }
    }
}
