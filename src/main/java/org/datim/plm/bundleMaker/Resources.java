package org.datim.plm.bundleMaker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

// DEPRICATED
public class Resources {

  java.util.HashMap<String,Observation> observations = new java.util.HashMap<String,Observation>();
  Patient patient = null;
  Condition condition = null;
  MedicationStatement medicationStatement = null;
  Encounter encounter = null;
  String provenanceUUID = null;
  String csvFileName = null;
  
  public Resources(String provenanceUUID, String csvFileName) {
    this.provenanceUUID = provenanceUUID;
    this.csvFileName = csvFileName;
  }
  
  /**
   * @return the observation
   */
  public Observation getObservation(String linkId) {
    if(observations.get(linkId) == null) {
      Observation o = new Observation();
      o.setId(UUID.randomUUID().toString());
      observations.put(linkId, o);
    }
    return observations.get(linkId);
  }
  
  /**
   * @return the patient
   */
  public Patient getPatient() {
    if(patient == null)
      patient = new Patient();
    Meta m = new Meta();
    m.addTag("http://openclientregistry.org/fhir/tag/csv", this.provenanceUUID, this.csvFileName);
// TODO needs to be dynamic
    m.addTag("http://openclientregistry.org/fhir/clientid", "dhis2", "DHIS2");

    patient.setMeta(m);
    return patient;
  }
  /**
   * @return the condition
   */
  public Condition getCondition() {
    if(condition == null) {
      condition = new Condition();
      condition.setId(UUID.randomUUID().toString());
    }
    return condition;
  }
  /**
   * @return the medicationStatement
   */
  public MedicationStatement getMedicationStatement() {
    if(medicationStatement == null) {
      medicationStatement = new MedicationStatement();
      medicationStatement.setId(UUID.randomUUID().toString());
    }
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

  private String getPatientSystemId() {
    // TODO how do you pick which identifier to use if more than one is present?
    if(patient.getIdentifier().size() > 0)
    //  return patient.getIdentifier().get(0).getSystem() + "." + 
      return patient.getIdentifier().get(0).getValue();
    else
      return UUID.randomUUID().toString();
    
  }
  
  public void addToBundle(Bundle bundle){
    patient.setId(getPatientSystemId());
    if(encounter != null && encounter.getId() == null)
      encounter.setId(UUID.randomUUID().toString());

    // cross reference to subject
    if(condition != null){
      condition.setSubject(new Reference("Patient/" + patient.getId()));
    }
    if(medicationStatement != null){
      medicationStatement.setSubject(new Reference("Patient/" + patient.getId()));
    }
    if(observations.size() > 0){
      for(Observation o : observations.values()) {
        o.setSubject(new Reference("Patient/" + patient.getId()));
        o.setEncounter(new Reference("Encounter/" + encounter.getId()));
      }
    }
    if(encounter!= null){
      encounter.setSubject(new Reference("Patient/" + patient.getId()));
    }

    // TODO temporary - need to find a way to get it out of HAPI
    @SuppressWarnings("rawtypes")
    HashMap<Class, String> resourceToUrl = new HashMap<Class, String>();
    resourceToUrl.put(Patient.class, "Patient");
    resourceToUrl.put(Condition.class, "Condition");
    resourceToUrl.put(Observation.class, "Observation");
    resourceToUrl.put(Encounter.class, "Encounter");
    resourceToUrl.put(MedicationStatement.class, "MedicationStatement");
    
    List<Resource> all = Arrays.asList(patient, encounter, condition, medicationStatement);
    all.forEach(r -> {if(r != null) bundle.addEntry(new BundleEntryComponent().setResource(r));});
//    BundleEntryComponent bec = new BundleEntryComponent();
    observations.values().forEach(r -> {bundle.addEntry(new BundleEntryComponent().setResource(r));});
    
    for(BundleEntryComponent be : bundle.getEntry()) {
      if(be.getResource() instanceof MessageHeader)
        continue;
      BundleEntryRequestComponent berc = new BundleEntryRequestComponent();
      be.setRequest(berc);
      berc.setMethod(HTTPVerb.PUT);
      berc.setUrl(resourceToUrl.get(be.getResource().getClass()) + "/" + be.getResource().getId());
    }
  }
}
