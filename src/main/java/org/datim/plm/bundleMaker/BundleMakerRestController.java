package org.datim.plm.bundleMaker;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@RestController
@EnableAutoConfiguration
public class BundleMakerRestController {

  @Value("${questionnaire.path}")
  private String questionnairePath;

  @RequestMapping(value="/extract", method=RequestMethod.POST)
  @ResponseBody
  String extract(@RequestBody String requestBody) throws IOException {
    String sd = "Uknown";
    return extract(sd, requestBody);
  }

  @RequestMapping(value="/extract/TX_PVLS", method=RequestMethod.POST)
  @ResponseBody
  String extractTXPVLS(@RequestBody String requestBody) throws IOException {
    String sd = "http://datim.org/fhir/StructureDefinition/TX_PVLS_Bundle";
    return extract(sd, requestBody);
  }

  @RequestMapping(value="/extract/FP_CONTR_NEW", method=RequestMethod.POST)
  @ResponseBody
  String extractFPCONTR(@RequestBody String requestBody) throws IOException {
    String sd = "http://datim.org/fhir/StructureDefinition/FP_CONTR_NEW_Bundle";
    return extract(sd, requestBody);
  }

  private String extract(String strcutureDefinition, String requestBody) throws IOException {
    IParser parser = FhirContext.forR4().newJsonParser();

    BundleMaker bm = new BundleMaker(parser, questionnairePath);

    // TODO need dynamic option - where do you get it?
    String bundleProfileId = strcutureDefinition;
    bm.generateBundle(bundleProfileId, requestBody);

    String encoded = bm.getBundleAsString();
    return encoded;
  }

  public static void main(String[] args) {
    SpringApplication.run(BundleMakerRestController.class, args);
  }

}