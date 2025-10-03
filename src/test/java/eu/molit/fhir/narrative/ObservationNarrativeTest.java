package eu.molit.fhir.narrative;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Observation resource narrative generation.
 */
class ObservationNarrativeTest extends NarrativeTestBase {

    @Test
    void generatesNarrativeForSimpleObservation() {
        // Arrange: Simple observation with value
        FhirContext ctx = createConfiguredFhirContext();
        
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode("29463-7")
                .setDisplay("Body Weight")));
        observation.setValue(new Quantity()
            .setValue(70)
            .setUnit("kg")
            .setSystem("http://unitsofmeasure.org")
            .setCode("kg"));
        observation.setEffective(new DateTimeType("2025-01-01T10:00:00+01:00"));

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, observation);

        // Assert
        assertNotNull(html, "Observation narrative should be generated");
        assertTrue(html.contains("70") || html.contains("kg"), "Should contain the measurement value");
        assertTrue(html.contains("FINAL"), "Should contain the status");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML");
    }

    @Test
    void generatesNarrativeForBloodPressureObservation() {
        // Arrange: Blood pressure observation with components
        FhirContext ctx = createConfiguredFhirContext();
        
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode("85354-9")
                .setDisplay("Blood pressure panel")));

        // Add systolic component
        observation.addComponent()
            .setCode(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem("http://loinc.org")
                    .setCode("8480-6")
                    .setDisplay("Systolic blood pressure")))
            .setValue(new Quantity()
                .setValue(120)
                .setUnit("mmHg")
                .setSystem("http://unitsofmeasure.org")
                .setCode("mm[Hg]"));

        // Add diastolic component
        observation.addComponent()
            .setCode(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem("http://loinc.org")
                    .setCode("8462-4")
                    .setDisplay("Diastolic blood pressure")))
            .setValue(new Quantity()
                .setValue(80)
                .setUnit("mmHg")
                .setSystem("http://unitsofmeasure.org")
                .setCode("mm[Hg]"));

        observation.setEffective(new DateTimeType("2025-01-01T10:00:00+01:00"));

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, observation);

        // Assert
        assertNotNull(html, "Blood pressure observation narrative should be generated");
        assertTrue(html.contains("120") || html.contains("Systolisch"), 
                  "Should contain systolic blood pressure");
        assertTrue(html.contains("80") || html.contains("Diastolisch"), 
                  "Should contain diastolic blood pressure");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML");
    }

    @Test
    void generatesNarrativeForObservationWithMinimalData() {
        // Arrange: Observation with minimal required data
        FhirContext ctx = createConfiguredFhirContext();
        
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode("1234-5")));

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, observation);

        // Assert: should handle minimal observation gracefully
        assertNotNull(html, "Observation narrative should be generated even with minimal data");
        assertTrue(html.contains("FINAL"), "Should contain the status");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML");
    }

    @Test
    void generatesNarrativeForObservationWithCategory() {
        // Arrange: Observation with category
        FhirContext ctx = createConfiguredFhirContext();
        
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode("33747-0")
                .setDisplay("General appearance")));
        observation.addCategory(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("Vital Signs")));

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, observation);

        // Assert
        assertNotNull(html, "Observation narrative should be generated");
        assertTrue(html.contains("vital-signs") || html.contains("Vital Signs"), 
                  "Should contain category information");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML");
    }
}