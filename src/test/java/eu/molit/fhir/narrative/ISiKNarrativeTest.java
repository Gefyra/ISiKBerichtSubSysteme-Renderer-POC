package eu.molit.fhir.narrative;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ISiK (Interoperabilitätsstandards im Krankenhaus) specific narrative generation.
 * These tests focus on German healthcare standards and the Intensivstation (ICU) Bundle example.
 */
class ISiKNarrativeTest extends NarrativeTestBase {

    @Test
    void generatesNarrativeForISiKPatient() throws Exception {
        // Arrange: Load the bundle and extract the patient
        String content = loadTestResource("BundleExampleIntensivstation.json");
        FhirContext ctx = createConfiguredFhirContext();
        Bundle bundle = (Bundle) ctx.newJsonParser().parseResource(content);

        Patient patient = bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Patient)
            .map(e -> (Patient) e.getResource())
            .findFirst()
            .orElseThrow(() -> new AssertionError("No Patient found in ISiK bundle"));

        // Act: Generate patient narrative
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, patient);

        // Assert: Check ISiK patient narrative content
        assertNotNull(html, "Patient narrative should be generated");
        assertTrue(html.contains("Thomas"), "Should contain patient's given name");
        assertTrue(html.contains("Müller") || html.contains("MÜLLER") || html.contains("M&#xdc;LLER"), 
                  "Should contain patient's family name (raw or HTML-encoded)");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML");
    }

    @Test
    void generatesNarrativeForISiKBloodPressureObservation() throws Exception {
        // Arrange: Load the bundle and extract the blood pressure observation
        String content = loadTestResource("BundleExampleIntensivstation.json");
        FhirContext ctx = createConfiguredFhirContext();
        Bundle bundle = (Bundle) ctx.newJsonParser().parseResource(content);

        Observation bloodPressureObs = bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Observation)
            .map(e -> (Observation) e.getResource())
            .filter(obs -> obs.getCode() != null && 
                          obs.getCode().getCoding().stream()
                              .anyMatch(coding -> "85354-9".equals(coding.getCode())))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No BloodPressureObservation found in ISiK bundle"));

        // Act: Generate observation narrative
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, bloodPressureObs);

        // Assert: Check blood pressure narrative content
        assertNotNull(html, "Blood pressure observation narrative should be generated");
        assertTrue(html.contains("Systolisch") || html.contains("210"), 
                  "Should contain systolic blood pressure information");
        assertTrue(html.contains("Diastolisch") || html.contains("115"), 
                  "Should contain diastolic blood pressure information");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML");
    }

    @Test
    void generatesNarrativeForISiKComposition() throws Exception {
        // Arrange: Load the bundle and extract the composition
        String content = loadTestResource("BundleExampleIntensivstation.json");
        FhirContext ctx = createConfiguredFhirContext();
        Bundle bundle = (Bundle) ctx.newJsonParser().parseResource(content);

        Composition composition = bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Composition)
            .map(e -> (Composition) e.getResource())
            .findFirst()
            .orElseThrow(() -> new AssertionError("No Composition found in ISiK bundle"));

        // Act: Generate composition narrative
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, composition);

        // Assert: Check composition narrative content
        assertNotNull(html, "Composition narrative should be generated");
        assertTrue(html.contains("Verlegungsbericht") || html.contains("Intensivstation"), 
                  "Should contain German clinical document terms");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML");
    }

    @Test 
    void validateISiKBundleHasExpectedProfiles() throws Exception {
        // Arrange: Load the ISiK bundle
        String content = loadTestResource("BundleExampleIntensivstation.json");
        FhirContext ctx = createConfiguredFhirContext();
        Bundle bundle = (Bundle) ctx.newJsonParser().parseResource(content);

        // Assert: Check that ISiK profiles are present
        boolean hasISiKBundleProfile = bundle.getMeta().getProfile().stream()
            .anyMatch(profile -> profile.getValue().contains("ISiKBerichtBundle"));
        
        assertTrue(hasISiKBundleProfile, "Bundle should have ISiK Bericht Bundle profile");

        // Check for ISiK patient profile
        boolean hasISiKPatientProfile = bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Patient)
            .map(e -> (Patient) e.getResource())
            .anyMatch(p -> p.getMeta().getProfile().stream()
                .anyMatch(profile -> profile.getValue().contains("ISiKPatient")));
        
        assertTrue(hasISiKPatientProfile, "Patient should have ISiK Patient profile");
    }
}