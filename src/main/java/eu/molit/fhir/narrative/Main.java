package eu.molit.fhir.narrative;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.CustomThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative2.NarrativeTemplateManifest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Bundle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            printUsageAndExit();
        }

        Path in = Path.of(args[0]);
        if (!Files.isRegularFile(in)) {
            System.err.println("Input file not found: " + in);
            System.exit(2);
        }

        // Create R4 FHIR context and register our custom narrative generator
        FhirContext ctx = FhirContext.forR4();
        CustomThymeleafNarrativeGenerator gen = new CustomThymeleafNarrativeGenerator();
        NarrativeTemplateManifest manifest = NarrativeTemplateManifest.forManifestFileLocation(new String[]{"classpath:/narratives/manifest.properties"});
        gen.setManifest(manifest);
        ctx.setNarrativeGenerator(gen);

        try {
            String content = Files.readString(in, StandardCharsets.UTF_8);
            boolean isJson = looksLikeJson(content);
            IBaseResource resource = isJson
                ? ctx.newJsonParser().parseResource(content)
                : ctx.newXmlParser().parseResource(content);

            // If a Bundle, pre-generate narratives for common entry resources so their text.div is set
            if (resource instanceof Bundle b && b.hasEntry()) {
                b.getEntry().stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .filter(r -> r != null)
                    .forEach(r -> {
                        try {
                            String sub = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, r);
                            if (r instanceof DomainResource dr && sub != null && !sub.isBlank()) {
                                dr.getText().setDivAsString(sub);
                            }
                        } catch (Exception e) { 
                            System.err.println("Warning: Failed to generate narrative for " + r.getResourceType() + "/" + r.getId() + ": " + e.getMessage());
                            // Continue processing other resources
                        }
                    });
            }

            // Generate narrative and print it (returns HTML for the top-level resource)
            String xhtml = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, resource);
            if (xhtml == null || xhtml.isBlank()) {
                System.err.println("No narrative generated (template missing or empty result)");
                System.exit(3);
            }
            System.out.print(xhtml);
        } catch (IOException e) {
            System.err.println("Failed to read input: " + e.getMessage());
            System.exit(4);
        } catch (Exception e) {
            System.err.println("Error generating narrative: " + e.getMessage());
            System.exit(5);
        }
    }

    private static boolean looksLikeJson(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i < s.length() && s.charAt(i) == '{';
    }

    private static void printUsageAndExit() {
        System.out.println("Usage: java -jar narrative-generator.jar <input-fhir-file.json|.xml>");
        System.out.println("Reads a FHIR resource (R4) and prints generated XHTML narrative to stdout.");
        System.exit(1);
    }
}
