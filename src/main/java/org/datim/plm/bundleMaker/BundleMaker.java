package org.datim.plm.bundleMaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class BundleMaker {

  QuestionnaireResponseParser qrParser;
  IParser parser;
  ResultBundle resultBundle;

  public static String extractBundle(String requestBody, String id, String fhirserverpath) throws IOException {
    FhirContext ctx = FhirContext.forR4();
    IParser parser = ctx.newJsonParser();
    //BundleMaker Validations
    BundleMakerValidator bundleMakerValidator = new BundleMakerValidator();
    bundleMakerValidator.jsonObjectValidator(requestBody);
    bundleMakerValidator.isFHIRServerValid(ctx,fhirserverpath);

    QuestionnaireLookup questionnaireLookup = new QuestionnaireLookup(ctx, fhirserverpath);


    ArrayList<QuestionnaireResponse> questionnaireResponses = QuestionnaireResponseParser
        .parseQuestionnaireResponses(parser, requestBody);

    //Validate QuestionnaireResponses -Has a linkId
    bundleMakerValidator.isQuestionnaireResponsesValid(questionnaireResponses);
    // TODO if zero length
    // TODO if invalid type
    if(questionnaireResponses.size()==0){
      throw new  HttpResponseException(HttpStatus.SC_NOT_FOUND, "QuestionResponse Bundle is empty");
    }

    // Check questionnaire reference & check that it is extractable
    Questionnaire baseQuestionnaire = null;
    if (id != null) {
      baseQuestionnaire = questionnaireLookup.findById(id);
      String expectedUrl = baseQuestionnaire.getUrl();
      // check that questionnaire with provided id has the same url as the
      // referenced one
      for (QuestionnaireResponse qr : questionnaireResponses) {
        if (!qr.getQuestionnaire().equals(expectedUrl)) {
          // TODO raise an error, referenced questionnaire is unexpected
          throw new IllegalArgumentException(
              "Questionnaire identified in the url "+expectedUrl+" does not match questionnaire referenced within the questionnaire response bundle "+qr.getQuestionnaire());
        }
      }
    } else {
      // extract questionnaire reference from the QR, if not found respond with
      // an error
      String firstQuestionnaireUrl = null;
      for (QuestionnaireResponse qr : questionnaireResponses) {
        if (firstQuestionnaireUrl == null) {
          firstQuestionnaireUrl = qr.getQuestionnaire();
          // check that it is valid - will throw an exception if invalid
          baseQuestionnaire = questionnaireLookup.findByUrl(firstQuestionnaireUrl);
          continue;
        }
        if (!firstQuestionnaireUrl.equals(qr.getQuestionnaire())) {
          throw new IllegalArgumentException(
              "Same questionnaire reference "+firstQuestionnaireUrl+" is expected in all questionnaire response resources within the bundle. Reference: "+qr.getQuestionnaire());
        }

      }
    }

    // TODO - expecting used questionnaire to be extractable - check that using
    // extension
    // http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-extract

    // TODO - for now not supporting item context:
    // http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext

    // TODO - currently only supporting creation of the context resources. Not
    // supporting pre-population and updating

    // TODO - currently not supporting queries/expressions

    //Validate Questionnaire - Has LinkId & Definition
    bundleMakerValidator.isQuestionnaireValid(baseQuestionnaire);

    QuestionnaireResponseParser questionnaireResponseParser = new QuestionnaireResponseParser(baseQuestionnaire);

    Bundle bundle = new Bundle();

    bundle.setType(BundleType.TRANSACTION);
    UUID uuid = UUID.randomUUID();
    bundle.setId(uuid.toString());
    bundle.setTimestamp(new java.util.Date());

    ArrayList<Resource> resources = new ArrayList<Resource>();
    for (QuestionnaireResponse qr : questionnaireResponses) {
      resources.addAll(questionnaireResponseParser.extractFrom(qr));
    }
    resources.forEach(r -> {
      if (r != null) {
        BundleEntryComponent bec = new BundleEntryComponent().setResource(r);
        bundle.addEntry(bec);

        BundleEntryRequestComponent berc = new BundleEntryRequestComponent();
        bec.setRequest(berc);
        berc.setMethod(HTTPVerb.PUT);
        berc.setUrl(r.getResourceType().toString() + "/" + r.getId());
      }
    });
    return parser.encodeResourceToString(bundle);
  }



//  public BundleMaker(IParser parser, String fhirServerPath) throws FileNotFoundException {
//    this.parser = parser;
//    // qrParser = new QuestionnaireResponseParser(parser, fhirServerPath);
//  }

  // TODO change path to stream
//  public void generateBundle(String bundleProfileId, String requestBody) throws FileNotFoundException, IOException {
//    // questionnaires = loadQuestionnaires(parser, pathToQuestionnaires);
//
//    // TODO support xml...
//
//    // list through first level items of the questionnaire (resource type)
//    // and assign each sub item to the resource...
//    // cross resource items: e.g. patient to others, etc.
//
//    // use structure definition to get to the base model...
//    // TODO - fix this:
//    String provenanceUID = UUID.randomUUID().toString();
//    String pattern = "yyyyMMdd_HHmmss";
//    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
//    String csvFileName = "Auto_generated_" + simpleDateFormat.format(new Date()) + ".csv";
//
//    // TODO where is resulting bundle structure definition specified - not in
//    // questionnaire or response...
//    ResultBundle rb = new ResultBundle(bundleProfileId);
//
//    // java.io.File inputFile = new java.io.File(pathToQuestionnaireResponse);
//    // FileInputStream in = new FileInputStream(inputFile);
//
//    JsonNode n = new ObjectMapper().readTree(requestBody);
//
//    if (n.isArray()) {
//      System.out.println("Processing array of questionnaire responses");
//      for (JsonNode nestedStructure : n) {
//        QuestionnaireResponse qr = parser.parseResource(QuestionnaireResponse.class, nestedStructure.toString());
//        Resources r = qrParser.processQR(qr, provenanceUID, csvFileName);
//        r.addToBundle(rb.getBundle());
//      }
//    } else {
//      try {
//        Bundle b = parser.parseResource(Bundle.class, n.toString());
//        for (BundleEntryComponent bec : b.getEntry()) {
//          Resource entryResource = bec.getResource();
//          if (entryResource instanceof QuestionnaireResponse) {
//            Resources r = qrParser.processQR((QuestionnaireResponse) entryResource, provenanceUID, csvFileName);
//            r.addToBundle(rb.getBundle());
//          } else
//            System.out.printf("entryResource type is %s\n", entryResource.getClass().getName());
//          System.err
//              .println("Entry resource is not a QuestionnaireResponse " + entryResource.getResourceType().toString());
//        }
//      } catch (DataFormatException dfe) {
//        System.out.println("Unexpected exception when parsting as a bundle: " + dfe.getMessage()
//            + ". Going to try as an individual resource.");
//        dfe.printStackTrace();
//        try {
//
//          Resource entryResource = parser.parseResource(QuestionnaireResponse.class, n.toString());
//          if (entryResource instanceof QuestionnaireResponse) {
//            Resources r = qrParser.processQR((QuestionnaireResponse) entryResource, provenanceUID, csvFileName);
//            r.addToBundle(rb.getBundle());
//          } else
//            System.out.printf("entryResource type is %s\n", entryResource.getClass().getName());
//          System.err
//              .println("Entry resource is not a QuestionnaireResponse " + entryResource.getResourceType().toString());
//        } catch (DataFormatException dfe2) {
//          throw new RuntimeException("Resource type is invalid. Either QuestionnaireResponse or Bundle is expected.");
//        }
//      }
//    }
//    this.resultBundle = rb;
//  }

  // TODO need testing
//  public void getBundleAsWriter(Writer w) throws IOException {
//    parser.encodeResourceToWriter(this.resultBundle.getBundle(), w);
//  }
//
//  public String getBundleAsString() {
//    return parser.encodeResourceToString(this.resultBundle.getBundle());
//  }

}
