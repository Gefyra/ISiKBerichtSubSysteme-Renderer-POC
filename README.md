# FHIR Narrative Generator

A Java application for generating human-readable narratives from FHIR R4 resources, with specialized support for ISiK (Informationstechnische Systeme in Krankenhäusern) Bericht Bundles.

## Features

- **FHIR R4 Narrative Generation**: Generate human-readable HTML narratives for FHIR resources
- **ISiK Bundle Support**: Specialized processing for German healthcare interoperability standards
- **Dual Interface**: Both traditional JAR execution and modern CLI using PicoCLI
- **Multiple Output Modes**: Bundle narrative only, complete Bundle with narratives, or Bundle with section text
- **Custom Templates**: Thymeleaf-based templates for different FHIR resource types
- **German Healthcare Standards**: Proper handling of German characters and ISiK profiles

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.8 or higher

### Building the Project

```bash
git clone <repository-url>
cd NarrativeGenerator
mvn clean package
```

## Usage

### 1. Command-Line Interface (CLI) - Recommended

The modern CLI interface supports ISiK Bundle processing with multiple output modes.

#### Basic Usage

```bash
# Show help
mvn exec:java -Dexec.args="--help"

# Generate Bundle with narratives (default mode)
mvn exec:java -Dexec.args="--pretty input-bundle.json"

# Generate with specific output mode
mvn exec:java -Dexec.args="--mode=BUNDLE_WITH_SECTION_TEXT --verbose input-bundle.json"
```

#### CLI Parameters

```
narrative-generator [-hpvV] [-m=<mode>] [-o=<outputFile>] <inputFile>
```

**Parameters:**
- `<inputFile>` - Input ISiK Bundle JSON file path (required)

**Options:**
- `-h, --help` - Show help message and exit
- `-m, --mode=<mode>` - Output mode (see below)
- `-o, --output=<outputFile>` - Output file path (default: stdout)
- `-p, --pretty` - Pretty print JSON output (default: true)
- `-v, --verbose` - Verbose output with detailed logging
- `-V, --version` - Print version information and exit

#### Output Modes

##### `BUNDLE_WITH_NARRATIVES` (Default)
Returns the complete Bundle with all entry resources having populated `text.div` fields.

```bash
mvn exec:java -Dexec.args="--mode=BUNDLE_WITH_NARRATIVES bundle.json"
```

##### `BUNDLE_WITH_SECTION_TEXT`
Returns the Bundle with narratives both in individual resources AND transferred to Composition section.text fields.

```bash
mvn exec:java -Dexec.args="--mode=BUNDLE_WITH_SECTION_TEXT bundle.json"
```

#### CLI Examples

```bash
# Basic usage with pretty printing
mvn exec:java -Dexec.args="--pretty my-bundle.json"

# Save output to file with verbose logging
mvn exec:java -Dexec.args="--mode=BUNDLE_WITH_NARRATIVES --verbose --output=result.json input.json"

# Generate section text with detailed output
mvn exec:java -Dexec.args="--mode=BUNDLE_WITH_SECTION_TEXT --verbose --pretty bundle.json"
```

### 2. Traditional JAR Execution

For single resource narrative generation:

```bash
# Build JAR
mvn -DskipTests package

# Run with single resource
java -jar target/NarrativeGenerator-1.0-SNAPSHOT.jar <path/to/resource.json|xml>
```

**Exit codes:**
- 1: usage error
- 2: input file not found
- 3: no narrative generated (missing template/empty)
- 4: I/O error
- 5: other error

## Supported FHIR Resources

The narrative generator includes templates for:

- **Bundle** - Document bundles with entry summaries
- **Composition** - Document composition with sections
- **Patient** - Patient demographics and identifiers
- **Encounter** - Healthcare encounters and episodes
- **Observation** - Clinical observations and measurements

## ISiK Integration

This application is specifically designed to work with ISiK (Informationstechnische Systeme in Krankenhäusern) profiles:

