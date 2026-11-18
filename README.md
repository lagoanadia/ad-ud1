# 🗂️ AD · UD1 — Ficheros y persistencia en Java

Activities for **Unit 1 of *Acceso a Datos*** (DAM): working with the file system, CSV, JSON and XML from Java. Each activity is an independent Maven project.

## 📁 Project Structure

```
├── a1-explorador/     → Command-line directory explorer with java.nio.file
├── a2-importador/     → CSV client importer: splits valid and rejected rows
│   └── salida/        → clientes_ok.csv, rechazados.csv
├── a3-persistencia/   → Generic JSON repository + XML backup with Jackson
│   └── lectura.md     → Answers to the ten code-reading questions
└── a4-conversor/      → Audit and fix of an AI-generated XML → CSV/JSON converter
    └── AUDITORIA.md   → Bug table and answers
```

## 🚀 How It Works

### A1 · Explorador

Walks a path given as an argument and prints, for each entry, its `rwx` permissions, size, last-modified date and name. With `-r` it recurses through the whole tree with increasing indentation. An `AccessDeniedException` on one entry is counted in the final summary instead of stopping the walk.

### A2 · Importador

Reads `clientes_importacion.csv` line by line with an explicit encoding, validates each row and writes good rows to `salida/clientes_ok.csv` and bad ones to `salida/rechazados.csv` — a single bad row never stops the import.

### A3 · Persistencia

A generic repository that stores records in a JSON file, plus an XML backup:

```java
public class RepositorioJson<T extends Identificable> implements Repositorio<T>
```

Only the code that builds it knows there's a JSON file behind it; the rest of the app just sees `Repositorio<T>`. Jackson is configured with `JavaTimeModule` so `LocalDate` is written as ISO text, and lists are read with `constructCollectionType` to avoid type erasure turning items into `LinkedHashMap`s.

### A4 · Conversor

An AI-generated XML → CSV/JSON converter is tested against 7 cases (`OriginalTest`): **all 7 fail** in the original. `AUDITORIA.md` explains each bug — invalid JSON, commas and quotes not escaped, output files truncated *before* the input is parsed, silent exceptions — and the fixed version lives in `ad/a4/tocar/`.

## 🧠 Key Concepts

- **`java.nio.file`** — `Path`, `Files.walk`, file attributes and permissions
- **Character encodings** — reading/writing text with an explicit `Charset`
- **Error handling per item** — continue on failure, report at the end
- **Jackson** — JSON and XML (de)serialisation, `java.time` support
- **Generics with bounds** — `<T extends Identificable>`
- **Repository pattern** — hiding the storage behind an interface
- **Testing with JUnit 5** — contract tests, auditing code by running tests
- **Resource safety** — try-with-resources, not destroying good output on failure

## 🛠️ How To Run

Each activity is a separate Maven project. For example:

```bash
cd a1-explorador
mvn -q compile exec:java -Dexec.args="prueba -r"
```

```bash
cd a3-persistencia
mvn test
```
