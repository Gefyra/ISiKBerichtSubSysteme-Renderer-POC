package eu.molit.fhir.narrative;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.CustomThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative2.NarrativeTemplateManifest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;

/**
 * Base class for narrative generation tests providing common setup and utility methods.
 */
public abstract class NarrativeTestBase {

    /**
     * Creates a fully configured FHIR context with the custom narrative generator.
     * 
     * @return FhirContext configured with CustomThymeleafNarrativeGenerator
     */
    protected FhirContext createConfiguredFhirContext() {
        FhirContext ctx = FhirContext.forR4();
        CustomThymeleafNarrativeGenerator gen = new CustomThymeleafNarrativeGenerator();
        NarrativeTemplateManifest manifest = NarrativeTemplateManifest.forManifestFileLocation(
            new String[]{"classpath:/narratives/manifest.properties"}
        );
        gen.setManifest(manifest);
        ctx.setNarrativeGenerator(gen);
        return ctx;
    }

    /**
     * Pre-generates narratives for all entries in a Bundle (as done in Main.java).
     * This sets the text.div for each DomainResource in the Bundle entries.
     * 
     * @param ctx the FHIR context with narrative generator
     * @param bundle the Bundle to process
     */
    protected void preGenerateBundleEntryNarratives(FhirContext ctx, Bundle bundle) {
        bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r != null)
            .forEach(r -> {
                try {
                    String narrative = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, r);
                    if (r instanceof DomainResource dr && narrative != null && !narrative.isBlank()) {
                        dr.getText().setDivAsString(narrative);
                    }
                } catch (Exception e) { 
                    System.err.println("Warning: Failed to generate narrative for " + 
                                     r.getResourceType() + "/" + r.getId() + ": " + e.getMessage());
                }
            });
    }

    /**
     * Checks if any resource in the Bundle has a generated narrative.
     * 
     * @param bundle the Bundle to check
     * @return true if at least one resource has a narrative
     */
    protected boolean hasAnyResourceNarrative(Bundle bundle) {
        return bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof DomainResource)
            .map(e -> (DomainResource) e.getResource())
            .anyMatch(dr -> dr.hasText() && dr.getText().hasDiv());
    }

    /**
     * Loads a JSON resource file from the test resources directory.
     * 
     * @param filename the filename (e.g., "BundleExample.json")
     * @return the file content as String
     * @throws Exception if file cannot be loaded
     */
    protected String loadTestResource(String filename) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        java.net.URL url = cl.getResource(filename);
        if (url == null) {
            throw new IllegalArgumentException("Test resource not found: " + filename);
        }
        return new String(
            java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(url.toURI())), 
            java.nio.charset.StandardCharsets.UTF_8
        );
    }
}