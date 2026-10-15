package ad.taller;

import java.util.List;
import java.util.Optional;

/** Lo único que el resto de la aplicación conoce de la persistencia. En la UD2 cambia la implementación. */
public interface Repositorio<T extends Identificable> {

    List<T> cargarTodos();

    void guardarTodos(List<T> elementos);

    default Optional<T> buscar(String id) {
        return cargarTodos().stream().filter(e -> e.id().equals(id)).findFirst();
    }
}
