package org.datim.plm.bundleMaker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceFactory;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;

public class QuestionnaireResponseParser {

  private Questionnaire baseQuestionnaire;

  public QuestionnaireResponseParser(Questionnaire questionnaire) throws FileNotFoundException {
    this.baseQuestionnaire = questionnaire;
  }

  public ArrayList<Resource> extractFrom(QuestionnaireResponse questionnaireResponse) {
    // multi-component observation vs single; multiple instances of same resources within questionnaire 
    // include code from questionnaire
    ArrayList<Resource> resources = new ArrayList<Resource>();
    
    for(QuestionnaireResponseItemComponent i : questionnaireResponse.getItem()) {
      parseItem(i, resources);
    }

    // if id not found, add a UUID
    for(Resource r : resources) {
      if(r.getId() == null || r.getId().equals(""))
        r.setId(UUID.randomUUID().toString());
    }

    
    // if there is patient resource, get id, and reference others from the same questionnaire to it
    Resource patient = null;
    for(Resource r : resources) {
      if(r.getResourceType().toString().equals("Patient")) {
        patient = r;
        break;
      }
    }
    if(patient != null) {
      Reference reference = new Reference("Patient/" + patient.getId());
      for(Resource r : resources) {
        if(r.getResourceType().toString().equals("Patient"))
          continue;
        r.setProperty("subject", reference);
      }
    }
    return resources;
  }
  
  private Resource getResource(ArrayList<Resource> resources, String type) {
    for(Resource r : resources) {
      if(r.getResourceType().toString().equals(type)) {
        return r;
      }
    }
    Resource r = ResourceFactory.createResource(type);
    resources.add(r);
    return r;
  }
  
  private void parseItem(QuestionnaireResponseItemComponent item, ArrayList<Resource> resources) {
    
    if(item.hasAnswer()) {
      QuestionnaireItemComponent questionnaireItem = findQuestionnaireItem(baseQuestionnaire, item.getLinkId());
      
      //TODO add checking for missing definitions
      String definition = questionnaireItem.getDefinition();
      
      String[] fragment = definition.substring(definition.lastIndexOf("#")+1).split("\\.");
      
      String resourceType = fragment[0];
      Resource resource = getResource(resources, resourceType);
      
      if(fragment.length < 2)
        throw new IllegalArgumentException("Questionnaire item does not have proper definition");

      Base base = resource;
      for(int i = 1; i < fragment.length; i++) {
        if(i < fragment.length - 1) {
          // add as a child, and make it the base
          base = base.addChild(fragment[i]);
        } else {
          String prop = fragment[i];
          Type value = getValue(item);
          if(prop.equals("id"))
            value = new IdType(value.toString());
          base.setProperty(prop, value);
        }
      }

      // if questionnaire.item has code, add it to the resource
      if(questionnaireItem.hasCode()) {
        CodeableConcept cc = new CodeableConcept();
        for(Coding c : questionnaireItem.getCode()) {
          cc.addCoding(c);
        }
        resource.setProperty("code", cc);
      }
    }
    for(QuestionnaireResponseItemComponent i : item.getItem()) {
      parseItem(i, resources);
    }
  }

  private QuestionnaireItemComponent findQuestionnaireItem(Questionnaire q, String linkid) {
    if(q.hasItem()) {
      for(QuestionnaireItemComponent i : q.getItem()) {
        QuestionnaireItemComponent r = findQuestionnaireItem(i, linkid);
        if(r != null)
          return r;
      }
    }
    return null;
  }
  
  private QuestionnaireItemComponent findQuestionnaireItem(QuestionnaireItemComponent item, String linkid) {
    if(item.hasItem()) {
      for(QuestionnaireItemComponent i : item.getItem()) {
        QuestionnaireItemComponent r = findQuestionnaireItem(i, linkid);
        if(r != null)
          return r;
      }
      return null;
    }
    if(item.getLinkId().equals(linkid))
      return item;
    else
      return null;
  }

