package org.datim.plm.bundleMaker;

import java.util.Arrays;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;

public class ResultBundle {

  private Bundle bundle = new Bundle();

  public ResultBundle(String profileName){
    bundle.setType(BundleType.MESSAGE);
    UUID uuid = UUID.randomUUID();
    bundle.setId(uuid.toString());

    bundle.setTimestamp(new java.util.Date());
    Narrative narrative = new Narrative();
    narrative.setDivAsString("This is an auto-generated resource bundle.");
    Resource r = new MessageHeader().setText(narrative);
    bundle.addEntry(new BundleEntryComponent().setResource(r));
    
    // TODO dynamic MDS profile identifier
    bundle.getMeta().setProfile(Arrays.asList(new CanonicalType(profileName)));
  }

  public Bundle getBundle(){
    return this.bundle;
  }
}