package ad.a4.tocar;

import java.io.File;
import java.io.FileWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Conversor de pedidos XML a CSV y JSON.
 * Código de partida representativo de lo que devuelve un modelo de IA con el prompt de la A4.
 * COPIA DE TRABAJO de la actividad 4: es la de la IA, con los dos cambios de la actividad ya hechos.
 */
public class ConversorPedidos {

    public static void main(String[] args) throws Exception {
        String entrada = args.length > 0 ? args[0] : "pedidos.xml";
        convertir(entrada, "pedidos.csv", "pedidos.json");
    }

    public static void convertir(String entrada, String salidaCsv, String salidaJson) throws Exception {
        // Cambio 1: el XML se valida ANTES de abrir ningún fichero de salida (antes, los dos
        // FileWriter se abrían primero, y al abrir ya truncaban cualquier fichero bueno que
        // hubiera de una conversión anterior, aunque el XML de entrada resultara ilegible).
        // Además, ya no se atrapa la excepción aquí dentro: se deja subir (el método declara
        // throws Exception) para que quien llame a convertir() se entere de que algo ha fallado,
        // en vez de que el programa "termine" en silencio con un simple printStackTrace().
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(entrada));

        // los dos FileWriter solo se abren una vez que el XML ya se ha podido leer; con
        // try-with-resources, además, se cierran siempre, aunque falle algo a mitad del bucle
        try (FileWriter csv = new FileWriter(salidaCsv);
             FileWriter json = new FileWriter(salidaJson)) {

            csv.write("pedido,fecha,cliente,producto,cantidad,precio\n");
            json.write("[\n");

            NodeList pedidos = doc.getElementsByTagName("pedido");
            for (int i = 0; i < pedidos.getLength(); i++) {
                Element pedido = (Element) pedidos.item(i);
                String id = pedido.getAttribute("id");
                String fecha = pedido.getAttribute("fecha");
                String cliente = pedido.getElementsByTagName("cliente").item(0).getTextContent();

                json.write("  {\"id\": \"" + id + "\", \"fecha\": \"" + fecha
                        + "\", \"cliente\": \"" + cliente + "\", \"lineas\": [");

                NodeList lineas = pedido.getElementsByTagName("linea");
                for (int j = 0; j < lineas.getLength(); j++) {
                    Element linea = (Element) lineas.item(j);
                    String producto = linea.getAttribute("producto");
                    int cantidad = Integer.parseInt(linea.getAttribute("cantidad"));
                    double precio = Double.parseDouble(linea.getAttribute("precio"));

                    // Cambio 2: cliente y producto pasan por csv(...) antes de escribirse, para
                    // que una coma o una comilla dentro del dato no descuadre las columnas.
                    csv.write(id + "," + fecha + "," + csv(cliente) + "," + csv(producto) + ","
                            + cantidad + "," + precio + "\n");

                    json.write("{\"producto\": \"" + producto + "\", \"cantidad\": " + cantidad
                            + ", \"precio\": " + precio + "}, ");
                }
                json.write("]},\n");
            }

            json.write("]\n");
        }
        System.out.println("Conversión completada.");
    }

    /**
     * Si el campo lleva una coma, una comilla o un salto de línea, lo devuelve entrecomillado
     * y con las comillas internas duplicadas (regla RFC 4180); si no, lo devuelve tal cual.
     */
    private static String csv(String campo) {
        if (campo.contains(",") || campo.contains("\"") || campo.contains("\n")) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }
}