  private static Type getValue(QuestionnaireResponseItemComponent r2) {
    QuestionnaireResponseItemAnswerComponent a = r2.getAnswerFirstRep();
    if (a.getValue() == null) {
      return null;
    }
    if (a.getValue() instanceof Coding) {
      return ((Coding) a.getValue()).getCodeElement();
    } else if (a.getValue() instanceof Quantity) {
      return (Quantity) a.getValue();
    } else if (a.getValue() instanceof Attachment) {
      return (Attachment) a.getValue();
    } else if (a.getValue() instanceof Reference) {
      return (Reference) a.getValue();
    } else if (a.getValue() instanceof DateTimeType) {
      return (DateTimeType) a.getValue();
    } else if (a.getValue() instanceof TimeType) {
      return (TimeType) a.getValue();
    } else if (a.getValue() instanceof DateType) {
      return (DateType) a.getValue();
    } else if (a.getValue() instanceof IntegerType) {
      return (IntegerType) a.getValue();
    } else if (a.getValue() instanceof StringType) {
      return (StringType) a.getValue();
    } else if (a.getValue() instanceof UriType) {
      return (UriType) a.getValue();
    } else if (a.getValue() instanceof BooleanType) {
      return (BooleanType) a.getValue();
    } else if (a.getValue() instanceof DecimalType) {
      return (DecimalType) a.getValue();
    } else {
      throw new IllegalArgumentException("Not sure how to handle: " + a.getValue().getClass());
    }
  }
  
  
  