- **ISiK Bericht Bundle**: Document bundles for German healthcare
- **ISiK Patient Profile**: German patient data standards
- **ISiK Encounter Profile**: Healthcare encounter specifications
- **Proper German Character Handling**: Umlauts and special characters (ü, ö, ä, ß)

### Example ISiK Bundle Processing

```bash
# Process ISiK Intensivstation Bundle
mvn exec:java -Dexec.args="--mode=BUNDLE_WITH_SECTION_TEXT --verbose isik-bundle.json"
```

## Templates

Templates are written in Thymeleaf and located in `src/main/resources/templates/`.

### Template Configuration

- **Templates**: `src/main/resources/templates/`
- **Manifest**: `src/main/resources/narratives/manifest.properties`

### Adding New Templates

1. Create template file: `src/main/resources/templates/ResourceType.html`
2. Add to manifest:
   ```properties
   resourcetype.resourceType=ResourceType
   resourcetype.narrative=classpath:templates/ResourceType.html
   resourcetype.style=THYMELEAF
   ```

### Template Example (Patient)

```html
<div xmlns="http://www.w3.org/1999/xhtml">
    <h3>Patient</h3>
    <!-- Patient ID -->
    <div th:if="${resource.hasId()}">
        <strong>ID:</strong>
        <span th:text="${resource.getResourceType().name() + '/' + resource.getId()}">Patient/123</span>
    </div>
    
    <!-- Name using HAPI's approach for R4 -->
    <div th:if="${resource.hasName()}">
        <strong>Name:</strong>
        <span th:each="name : ${resource.name}" th:if="${name.hasGiven()}">
            <span th:each="given : ${name.given}" th:text="${given.value}">Given</span>
        </span>
        <span th:if="${resource.name.get(0).hasFamily()}">
            <b th:text="${resource.name.get(0).family}">Family</b>
        </span>
    </div>
    
    <!-- Gender -->
    <div th:if="${resource.hasGender()}">
        <strong>Geschlecht:</strong>
        <span th:text="${resource.gender.toCode()}">Gender</span>
    </div>
    
    <!-- Birth date using HAPI's date formatting -->
    <div th:if="${resource.hasBirthDate()}">
        <strong>Geburtsdatum:</strong>
        <span th:text="${resource.birthDateElement.valueAsString}">Birth Date</span>
    </div>
</div>
```

## Dependencies

- **HAPI FHIR 8.4.0**: FHIR R4 processing library
- **Thymeleaf 3.1.2**: Template engine for narrative generation
- **PicoCLI 4.7.5**: Command-line interface framework
- **JUnit 5**: Testing framework

All HAPI FHIR dependencies use the centralized version property: `${hapi.version}` in `pom.xml`.

## Testing

The project includes comprehensive test suites organized by functionality:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ISiKNarrativeTest
```

## Standalone Executable JAR

The project is configured to create a fat JAR with all dependencies included using the Maven Shade Plugin.

### Building the Fat JAR

```bash
mvn clean package
```

This creates two JAR files in the `target/` directory:
- `NarrativeGenerator-1.0-SNAPSHOT.jar` - Regular JAR (requires classpath)
- `NarrativeGenerator-1.0-SNAPSHOT-shaded.jar` - Fat JAR with all dependencies

### Running the Standalone JAR

```bash
# Using the fat JAR (recommended for distribution)
java -jar target/NarrativeGenerator-1.0-SNAPSHOT-shaded.jar --help

# Process ISiK Bundle
java -jar target/NarrativeGenerator-1.0-SNAPSHOT-shaded.jar --mode=BUNDLE_WITH_NARRATIVES bundle.json

