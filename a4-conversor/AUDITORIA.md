# AUDITORIA.md · actividad 4 · auditoría del conversor generado por IA

`mvn test -Dtest=OriginalTest` contra `ad/a4/original/ConversorPedidos.java` (sin tocar):
**7 pruebas, 7 en rojo** (5 failures + 2 errors), confirmado ejecutándolo de verdad, no solo
leído. Las líneas se refieren siempre al original.

## 1 · La tabla

| Prueba | Qué pasa | Línea(s) | Categoría |
|---|---|---|---|
| F1 · el JSON es válido | El JSON que genera no se puede leer | 42-58 | formato |
| F2 · una coma en un campo | El CSV tiene más campos de los debidos | 52-53 | formato |
| F3 · comillas y & en los datos | El JSON se rompe o pierde texto | 55-56 | formato |
| F4 · fichero inexistente | No avisa, y quedan ficheros vacíos | 25-26 y 65-66 | recursos y excepciones |
| F5 · XML mal formado | No avisa, y quedan ficheros vacíos | 25-26 y 65-66 | recursos y excepciones |
| F6 · pedido sin `<cliente>` | Se rompe y nadie se entera | 40 (silenciado en 65-66) | excepciones |
| F7 · un fallo no pisa lo bueno | Una salida buena anterior se destruye | 25-26 | recursos |

## 2 · Las siete preguntas

**2 · líneas 25-30.** ¿En qué orden se abren los ficheros de salida y se lee el XML? ¿Qué le
pasa a un `pedidos.csv` bueno de ayer si el XML de hoy está roto?

Los `FileWriter` de `csv` y `json` se abren en las líneas 25-26, **antes** de crear el
`DocumentBuilder` y de `builder.parse(entrada)` en la línea 30. Abrir un `FileWriter` con el
constructor de un argumento trunca el fichero de destino a 0 bytes al momento, tenga ya
contenido válido o no. Así que si `pedidos.csv` tenía la conversión buena de ayer y hoy el XML
de entrada no existe o está mal formado, el programa ya lo ha vaciado en la línea 25 antes de
enterarse en la línea 30 de que no va a poder completar nada — se pierde la salida buena
anterior por un fallo que ni siquiera tiene que ver con los ficheros de salida.

**3 · líneas 65-66.** ¿Qué hace este `catch` con cualquier error? ¿Cómo se entera de él quien
llama a `convertir`? ¿Qué se pierde con un `catch (Exception)` tan ancho?

`catch (Exception e) { e.printStackTrace(); }` atrapa cualquier excepción —de parseo XML, de
E/S, un `NullPointerException`, lo que sea— y solo la imprime por la salida de error estándar.
`convertir` no declara `throws` y no relanza nada, así que vuelve con normalidad (`void`)
después del `catch`: quien lo llama no tiene forma de saber que algo falló, ni por el valor de
retorno (no hay) ni por una excepción (no se propaga). Con un `catch` tan ancho se pierde la
distinción entre fallos muy distintos (fichero inexistente, XML roto, un dato que falta): todos
acaban en la misma traza por consola y todos se tratan exactamente igual, que es como no
tratarlos.

**4 · líneas 62-63.** ¿Cuándo se cierran los ficheros? ¿Qué pasa con ellos si el programa lanza
una excepción antes? Decid con qué construcción de Java se arreglaría.

`csv.close()` y `json.close()` están en las líneas 62-63, justo antes del mensaje de éxito,
dentro del mismo `try`. Si `builder.parse()` (línea 30) lanza una excepción, el flujo salta
directo al `catch` de la línea 65 sin pasar por esos `close()`: los dos `FileWriter` se quedan
abiertos, con lo que tuvieran en el búfer sin vaciar, y el sistema operativo solo los libera
cuando la JVM termina. Se arregla con **try-with-resources**
(`try (FileWriter csv = ...; FileWriter json = ...) { ... }`), que garantiza el cierre de los
dos pase lo que pase dentro del bloque.

**5 · línea 40.** Si un pedido no trae `<cliente>`, ¿qué devuelve `item(0)` y qué pasa en la
línea siguiente? ¿Por qué el programa "termina" sin decir nada?

`pedido.getElementsByTagName("cliente")` devuelve una `NodeList` vacía si no hay ningún
`<cliente>`; sobre una `NodeList` vacía, `.item(0)` no lanza excepción, devuelve `null`. La
línea 40 llama `.getTextContent()` sobre ese `null` inmediatamente, lo que lanza
`NullPointerException`. Esa excepción sube hasta el `catch` de la línea 65, que la imprime y no
la propaga: `convertir()` vuelve con normalidad, el proceso sale con código 0, y quien lo
ejecute ve "Conversión completada" nunca —o nada— sin saber que ese pedido (y todos los que
venían después en el bucle) se quedaron sin procesar.

