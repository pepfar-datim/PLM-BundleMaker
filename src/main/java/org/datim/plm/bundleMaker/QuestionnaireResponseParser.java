package org.datim.plm.bundleMaker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Type;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;

public class QuestionnaireResponseParser {

  private static HashMap<String, Questionnaire> questionnaires = null;

  private static HashMap<String, Questionnaire> loadQuestionnaires(IParser parser, String folder) throws FileNotFoundException{
    HashMap<String, Questionnaire> questionnaires = new HashMap<String, Questionnaire>();
    for(File f : new java.io.File(folder).listFiles()){
      try{
        Questionnaire q = parser.parseResource(Questionnaire.class, new FileReader(f));
        questionnaires.put(q.getUrl(), q);
      } catch(DataFormatException dfe){
        System.err.printf("%s cannot be parsed. Exception is: %s\n", f.getAbsolutePath(), dfe.getMessage());
      }
    }
    return questionnaires;
  }
  
  public QuestionnaireResponseParser(IParser parser, String folder) throws FileNotFoundException {
    questionnaires = loadQuestionnaires(parser, folder);
  }
  
  public Resources processQR(QuestionnaireResponse qr){
    // TODO get QR from FHIR server...
    Questionnaire questionnaire = questionnaires.get(qr.getQuestionnaire());
    Resources resources = new Resources();

    // process the QR
    // TODO assuming that there are two levels of items; it is not guaranteed
    for(QuestionnaireResponseItemComponent r : qr.getItem()){
      for(QuestionnaireResponseItemComponent r2 : r.getItem()){
        String definition = findDefinition(questionnaire, r2.getLinkId());
        String[] definitionPath = definition.substring(definition.indexOf("#")+1).split("\\.");
        String[] restOfPath = Arrays.copyOfRange(definitionPath, 1, definitionPath.length);

        Type value = getValue(r2);

        if(restOfPath.length == 1){
          if(restOfPath[0].equals("id")){
//            value = new IdType(value.castToString(value).asStringValue());
          }
        }

        switch(definitionPath[0]){
        case "Patient":
          drillDown(resources.getPatient(), restOfPath, value);
          break;
        case "Observation": 
          drillDown(resources.getObservation(), restOfPath, value);
          break;
        case "Condition": 
          drillDown(resources.getCondition(), restOfPath, value);
          break;
        case "MedicationStatement": 
          drillDown(resources.getMedicationStatement(), restOfPath, value);
          break;
        case "Encounter": 
          drillDown(resources.getEncounter(), restOfPath, value);
          break;
        default:
          throw new IllegalArgumentException("Not sure what the resource type is: " + definitionPath[0]);
        }
      }
    }
    return resources;
  }

  private static Type getValue(QuestionnaireResponseItemComponent r2){
    QuestionnaireResponseItemAnswerComponent a = r2.getAnswerFirstRep();
    if(a.getValue() == null)
      return null;

//    return a.getValue();
    if(a.getValue() instanceof Coding){
      // deal with coding type
      Coding codingTypeValue = (Coding)a.getValue();
      return codingTypeValue;
    } else if(a.getValue() instanceof DateTimeType){
      return (DateTimeType)a.getValue();
    } else if(a.getValue() instanceof DateType){
      return  (DateType)a.getValue();
    } else if(a.getValue() instanceof IntegerType){
      return (IntegerType)a.getValue();
    } else if(a.getValue() instanceof StringType){
      return (StringType)a.getValue();
    } else {
      throw new IllegalArgumentException("Not sure how to handle: " + a.getValue().getClass());
    }
  }
  
  private static String findDefinition(Questionnaire q, String linkId){
    for(QuestionnaireItemComponent r : q.getItem()){
      for(QuestionnaireItemComponent r2 : r.getItem()){
        if(r2.getLinkId().equals(linkId))
          return r2.getDefinition();
      }
    }
    throw new IllegalArgumentException("Cannot find the linkId");
  }
  
  private static void drillDown(Base b, String[] path, Type value){
    if(value == null){
      System.out.println("Cannot handle null value for " + b.toString() + " " + Arrays.asList(path));
      return;
    }
      
    String p = null;

    if(b instanceof Encounter && path[0].equals("location")){
      Encounter e = (Encounter)b;
      e.addLocation();
      Identifier identifier = new Identifier();
      identifier.setSystem("MFL");
      identifier.setValue(value.primitiveValue());
      e.getLocation().get(0).getLocation().setIdentifier(identifier);
      return;
    }
    if(b instanceof Observation && path[0].equals("performer")){
      Observation o = (Observation)b;
      Identifier identifier = new Identifier();
      identifier.setSystem("MFL");
      identifier.setValue(value.primitiveValue());

      o.addPerformer();
      o.getPerformer().get(0).setIdentifier(identifier);
      return;
    }
    if(b instanceof MedicationStatement && path[0].equals("effectivePeriod")){
      MedicationStatement ms = (MedicationStatement)b;
      Period period = new Period();
      period.setStartElement((DateTimeType)value);
      ms.setEffective(period);
      return;
    }
    if(b instanceof MedicationStatement && path[0].equals("medicationCodeableConcept")){
      MedicationStatement ms = (MedicationStatement)b;
      ms.setMedication(new CodeableConcept().setCoding(Arrays.asList((Coding)value)));
      return;
    }

    for(int i = 0; i < path.length; i++){
      Property prop = b.getNamedProperty(path[i]);

      p = path[i];
      if(p.equals("coding") && value instanceof Coding){
        if(b instanceof CodeableConcept)
          ((CodeableConcept)b).setCoding(Arrays.asList((Coding)value));
        break;
      }
      if(i == path.length-1){
        // assign the value
        if(p.equals("effectiveDateTime")){
          if(b instanceof Observation)
            ((Observation)b).setEffective(value);
          continue;
        }
        if(p.equals("valueQuantity")){
          ((Observation)b).setValue(new Quantity(value.castToInteger(value).getValue()));
          continue;
        }
        if(value instanceof Coding)
          b.setProperty(p, ((Coding)value).getCodeElement());
        else {
          if(p.equals("id"))
            value = new IdType(value.castToString(value).asStringValue());
          b.setProperty(p, value);
        }
      } else {
        if(prop == null){
          if(b instanceof Encounter){
            Encounter e = (Encounter)b;
            Location l = new Location();
            Identifier identifier = new Identifier();
            identifier.setValue(value.primitiveValue());
            l.setIdentifier(Arrays.asList(identifier));
            Reference r = new Reference(new Location());
            e.addLocation(new EncounterLocationComponent(r));
          }
          b.addChild(p);
        }
        if(prop.getStructure()==null){
          prop.setStructure(new StructureDefinition());
        }
        b = prop.getStructure();
      }
    }
  }

}