# Save to file with verbose output
java -jar target/NarrativeGenerator-1.0-SNAPSHOT-shaded.jar --mode=BUNDLE_WITH_SECTION_TEXT --verbose --output=result.json input.json
```

### Distribution

The shaded JAR can be distributed as a single file and run on any system with Java 21+ without requiring Maven or additional dependencies.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass: `mvn test`
5. Follow German healthcare data standards for ISiK compatibility
6. Submit a pull request

## License

[Add your license information here]NarrativeGenerator

CLI tool to generate XHTML narratives for FHIR R4 resources using HAPI FHIR’s CustomThymeleafNarrativeGenerator and Thymeleaf templates.

### Features
- Generates `Resource.text.div` XHTML via Thymeleaf templates
- Supports JSON and XML input (auto-detected)
- Template mapping via a simple manifest file
- HAPI FHIR version controlled with a single property

### Requirements
- Java 21+
- Maven 3.8+

### Build
- `mvn -DskipTests package`

Produces `target/NarrativeGenerator-1.0-SNAPSHOT.jar` with `Main-Class` set.

### Run (CLI)
- `java -jar target/NarrativeGenerator-1.0-SNAPSHOT.jar <path/to/resource.json|xml>`
- Prints generated XHTML narrative to stdout
- Exit codes:
  - 1: usage
  - 2: input file not found
  - 3: no narrative generated (missing template/empty)
  - 4: I/O error
  - 5: other error

### Templates
- Templates live under `src/main/resources/templates`
- Manifest: `src/main/resources/narratives/manifest.properties`
- Example mapping (Patient):
  ```properties
  patient.resourceType=Patient
  patient.narrative=classpath:templates/Patient.html
  patient.style=THYMELEAF
  ```
- Add a new resource template (e.g., Observation):
  1) Create `src/main/resources/templates/Observation.html`
  2) Add to manifest:
     ```properties
     observation.resourceType=Observation
     observation.narrative=classpath:templates/Observation.html
     observation.style=THYMELEAF
     ```

#### Template notes
- Thymeleaf evaluates against the HAPI resource as `resource`.
- In HAPI 8.x, prefer direct model access over custom helpers. Example (Patient):
  ```html
  <div xmlns="http://www.w3.org/1999/xhtml">
    <h2 th:text="'Patient: ' + ${resource.id}">Patient</h2>
    <p>
      <strong>Name:</strong>
      <span th:text="${resource.name.get(0).given.get(0).value}">Given</span>
      <span th:text="${resource.name.get(0).family}">Family</span>
    </p>
    <p>
      <strong>Geschlecht:</strong>
      <span th:text="${resource.gender.toCode()}">unknown</span>
    </p>
    <p>
      <strong>Geburtsdatum:</strong>
      <span th:text="${resource.birthDateElement.valueAsString}">YYYY-MM-DD</span>
    </p>
  </div>
  ```

### Programmatic wiring
- Main entry: `src/main/java/eu/molit/fhir/narrative/Main.java`
- Uses:
  - `FhirContext.forR4()`
  - `CustomThymeleafNarrativeGenerator`
  - `NarrativeTemplateManifest.forManifestFileLocation(new String[]{"classpath:/narratives/manifest.properties"})`
  - `gen.setManifest(manifest)`
  - `ctx.getNarrativeGenerator().generateResourceNarrative(ctx, resource)` → returns XHTML

### Testing
- JUnit 5 test: `src/test/java/eu/molit/fhir/narrative/NarrativeGeneratorTest.java`
- Run tests: `mvn test`

### Configuration
- HAPI version is centralized:
  - `pom.xml` property: `hapi.version=8.4.0`
  - Dependencies `hapi-fhir-base`, `hapi-fhir-structures-r4`, and `hapi-fhir-caching-caffeine` use `${hapi.version}`
- Thymeleaf and supporting dependencies are declared in `pom.xml`.

### Example (Traditional JAR)
Given `patient.json`:
```json
{
  "resourceType": "Patient",
  "name": [{"given": ["Erika"], "family": "Musterfrau"}],
  "gender": "female",
  "birthDate": "1970-01-01"
}
```
Run:
```bash
java -jar target/NarrativeGenerator-1.0-SNAPSHOT.jar patient.json
```
Output (XHTML snippet):
```html
<div xmlns="http://www.w3.org/1999/xhtml">
  <h2>Patient: Patient/...</h2>
  <p><strong>Name:</strong> Erika Musterfrau</p>
  <p><strong>Geschlecht:</strong> female</p>
  <p><strong>Geburtsdatum:</strong> 1970-01-01</p>
  </div>
```