**6 · líneas 42-61.** El JSON se construye pegando textos. Buscad dos razones por las que no
es JSON válido aunque los datos sean normales.

(1) Coma sobrante al final: cada producto añade `", "` después de sí mismo en la línea 56 sin
comprobar si es el último de la lista, y cada pedido añade una coma tras `"]}"` en la línea 58
sin comprobar si es el último pedido del fichero. JSON no permite una coma antes de cerrar un
array o un objeto. (2) Sin escapado de caracteres especiales: `cliente` y `producto` se insertan
tal cual entre comillas dobles de JSON (líneas 42-43 y 55-56); si el dato trae una comilla o una
barra invertida, rompe la cadena JSON a la mitad. No hay ninguna librería de JSON de por medio
que escape esto, es texto plano concatenado.

**7 · líneas 52 y 55.** Un cliente se llama «Talleres "O Rápido", S.L.». ¿Qué ocurre con él en
el CSV y qué ocurre en el JSON? ¿Qué regla de escape falta en cada formato?

En el CSV (línea 52), `cliente` se escribe tal cual separado por comas. Como el nombre trae una
coma, la fila de salida tiene un campo de más y todo se desplaza una columna a partir de ahí.
Falta la regla RFC 4180: un campo con una coma (o una comilla, o un salto de línea) debe
entrecomillarse, y las comillas que ya traiga el dato deben duplicarse. En el JSON (línea 55),
el nombre lleva comillas dobles sin escapar: `"Talleres "O Rápido", S.L."` cierra la cadena JSON
en la primera comilla interna, dejando el resto como texto JSON inválido suelto. Falta escapar
`\"` (y `\\`) antes de insertar el valor.

**8 · líneas 25-26.** ¿Qué codificación usan estos `FileWriter`? En el Java 21 del aula, ¿se
puede demostrar que es un fallo ejecutando el programa? Si no, ¿por qué es un fallo igualmente
y cómo se anota?

`new FileWriter(salidaCsv)` sin segundo argumento usa el `Charset` por defecto de la
**plataforma**, no uno declarado explícitamente. En el Java 21 de este entorno (Linux, con el
charset por defecto ya en UTF-8) ejecutar el programa con nombres como "Xoán" o "Núñez" sale
correcto, así que no se puede demostrar el fallo con una prueba que compare bytes aquí — por
eso no aparece como un F1-F7, ninguna prueba de `ConversorContrato` lo detecta, porque todas
corren en el mismo entorno con el mismo charset. Aun así es un fallo real: el comportamiento
depende de una configuración externa que el código no controla ni declara, así que el mismo
`.jar` ejecutado en otra máquina con otro charset por defecto (Windows con `code page 1252`,
por ejemplo) produciría una salida distinta con los mismos datos de entrada sin que nada lo
avise. Se anota igualmente, en una fila aparte de la tabla, con categoría **codificación**.

## 3 · Los dos cambios (`tocar/ConversorPedidos.java`)

- **Cambio 1** (resuelve F4, F5 y la mitad de F7): el XML se valida con `builder.parse()`
  *antes* de abrir ningún `FileWriter`, así que un fichero inexistente o mal formado nunca llega
  a truncar una salida existente. Los dos `FileWriter` pasan a `try-with-resources`, y
  `convertir()` ya no atrapa la excepción: la deja subir (`throws Exception`) para que quien
  llame se entere.
- **Cambio 2** (resuelve F2 y la mitad de F3, solo para el CSV): nuevo método `csv(String campo)`
  que entrecomilla y duplica las comillas internas si el campo trae una coma, una comilla o un
  salto de línea (RFC 4180); se usa en `cliente` y `producto` al escribir el CSV.

Lo que **no** se pide arreglar en esta actividad (y por tanto sigue fallando en `tocar/` si se
le pasaran las pruebas F1, F3-en-JSON y F6 de `ConversorContrato`): el JSON construido por
concatenación de texto (comas sobrantes, sin escapar) y el pedido sin `<cliente>`. Son los
fallos "difíciles" que el cuaderno deja para el producto real, con un agente, más adelante.

Verificado: `mvn test -Dtest=OriginalTest` → 7/7 en rojo. `mvn test -Dtest=Actividad4Test` →
3/3 en verde.
