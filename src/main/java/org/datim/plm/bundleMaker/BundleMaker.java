package org.datim.plm.bundleMaker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;

public class BundleMaker {

  QuestionnaireResponseParser qrParser;
  IParser parser;
  ResultBundle resultBundle;

  public BundleMaker(IParser parser, String pathToQuestionnaires) throws FileNotFoundException {
    this.parser = parser;
    qrParser = new QuestionnaireResponseParser(parser, pathToQuestionnaires);
  }

// TODO change path to stream
  public void generateBundle(String bundleProfileId, String requestBody) throws FileNotFoundException, IOException {
//  questionnaires = loadQuestionnaires(parser, pathToQuestionnaires);

  //TODO support xml...


  // list through first level items of the questionnaire (resource type)
  // and assign each sub item to the resource... 
  // cross resource items: e.g. patient to others, etc.

  // use structure definition to get to the base model... 

  // TODO where is resulting bundle structure definition specified - not in questionnaire or response...
  ResultBundle rb = new ResultBundle(bundleProfileId);

  
//  java.io.File inputFile = new java.io.File(pathToQuestionnaireResponse);
//  FileInputStream in = new FileInputStream(inputFile);

  JsonNode n = new ObjectMapper().readTree(requestBody);
  
  if(n.isArray()){
    System.out.println("Processing array of questionnaire responses");
    for(JsonNode nestedStructure : n){
      QuestionnaireResponse qr = parser.parseResource(QuestionnaireResponse.class, nestedStructure.toString());
      Resources r = qrParser.processQR(qr);
      r.addToBundle(rb.getBundle());
    }
  } else {
    try {
    Bundle b = parser.parseResource(Bundle.class, n.toString());
    for(BundleEntryComponent bec : b.getEntry()){
      Resource entryResource = bec.getResource(); 
      if(entryResource instanceof QuestionnaireResponse){
        Resources r = qrParser.processQR((QuestionnaireResponse)entryResource);
        r.addToBundle(rb.getBundle());
      }
      else
        System.out.printf("entryResource type is %s\n", entryResource.getClass().getName() );
        System.err.println("Entry resource is not a QuestionnaireResponse " + entryResource.
            getResourceType().
            toString());
    }
    } catch (DataFormatException dfe){
      try{
      Resource entryResource = parser.parseResource(QuestionnaireResponse.class, n.toString());
      if(entryResource instanceof QuestionnaireResponse){
        Resources r = qrParser.processQR((QuestionnaireResponse)entryResource);
        r.addToBundle(rb.getBundle());
      }
      else
        System.out.printf("entryResource type is %s\n", entryResource.getClass().getName() );
        System.err.println("Entry resource is not a QuestionnaireResponse " + entryResource.
            getResourceType().
            toString());
      } catch(DataFormatException dfe2){
        throw new RuntimeException("Resource type is invalid. Either QuestionnaireResponse or Bundle is expected.");
      }
    }
  }
    this.resultBundle = rb;
  }


  // TODO need testing
  public void getBundleAsWriter(Writer w) throws IOException{
    parser.encodeResourceToWriter(this.resultBundle.getBundle(), w);
  }

  public String getBundleAsString(){
    return parser.encodeResourceToString(this.resultBundle.getBundle());
  }

}
