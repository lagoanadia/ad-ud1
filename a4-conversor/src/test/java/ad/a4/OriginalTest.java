package ad.a4;

import java.nio.file.Path;

/** Las mismas pruebas contra el código de la IA: deben FALLAR. Se entrega para demostrar los fallos. */
class OriginalTest extends ConversorContrato {
    @Override
    void convertir(Path xml, Path csv, Path json) {
        ad.a4.original.ConversorPedidos.convertir(xml.toString(), csv.toString(), json.toString());
    }
}
