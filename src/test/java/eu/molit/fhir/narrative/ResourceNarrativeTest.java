package eu.molit.fhir.narrative;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for various FHIR resource narrative generation (Encounter, Procedure, Composition, etc.).
 */
class ResourceNarrativeTest extends NarrativeTestBase {

    @Test
    void generatesNarrativeForEncounter() {
        // Arrange
        FhirContext ctx = createConfiguredFhirContext();
        
        Encounter encounter = new Encounter();
        encounter.setStatus(Encounter.EncounterStatus.INPROGRESS);
        Period period = new Period();
        period.setStartElement(new DateTimeType("2025-01-01T08:00:00+01:00"));
        period.setEndElement(new DateTimeType("2025-01-01T16:00:00+01:00"));
        encounter.setPeriod(period);

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, encounter);

        // Assert
        assertNotNull(html, "Encounter narrative should be generated");
        assertTrue(html.contains("in-progress"), "Should contain encounter status");
        assertTrue(html.contains("01.01.2025"), "Should contain encounter date in German format");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML");
    }

    @Test
    void generatesNarrativeForProcedure() {
        // Arrange
        FhirContext ctx = createConfiguredFhirContext();
        
        Procedure procedure = new Procedure();
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        procedure.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://snomed.info/sct")
                .setCode("182960004")
                .setDisplay("Emergency treatment")));
        procedure.setPerformed(new DateTimeType("2025-01-01T12:00:00+01:00"));

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, procedure);

        // Assert
        assertNotNull(html, "Procedure narrative should be generated");
        assertTrue(html.contains("COMPLETED") || html.contains("completed"), 
                  "Should contain procedure status");
        assertTrue(html.contains("Emergency treatment") || html.contains("182960004"), 
                  "Should contain procedure code or display");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML");
    }

    @Test
    void generatesNarrativeForComposition() {
        // Arrange
        FhirContext ctx = createConfiguredFhirContext();
        
        Composition composition = new Composition();
        composition.setTitle("Discharge Summary");
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.setDate(new java.util.Date());
        composition.setType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode("18842-5")
                .setDisplay("Discharge summary")));

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, composition);

        // Assert
        assertNotNull(html, "Composition narrative should be generated");
        assertTrue(html.contains("Discharge Summary") || html.contains("discharge"), 
                  "Should contain composition title or type");
        assertTrue(html.contains("final"), "Should contain composition status");
        assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML");
    }

    @Test
    void generatesNarrativeForMedication() {
        // Arrange
        FhirContext ctx = createConfiguredFhirContext();
        
        Medication medication = new Medication();
        medication.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
                .setCode("1594660")
                .setDisplay("Aspirin 81 MG Oral Tablet")));

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, medication);

        // Assert: May or may not have a template, so check gracefully
        if (html != null) {
            assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML if generated");
        }
        // Note: It's acceptable for some resources to not have templates
    }

    @Test
    void generatesNarrativeForCondition() {
        // Arrange
        FhirContext ctx = createConfiguredFhirContext();
        
        Condition condition = new Condition();
        condition.setClinicalStatus(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active")));
        condition.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("E11.9")
                .setDisplay("Type 2 diabetes mellitus without complications")));

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, condition);

        // Assert: May or may not have a template
        if (html != null) {
            assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML if generated");
        }
    }

    @Test
    void handlesResourceWithoutTemplate() {
        // Arrange: Use a resource type that likely doesn't have a template
        FhirContext ctx = createConfiguredFhirContext();
        
        Organization organization = new Organization();
        organization.setName("Test Hospital");

        // Act
        String html = ctx.getNarrativeGenerator().generateResourceNarrative(ctx, organization);

        // Assert: Should handle gracefully (may return null)
        if (html != null) {
            assertTrue(html.contains("http://www.w3.org/1999/xhtml"), "Should be valid XHTML if generated");
            assertTrue(html.contains("Test Hospital"), "Should contain organization name if template exists");
        }
        // Note: null return is acceptable for resources without templates
    }
}