  /**
   * 
   * @param content
   *          can be either a bundle of QuestionnaireResponse type resources or
   *          a single QuestionnaireResponse resource
   */
  public static ArrayList<QuestionnaireResponse> parseQuestionnaireResponses(IParser parser, String content)
      throws IOException {

    ArrayList<QuestionnaireResponse> questionnaireResponses = new ArrayList<QuestionnaireResponse>();
    JsonNode n = new ObjectMapper().readTree(content);
    try {
      Bundle b = parser.parseResource(Bundle.class, n.toString());
      for (BundleEntryComponent bec : b.getEntry()) {
        Resource entryResource = bec.getResource();
        if (entryResource instanceof QuestionnaireResponse) {
          questionnaireResponses.add((QuestionnaireResponse) entryResource);
        } else {
          System.out.printf("entryResource type is %s\n", entryResource.getClass().getName());
        System.err
            .println("Entry resource is not a QuestionnaireResponse " + entryResource.getResourceType().toString());
        }
      }
    } catch (DataFormatException dfe) {
      try {
        Resource entryResource = parser.parseResource(QuestionnaireResponse.class, n.toString());
        if (entryResource instanceof QuestionnaireResponse) {
          questionnaireResponses.add((QuestionnaireResponse) entryResource);
        } else {
          System.out.printf("entryResource type is %s\n", entryResource.getClass().getName());
        System.err
            .println("Entry resource is not a QuestionnaireResponse " + entryResource.getResourceType().toString());
        }
        // TODO need to raise as an error, as it is unexpected to see a single
        // resource that is not QR
        throw new RuntimeException("Resource type is invalid. Either QuestionnaireResponse or Bundle is expected.");
      } catch (DataFormatException dfe2) {
        throw new RuntimeException("Resource type is invalid. Either QuestionnaireResponse or Bundle is expected.");
      }
    }
    return questionnaireResponses;
  }
//
//  // process human name
//  public void setHumanName(QuestionnaireResponseItemComponent r, Patient p) {
//    HumanName hn = new HumanName();
//    p.addName(hn);
//    for (QuestionnaireResponseItemComponent r2 : r.getItem()) {
//      // TODO - fix - it needs to be dynamic and generic
//      QuestionnaireResponseItemAnswerComponent a = r2.getAnswerFirstRep();
//      String v = a.getValue().primitiveValue();
//      if (r2.getLinkId().contains("given")) {
//        hn.addGiven(v);
//      } else if (r2.getLinkId().contains("family")) {
//        hn.setFamily(v);
//      } else if (r2.getLinkId().contains("use")) {
//        hn.setUse(NameUse.valueOf(((Coding) a.getValue()).getCodeElement().primitiveValue().toUpperCase()));
//
//      }
//    }
//  }
//
//  // process address
//  public void setAddress(QuestionnaireResponseItemComponent r, Patient p) {
//    org.hl7.fhir.r4.model.Address address = new org.hl7.fhir.r4.model.Address();
//    p.addAddress(address);
//    for (QuestionnaireResponseItemComponent r2 : r.getItem()) {
//      // TODO - fix - it needs to be dynamic and generic
//      QuestionnaireResponseItemAnswerComponent a = r2.getAnswerFirstRep();
//      String v = a.getValue().primitiveValue();
//      if (r2.getLinkId().contains("text")) {
//        address.setText(v);
//      } else if (r2.getLinkId().contains("use")) {
//        address.setUse(AddressUse.valueOf(((Coding) a.getValue()).getCodeElement().primitiveValue().toUpperCase()));
//      }
//    }
//  }
//
//  // process identifier system (will need to be generic, not just for patient)
//  public void setIndetifier(QuestionnaireResponseItemComponent r, Patient p) {
//    Identifier id = new Identifier();
//    p.addIdentifier(id);
//    for (QuestionnaireResponseItemComponent r2 : r.getItem()) {
//      // TODO - fix - it needs to be dynamic and generic
//      QuestionnaireResponseItemAnswerComponent a = r2.getAnswerFirstRep();
//      String v = a.getValue().primitiveValue();
//      if (r2.getLinkId().contains("system")) {
//        id.setSystem(v);
//      } else if (r2.getLinkId().contains("value")) {
//        id.setValue(v);
//      }
//    }
//  }
//
//  // process system
//
//  public Resources processQR(QuestionnaireResponse qr, String provenanceUUID, String csvFileName) {
//    // TODO get QR from FHIR server...
//    // Questionnaire questionnaire = questionnaires.get(qr.getQuestionnaire());
//    //Questionnaire questionnaire = this.questionnaireLookup.findByUrl(qr.getQuestionnaire());
//    Resources resources = new Resources(provenanceUUID, csvFileName);
//
//    // process the QR
//    // TODO assuming that there are two levels of items; it is not guaranteed
//    // need to traverse the item -
//    for (QuestionnaireResponseItemComponent r : qr.getItem()) {
//      for (QuestionnaireResponseItemComponent r2 : r.getItem()) {
//        String definition = findDefinition(baseQuestionnaire, r2.getLinkId());
//        String[] definitionPath = definition.substring(definition.indexOf("#") + 1).split("\\.");
//        String[] restOfPath = Arrays.copyOfRange(definitionPath, 1, definitionPath.length);
//        Type value = getValue(r2);
//
//        if (definition.contains("Patient.name")) {
//          setHumanName(r2, resources.getPatient());
//          continue;
//        }
//        if (definition.contains("Patient.address")) {
//          setAddress(r2, resources.getPatient());
//          continue;
//        }
//        if (definition.contains("Patient.identifier")) {
//          setIndetifier(r2, resources.getPatient());
//          continue;
//        }
//        if (restOfPath.length == 1) {
//          if (restOfPath[0].equals("id")) {
//            // value = new IdType(value.castToString(value).asStringValue());
//          }
//        }
//
//        switch (definitionPath[0]) {
//        case "Patient":
//          drillDown(resources.getPatient(), restOfPath, value);
//          break;
//        case "Observation":
//          // TODO this linkid is finicky
//          drillDown(resources.getObservation(r.getLinkId()), restOfPath, value);
//          break;
//        case "Condition":
//          drillDown(resources.getCondition(), restOfPath, value);
//          break;
//        case "MedicationStatement":
//          drillDown(resources.getMedicationStatement(), restOfPath, value);
//          break;
//        case "Encounter":
//          drillDown(resources.getEncounter(), restOfPath, value);
//          break;
//        default:
//          throw new IllegalArgumentException("Not sure what the resource type is: " + definitionPath[0]);
//        }
//      }
//    }
//    return resources;
//  }
//

//
//  // TODO need to make it recursive...
//  private static String findDefinition(Questionnaire q, String linkId) {
//    for (QuestionnaireItemComponent r : q.getItem()) {
//      for (QuestionnaireItemComponent r2 : r.getItem()) {
//        if (r2.getLinkId().equals(linkId))
//          return r2.getDefinition();
//        for (QuestionnaireItemComponent r3 : r2.getItem()) {
//          if (r3.getLinkId().equals(linkId))
//            return r3.getDefinition();
//        }
//      }
//    }
//    throw new IllegalArgumentException("Cannot find the linkId");
//  }
//
//  // TODO - need to remove hard coded paths
//  private static void drillDown(Base b, String[] path, Type value) {
//    if (value == null) {
//      System.out.println("Cannot handle null value for " + b.toString() + " " + Arrays.asList(path));
//      return;
//    }
//
//    String p = null;
//
//    // TODO - needs to be generic
//    if (b instanceof Encounter && path[0].equals("location")) {
//      Encounter e = (Encounter) b;
//      e.addLocation();
//      Identifier identifier = new Identifier();
//      identifier.setSystem("MFL");
//      identifier.setValue(value.primitiveValue());
//      e.getLocation().get(0).getLocation().setIdentifier(identifier);
//      return;
//    }
//    if (b instanceof Observation && path[0].equals("performer")) {
//      Observation o = (Observation) b;
//      Identifier identifier = new Identifier();
//      identifier.setSystem("MFL");
//      identifier.setValue(value.primitiveValue());
//
//      o.addPerformer();
//      o.getPerformer().get(0).setIdentifier(identifier);
//      return;
//    }
//    if (b instanceof MedicationStatement && path[0].equals("effectivePeriod")) {
//      MedicationStatement ms = (MedicationStatement) b;
//      Period period = new Period();
//      period.setStartElement((DateTimeType) value);
//      ms.setEffective(period);
//      return;
//    }
//    if (b instanceof MedicationStatement && path[0].equals("medicationCodeableConcept")) {
//      MedicationStatement ms = (MedicationStatement) b;
//      ms.setMedication(new CodeableConcept().setCoding(Arrays.asList((Coding) value)));
//      return;
//    }
//
//    for (int i = 0; i < path.length; i++) {
//      Property prop = b.getNamedProperty(path[i]);
//
//      p = path[i];
//      if (p.equals("coding") && value instanceof Coding) {
//        if (b instanceof CodeableConcept)
//          ((CodeableConcept) b).setCoding(Arrays.asList((Coding) value));
//        break;
//      }
//      if (i == path.length - 1) {
//        // assign the value
//        if (p.equals("effectiveDateTime")) {
//          if (b instanceof Observation)
//            ((Observation) b).setEffective(value);
//          continue;
//        }
//        if (p.equals("valueQuantity")) {
//          ((Observation) b).setValue(new Quantity(value.castToInteger(value).getValue()));
//          continue;
//        }
//        if (p.equals("valueBoolean")) {
//          ((Observation) b).setValue(value);
//          continue;
//        }
//        if (value instanceof Coding)
//          b.setProperty(p, ((Coding) value).getCodeElement());
//        else {
//          if (p.equals("id"))
//            value = new IdType(value.castToString(value).asStringValue());
//
//          b.setProperty(p, value);
//        }
//      } else {
//        if (prop == null) {
//          if (b instanceof Encounter) {
//            Encounter e = (Encounter) b;
//            Location l = new Location();
//            Identifier identifier = new Identifier();
//            identifier.setValue(value.primitiveValue());
//            l.setIdentifier(Arrays.asList(identifier));
//            Reference r = new Reference(new Location());
//            e.addLocation(new EncounterLocationComponent(r));
//          }
//          b.addChild(p);
//        }
//        if (prop.getStructure() == null) {
//          prop.setStructure(new StructureDefinition());
//        }
//        b = prop.getStructure();
//      }
//    }
//  }

}
