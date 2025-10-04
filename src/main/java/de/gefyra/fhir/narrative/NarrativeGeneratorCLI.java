package de.gefyra.fhir.narrative;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.CustomThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative2.NarrativeTemplateManifest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * CLI application for ISiK Bundle narrative generation using PicoCLI.
 * 
 * Supports generating narratives for ISiK Bericht Bundles with multiple output options:
 * - Bundle narrative only
 * - Complete Bundle with populated narratives
 * - Bundle with narratives transferred to section.text
 */
@Command(
    name = "narrative-generator",
    description = "Generate narratives for ISiK Bericht Bundles",
    version = "1.0.0",
    mixinStandardHelpOptions = true
)
public class NarrativeGeneratorCLI implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "Input ISiK Bundle JSON file path"
    )
    private File inputFile;

    @Option(
        names = {"-o", "--output"},
        description = "Output file path (default: stdout)"
    )
    private File outputFile;

    @Option(
        names = {"-m", "--mode"},
        description = "Output mode: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})"
    )
    private OutputMode mode = OutputMode.BUNDLE_WITH_NARRATIVES;

    @Option(
        names = {"-p", "--pretty"},
        description = "Pretty print JSON output (default: ${DEFAULT-VALUE})"
    )
    private boolean prettyPrint = true;

    @Option(
        names = {"-v", "--verbose"},
        description = "Verbose output"
    )
    private boolean verbose = false;

    /**
     * Output modes for the narrative generation
     */
    public enum OutputMode {
        /** Output the complete Bundle with all entry narratives populated */
        BUNDLE_WITH_NARRATIVES,
        /** Output Bundle with narratives transferred to Composition section.text */
        BUNDLE_WITH_SECTION_TEXT
    }

    private FhirContext fhirContext;
    private CustomThymeleafNarrativeGenerator narrativeGenerator;

    @Override
    public Integer call() throws Exception {
        try {
            // Initialize FHIR context and narrative generator
            initializeFhirContext();

            // Validate input file
            if (!inputFile.exists() || !inputFile.isFile()) {
                System.err.println("Error: Input file does not exist or is not a file: " + inputFile);
                return 1;
            }

            // Load and parse Bundle
            Bundle bundle = loadBundle(inputFile.toPath());
            if (bundle == null) {
                System.err.println("Error: Could not parse FHIR Bundle from input file");
                return 1;
            }

            // Validate Bundle type
            if (!Bundle.BundleType.DOCUMENT.equals(bundle.getType())) {
                System.err.println("Warning: Bundle type is not DOCUMENT, but " + bundle.getType());
            }

            // Generate output based on mode
            String output = generateOutput(bundle, mode);
            
            // Write output
            writeOutput(output);

            if (verbose) {
                System.err.println("Successfully processed Bundle with " + bundle.getEntry().size() + " entries");
                System.err.println("Output mode: " + mode);
            }

            return 0;

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    /**
     * Initialize FHIR context with custom narrative generator
     */
    private void initializeFhirContext() {
        fhirContext = FhirContext.forR4();
        narrativeGenerator = new CustomThymeleafNarrativeGenerator();
        NarrativeTemplateManifest manifest = NarrativeTemplateManifest.forManifestFileLocation(
            new String[]{"classpath:/narratives/manifest.properties"}
        );
        narrativeGenerator.setManifest(manifest);
        fhirContext.setNarrativeGenerator(narrativeGenerator);

        if (verbose) {
            System.err.println("Initialized FHIR R4 context with custom narrative generator");
        }
    }

    /**
     * Load Bundle from JSON file
     */
    private Bundle loadBundle(Path filePath) throws Exception {
        String content = Files.readString(filePath);
        IBaseResource resource = fhirContext.newJsonParser().parseResource(content);
        
        if (!(resource instanceof Bundle)) {
            throw new IllegalArgumentException("Input file does not contain a FHIR Bundle");
        }
        
        return (Bundle) resource;
    }

    /**
     * Generate output based on the specified mode
     */
    private String generateOutput(Bundle bundle, OutputMode mode) throws Exception {
        switch (mode) {
            case BUNDLE_WITH_NARRATIVES:
                return generateBundleWithNarratives(bundle);
                
            case BUNDLE_WITH_SECTION_TEXT:
                return generateBundleWithSectionText(bundle);
                
            default:
                throw new IllegalArgumentException("Unsupported output mode: " + mode);
        }
    }

    /**
     * Generate Bundle with all entry narratives populated
     */
    private String generateBundleWithNarratives(Bundle bundle) throws Exception {
        // Pre-generate narratives for entries
        preGenerateBundleEntryNarratives(bundle);
        
        // Return complete Bundle as JSON
        return prettyPrint ? 
            fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle) :
            fhirContext.newJsonParser().encodeResourceToString(bundle);
    }

    /**
     * Generate Bundle with narratives transferred to Composition section.text
     */
    private String generateBundleWithSectionText(Bundle bundle) throws Exception {
        // Pre-generate narratives for entries
        preGenerateBundleEntryNarratives(bundle);
        
        // Find Composition in Bundle
        Composition composition = bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Composition)
            .map(e -> (Composition) e.getResource())
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No Composition found in Bundle"));

        // Transfer narratives to section.text
        transferNarrativesToSections(bundle, composition);
        
        // Return complete Bundle as JSON (Bundle doesn't have text field)
        return prettyPrint ? 
            fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle) :
            fhirContext.newJsonParser().encodeResourceToString(bundle);
    }

    /**
     * Pre-generate narratives for all Bundle entries (as done in Main.java)
     */
    private void preGenerateBundleEntryNarratives(Bundle bundle) {
        bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r != null)
            .forEach(r -> {
                try {
                    String narrative = fhirContext.getNarrativeGenerator().generateResourceNarrative(fhirContext, r);
                    if (r instanceof DomainResource dr && narrative != null && !narrative.isBlank()) {
                        dr.getText().setStatus(Narrative.NarrativeStatus.GENERATED);
                        dr.getText().setDivAsString(narrative);
                        
                        if (verbose) {
                            System.err.println("Generated narrative for " + r.getResourceType() + "/" + r.getId() + 
                                             " (" + narrative.length() + " chars)");
                        }
                    }
                } catch (Exception e) { 
                    System.err.println("Warning: Failed to generate narrative for " + 
                                     r.getResourceType() + "/" + r.getId() + ": " + e.getMessage());
                    if (verbose) {
                        e.printStackTrace();
                    }
                }
            });
    }

    /**
     * Transfer resource narratives to Composition sections while keeping original resource narratives
     */
    private void transferNarrativesToSections(Bundle bundle, Composition composition) {
        // First, handle existing sections and populate their narratives from referenced resources
        for (Composition.SectionComponent section : composition.getSection()) {
            if (section.hasEntry() && (!section.hasText() || !section.getText().hasDiv())) {
                StringBuilder sectionNarrative = new StringBuilder();
                boolean foundNarratives = false;
                
                for (Reference entryRef : section.getEntry()) {
                    // Find the referenced resource in the bundle
                    String resourceId = entryRef.getReference();
                    for (Bundle.BundleEntryComponent bundleEntry : bundle.getEntry()) {
                        if (bundleEntry.getResource() instanceof DomainResource domainResource) {
                            String resourceReference = domainResource.getId();
                            String uuidReference = "urn:uuid:" + domainResource.getId();
                            String typeReference = domainResource.getResourceType().name() + "/" + domainResource.getId();
                            
                            if (resourceId.equals(resourceReference) || 
                                resourceId.equals(uuidReference) ||
                                resourceId.equals(typeReference)) {
                                
                                if (domainResource.hasText() && domainResource.getText().hasDiv()) {
                                    if (foundNarratives) {
                                        sectionNarrative.append("<hr/>");
                                    }
                                    sectionNarrative.append(domainResource.getText().getDivAsString());
                                    foundNarratives = true;
                                    
                                    if (verbose) {
                                        System.err.println("Added narrative from " + domainResource.getResourceType() + "/" + domainResource.getId() + 
                                                         " to existing section '" + section.getTitle() + "'");
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Set the combined narrative for this section
                if (foundNarratives) {
                    Narrative sectionText = new Narrative();
                    sectionText.setStatus(Narrative.NarrativeStatus.GENERATED);
                    sectionText.setDivAsString("<div xmlns=\"http://www.w3.org/1999/xhtml\">" + sectionNarrative.toString() + "</div>");
                    section.setText(sectionText);
                }
            }
        }
        
        // Then, create new sections for resources that don't have sections yet
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof DomainResource dr && 
                dr.hasText() && dr.getText().hasDiv() &&
                !(dr instanceof Composition)) {
                
                // Check if this resource is already referenced in an existing section
                boolean alreadyReferenced = false;
                for (Composition.SectionComponent section : composition.getSection()) {
                    for (Reference entryRef : section.getEntry()) {
                        String resourceId = entryRef.getReference();
                        String resourceReference = dr.getId();
                        String uuidReference = "urn:uuid:" + dr.getId();
                        String typeReference = dr.getResourceType().name() + "/" + dr.getId();
                        
                        if (resourceId.equals(resourceReference) || 
                            resourceId.equals(uuidReference) ||
                            resourceId.equals(typeReference)) {
                            alreadyReferenced = true;
                            break;
                        }
                    }
                    if (alreadyReferenced) break;
                }
                
                // Only create new section if resource is not already referenced
                if (!alreadyReferenced) {
                    Composition.SectionComponent section = new Composition.SectionComponent();
                    section.setTitle(dr.getResourceType().name());
                    
                    // Set section text from resource narrative (copy, don't move)
                    Narrative sectionText = new Narrative();
                    sectionText.setStatus(Narrative.NarrativeStatus.GENERATED);
                    sectionText.setDivAsString(dr.getText().getDivAsString());
                    section.setText(sectionText);
                    
                    // Add resource reference
                    section.addEntry(new Reference(dr.getResourceType().name() + "/" + dr.getId()));
                    
                    composition.addSection(section);
                    
                    if (verbose) {
                        System.err.println("Transferred narrative from " + dr.getResourceType() + "/" + dr.getId() + 
                                         " to new Composition section (resource narrative preserved)");
                    }
                }
            }
        }
    }

    /**
     * Write output to file or stdout
     */
    private void writeOutput(String output) throws Exception {
        if (outputFile != null) {
            Files.writeString(outputFile.toPath(), output);
            if (verbose) {
                System.err.println("Output written to: " + outputFile);
            }
        } else {
            System.out.println(output);
        }
    }

    /**
     * Main entry point
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new NarrativeGeneratorCLI()).execute(args);
        System.exit(exitCode);
    }
}