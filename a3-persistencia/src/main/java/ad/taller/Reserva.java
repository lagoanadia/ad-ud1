package ad.taller;

import java.time.LocalDate;

/** Entidad del caso común de la actividad 3: una reserva del taller. La misma para todo el grupo. */
public record Reserva(String id, String cliente, LocalDate fecha, int personas) implements Identificable {
}
