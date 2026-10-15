package ad.taller;

/** Fallo de persistencia con el fichero y la operación que lo causaron. No comprobada. */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String operacion, java.nio.file.Path fichero, Throwable causa) {
        super("No se pudo " + operacion + " " + fichero + ": " + causa.getMessage(), causa);
    }
}
