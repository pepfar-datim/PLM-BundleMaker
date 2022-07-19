package org.datim.plm.bundleMaker;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;

@RestController
@EnableAutoConfiguration
public class BundleMakerRestController {

    @Value("${fhirserverpath:NULL}")
    private String fhirserverpath;

    @RequestMapping(value = "/Questionnaire/$extract", method = RequestMethod.POST, consumes = {"application/json","application/fhir+json", "application/fhir+xml", "application/xml"})
    @ResponseBody
    String _extract(@RequestBody String requestBody,
                    @RequestHeader(value = "content-type", required = true) String contentType,
                    @RequestHeader(value = "accept", required = false) String acceptHeader) throws IOException {

        return BundleMaker.extractBundle(requestBody, null, fhirserverpath, contentType, acceptHeader);
    }

    @RequestMapping(value = "/Questionnaire/{id}/$extract", method = RequestMethod.POST, consumes = {"application/json","application/fhir+json", "application/fhir+xml", "application/xml"})
    @ResponseBody
    String _extract(@RequestBody String requestBody, @PathVariable String id,
                    @RequestHeader(value = "content-type", required = true) String contentType,
                    @RequestHeader(value = "accept", required = false) String acceptHeader) throws IOException {
        return BundleMaker.extractBundle(requestBody, id, fhirserverpath, contentType, acceptHeader);
    }

    @RequestMapping(value = "/extract", method = RequestMethod.POST, consumes = {"application/json","application/fhir+json", "application/fhir+xml", "application/xml"})
    @ResponseBody
    String extract(@RequestBody String requestBody,
                   @RequestHeader(value = "content-type", required = true) String contentType,
                   @RequestHeader(value = "accept", required = false) String acceptHeader) throws IOException {
        return BundleMaker.extractBundle(requestBody, null, fhirserverpath, contentType, acceptHeader);
    }

    public static void main(String[] args) {
        SpringApplication.run(BundleMakerRestController.class, args);
    }

}