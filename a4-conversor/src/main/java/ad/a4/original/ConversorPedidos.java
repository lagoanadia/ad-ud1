package ad.a4.original;

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
 * NO CORREGIR: es el material que se audita.
 */
public class ConversorPedidos {

    public static void main(String[] args) {
        String entrada = args.length > 0 ? args[0] : "pedidos.xml";
        convertir(entrada, "pedidos.csv", "pedidos.json");
    }

    public static void convertir(String entrada, String salidaCsv, String salidaJson) {
        try {
            FileWriter csv = new FileWriter(salidaCsv);
            FileWriter json = new FileWriter(salidaJson);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(entrada));

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

                    csv.write(id + "," + fecha + "," + cliente + "," + producto + ","
                            + cantidad + "," + precio + "\n");

                    json.write("{\"producto\": \"" + producto + "\", \"cantidad\": " + cantidad
                            + ", \"precio\": " + precio + "}, ");
                }
                json.write("]},\n");
            }

            json.write("]\n");
            csv.close();
            json.close();
            System.out.println("Conversión completada.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
