package eu.molit.fhir.narrative;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Patient resource narrative generation.
 */
class PatientNarrativeTest extends NarrativeTestBase {

    @Test
    void generatesPatientNarrativeFromTemplate() {
        // Arrange: FHIR context + Patient
        FhirContext ctx = createConfiguredFhirContext();

        Patient patient = new Patient();
        patient.addName(new HumanName().addGiven("Erika").setFamily("Musterfrau"));
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        patient.setBirthDateElement(new DateType("1970-01-01"));

        // Act: generate narrative
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, patient);

        // Debug: Print the generated HTML to see what's actually in it
        System.out.println("Generated Patient HTML: " + html);

        // Assert: narrative exists and includes expected content
        assertNotNull(html, "Narrative html should be returned");
        assertTrue(html.contains("Erika"), "Narrative should include given name");
        assertTrue(html.contains("Musterfrau") || html.contains("MUSTERFRAU"), "Narrative should include family name");
        assertTrue(html.contains("Geburtsdatum"), "Narrative should include label from template");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Narrative should be XHTML");
    }

    @Test
    void generatesPatientNarrativeWithMultipleGivenNames() {
        // Arrange
        FhirContext ctx = createConfiguredFhirContext();

        Patient patient = new Patient();
        patient.addName(new HumanName()
            .addGiven("Thomas")
            .addGiven("Wilhelm")
            .setFamily("MÜLLER"));
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setBirthDateElement(new DateType("1965-04-11"));

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, patient);

        // Assert
        assertNotNull(html, "Narrative should be generated");
        assertTrue(html.contains("Thomas"), "Should contain first given name");
        assertTrue(html.contains("Wilhelm"), "Should contain second given name");
        assertTrue(html.contains("MÜLLER") || html.contains("M&#xdc;LLER") || html.contains("Müller"), 
                  "Should contain family name (raw or HTML-encoded)");
        assertTrue(html.contains("male"), "Should contain gender");
    }

    @Test
    void generatesPatientNarrativeWithMissingOptionalFields() {
        // Arrange: Patient with minimal data
        FhirContext ctx = createConfiguredFhirContext();

        Patient patient = new Patient();
        patient.addName(new HumanName().setFamily("Doe"));
        // No given name, no gender, no birth date

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, patient);

        // Debug: Print the generated HTML
        System.out.println("Generated HTML for minimal Patient: " + html);

        // Assert: should still generate narrative without errors
        assertNotNull(html, "Narrative should be generated even with minimal data");
        assertTrue(html.contains("Doe") || html.contains("DOE"), "Should contain family name");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML");
    }
}