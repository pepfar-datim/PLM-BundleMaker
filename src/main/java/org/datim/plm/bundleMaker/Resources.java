package org.datim.plm.bundleMaker;

import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

public class Resources {

  // TODO use UUIDs instead
  static int patientId = 0, observationId = 0, conditionId = 0, medicationStatementId = 0, encounterId = 0;
  
  Observation observation = null;
  Patient patient = null;
  Condition condition = null;
  MedicationStatement medicationStatement = null;
  Encounter encounter = null;

  /**
   * @return the observation
   */
  public Observation getObservation() {
    if(observation == null)
      observation = new Observation();
    return observation;
  }
  
  /**
   * @return the patient
   */
  public Patient getPatient() {
    if(patient == null)
      patient = new Patient();
    return patient;
  }
  /**
   * @return the condition
   */
  public Condition getCondition() {
    if(condition == null)
      condition = new Condition();
    return condition;
  }
  /**
   * @return the medicationStatement
   */
  public MedicationStatement getMedicationStatement() {
    if(medicationStatement == null)
      medicationStatement = new MedicationStatement();
    return medicationStatement;
  }
  /**
   * @return the encounter
   */
  public Encounter getEncounter() {
    if(encounter == null)
      encounter = new Encounter();
    return encounter;
  }

  public void addToBundle(Bundle bundle){

    if(patient.getId() == null)
      patient.setId("" + patientId++);
    if(encounter != null && encounter.getId() == null)
      encounter.setId("" + encounterId++);

    // cross reference to subject
    if(condition != null){
      condition.setSubject(new Reference("Patient/" + patient.getId()));
    }
    if(medicationStatement != null){
      medicationStatement.setSubject(new Reference("Patient/" + patient.getId()));
    }
    if(observation != null){
      observation.setSubject(new Reference("Patient/" + patient.getId()));
      observation.setEncounter(new Reference("Encounter/" + encounter.getId()));
    }
    if(encounter!= null){
      encounter.setSubject(new Reference("Patient/" + patient.getId()));
    }

    List<Resource> all = Arrays.asList(patient, encounter, observation, condition, medicationStatement);
    all.forEach(r -> {if(r != null) bundle.addEntry(new BundleEntryComponent().setResource(r));});
  }
}
