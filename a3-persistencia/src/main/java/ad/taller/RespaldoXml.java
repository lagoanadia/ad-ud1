package ad.taller;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Exportación e importación del respaldo en XML. */
public class RespaldoXml {

    /** Envoltorio que da nombre al elemento raíz y a cada registro. */
    @JacksonXmlRootElement(localName = "reservas")
    public record Reservas(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "reserva")
            List<Reserva> reserva) { }

    private final XmlMapper xml;

    public RespaldoXml() {
        xml = new XmlMapper();
        xml.registerModule(new JavaTimeModule());
        xml.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        xml.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void exportar(List<Reserva> datos, Path destino) {
        try {
            Files.createDirectories(destino.toAbsolutePath().getParent());
            xml.writeValue(destino.toFile(), new Reservas(datos));
        } catch (IOException e) {
            throw new PersistenciaException("exportar el XML a", destino, e);
        }
    }

    public List<Reserva> importar(Path origen) {
        try {
            Reservas r = xml.readValue(origen.toFile(), Reservas.class);
            List<Reserva> reservas = r.reserva() == null ? List.of() : r.reserva();
            // Cambio 2: un id repetido no se importa en silencio. Se recorre una vez
            // con un HashSet; add() devuelve false si el id ya estaba.
            Set<String> ids = new HashSet<>();
            for (Reserva reserva : reservas) {
                if (!ids.add(reserva.id())) {
                    throw new PersistenciaException("importar el XML de", origen,
                            new IllegalStateException("id repetido: " + reserva.id()));
                }
            }
            return reservas;
        } catch (IOException e) {
            throw new PersistenciaException("importar el XML de", origen, e);
        }
    }
}
