# lectura.md · actividad 3 · las diez preguntas

**1 · RepositorioJson, línea 18.** ¿Qué quiere decir `<T extends Identificable>`? ¿Qué parte de
la aplicación sabe que los datos están en un fichero JSON y cuál solo conoce `Repositorio`?

`<T extends Identificable>` es un parámetro de tipo acotado: `RepositorioJson` admite cualquier
tipo `T`, pero solo si implementa `Identificable` (tiene `id()`), porque `Repositorio.buscar()`
lo necesita para filtrar por id. Solo quien construye el repositorio
(`new RepositorioJson<>(ruta, Reserva.class)`) sabe que hay un JSON detrás; el resto de la
aplicación solo ve la interfaz `Repositorio<T>`, sin saber si por debajo hay JSON, XML o una
base de datos.

**2 · líneas 22 a 25.** El `ObjectMapper` se configura con tres cosas. Decid para qué sirve cada
una. Si quitáis la línea de `JavaTimeModule` y ejecutáis `mvn test`, ¿qué pasa y con qué mensaje?

`registerModule(new JavaTimeModule())` enseña a Jackson a serializar tipos de `java.time` como
`LocalDate`. `disable(WRITE_DATES_AS_TIMESTAMPS)` hace que esas fechas se escriban como texto
ISO (`"2026-09-20"`) en vez de como un array de números. `enable(INDENT_OUTPUT)` da formato
legible al JSON. Sin `JavaTimeModule`, `mvn test` falla con `InvalidDefinitionException`:
*"Java 8 date/time type `java.time.LocalDate` not supported by default"*.

**3 · línea 29.** ¿Por qué no se escribe simplemente `readValue(fichero, List.class)`? ¿Cuándo
aparecería el fallo y qué tipo de objeto habría dentro de la lista?

Por el borrado de tipos genéricos de Java: en tiempo de ejecución `List.class` no sabe de qué es
la lista, así que cada elemento se deserializaría como un `LinkedHashMap` genérico, no como
`Reserva`. No fallaría al leer, sino más tarde, al usar un elemento como si fuera `Reserva` (por
ejemplo `r.cliente()`): un `ClassCastException`. Por eso se usa `tipoLista`
(`constructCollectionType`), que conserva el tipo concreto.

**4 · línea 34.** Si el fichero no existe, `cargarTodos()` devuelve una lista vacía y no falla.
¿Por qué es lo correcto la primera vez que se ejecuta la aplicación? ¿Y si el fichero existe pero
está cortado a mitad?

La primera vez no hay reservas guardadas todavía: "no existe el fichero" significa "no hay
datos", no un error, así que tratarlo como lista vacía evita que la primera ejecución falle
siempre. Si el fichero existe pero está cortado, sí es un error real: el `JsonProcessingException`
de la línea 37 salta y se relanza como `PersistenciaException`, porque ahí sí había datos que se
han corrompido, y devolver vacío escondería la pérdida.

**5 · líneas 37 a 40.** Hay dos `catch` seguidos para leer. ¿Qué distingue un fallo del otro?
¿Por qué el de `JsonProcessingException` va primero? Pista: mirad de qué clase hereda.

`JsonProcessingException` es un fallo de interpretación: el fichero se lee bien, pero su
contenido no es JSON válido o no encaja con `Reserva`. `IOException` genérica es un fallo de
acceso al propio fichero (permisos, disco), antes de llegar a interpretar nada.
`JsonProcessingException` **extiende** `IOException`, así que si el `catch (IOException)` fuera
primero, atraparía también los fallos de JSON con el mensaje genérico "leer" en vez del preciso
"interpretar el JSON de" — por eso la más concreta va antes.

**6 · líneas 50 a 53.** Describid con vuestras palabras los tres pasos de guardar. Si se corta la
luz justo entre el segundo y el tercero, ¿qué queda en el disco? ¿Y si el código escribiera
directamente sobre `reservas.json`?

Los tres pasos: (1) crear un fichero temporal vacío en la misma carpeta (línea 50); (2) escribir
ahí todos los datos nuevos (línea 51); (3) sustituir el fichero final por el temporal con
`Files.move(..., ATOMIC_MOVE)` (línea 53). Si se corta la luz entre el 2 y el 3, en disco queda el
`.tmp` a medias (o completo) y `reservas.json` **intacto**, con la versión anterior, porque el
`move` nunca se ejecutó. Si se escribiera directamente sobre `reservas.json`, un corte a mitad
dejaría el fichero final con JSON incompleto: la siguiente carga lanzaría `PersistenciaException`
y se habrían perdido los datos buenos de antes, sin copia de seguridad.

**7 · línea 50.** El temporal se crea en la misma carpeta que el destino. ¿Qué pasaría si
estuviera en otro disco? Buscad en el código el sitio donde se vería.

`Files.createTempFile(dir, ...)` usa explícitamente `dir` (la carpeta del fichero final), no una
carpeta temporal del sistema. Si el temporal estuviera en otro disco, `Files.move` de la línea 53
dejaría de poder ser atómico: `ATOMIC_MOVE` solo lo garantiza dentro del mismo sistema de
ficheros; entre discos distintos se implementaría como copiar + borrar, con riesgo de quedarse a
medias. Se vería justo ahí: `Files.move(tmp, fichero, REPLACE_EXISTING, ATOMIC_MOVE)` lanzaría
`AtomicMoveNotSupportedException`.

**8 · líneas 56 a 58.** ¿Para qué sirve el `finally`? ¿Cuándo hay un temporal que borrar y cuándo
ya no queda ninguno? ¿Por qué el borrado lleva su propio `try` y `catch` vacío?

El `finally` garantiza que se intenta borrar el temporal tanto si `guardarTodos` termina bien
como si lanza una excepción. Hay temporal que borrar si algo falla después de crearlo (línea 50)
pero antes de que `Files.move` lo consuma; si el `move` ya se ejecutó, el temporal ya no existe
como tal (se convirtió en el fichero final), así que `deleteIfExists` no encuentra nada y no hace
nada. Lleva su propio `try`/`catch` vacío porque es una limpieza de mejor esfuerzo: si falla, no
debe ocultar ni sustituir la excepción real que ya se estaba propagando desde el `catch` anterior.

**9 · RespaldoXml, líneas 19 a 23.** El `record Reservas` lleva tres anotaciones. Decid qué
cambia en el XML con cada una. Comprobadlo: quitad `@JacksonXmlElementWrapper(useWrapping = false)`
y mirad qué prueba falla y con qué número.

`@JacksonXmlRootElement(localName = "reservas")` nombra el elemento raíz `<reservas>` en vez del
nombre por defecto del record. `@JacksonXmlElementWrapper(useWrapping = false)` evita envolver la
lista en un elemento contenedor extra (sin ella saldría un `<reserva>` de más envolviendo toda la
lista); cada elemento pasa a ser hijo directo de la raíz. `@JacksonXmlProperty(localName = "reserva")`
fija el nombre de cada elemento individual. Quitando `useWrapping = false` falla la prueba
**6 · estructura: raíz `<reservas>` y un `<reserva>` por registro** de `RespaldoXmlTest`, porque
`xml.split("<reserva>", -1).length - 1` deja de dar 2.

**10 · líneas 37 y 47.** Una reserva con un cliente «Talleres "O Rápido" & Cía» se exporta y se
vuelve a importar igual. ¿Quién escribe el `&amp;` y quién lo deshace? ¿Cuándo es `r.reserva()`
nulo y qué prueba lo cubre?

El propio `XmlMapper` escapa `&` a `&amp;` al exportar (línea 37, `xml.writeValue`), porque es un
carácter especial de XML, y el mismo `XmlMapper` lo deshace al importar (línea 45,
`xml.readValue`) — lo hace la librería, no el código de `RespaldoXml`. `r.reserva()` es `null`
cuando el XML raíz no trae ningún `<reserva>` dentro (por ejemplo `<reservas/>` vacío): Jackson no
crea una lista vacía por sí solo para un elemento ausente, deja el campo a `null`; por eso la
línea 47 comprueba `r.reserva() == null ? List.of() : r.reserva()`. La prueba
**8 · respaldo vacío: lista vacía** de `RespaldoXmlTest` cubre justo ese caso.
