package org.datim.plm.bundleMaker;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
public class BundleMakerRestController {

    @Value("${fhirserverpath:NULL}")
    private String fhirserverpath;

    @RequestMapping(value = "/Questionnaire/$extract", method = RequestMethod.POST, consumes = {"application/json"})
    @ResponseBody
    String _extract(@RequestBody String requestBody) throws IOException {
        return BundleMaker.extractBundle(requestBody, null, fhirserverpath);
    }

    @RequestMapping(value = "/Questionnaire/{id}/$extract", method = RequestMethod.POST, consumes = {"application/json"})
    @ResponseBody
    String _extract(@RequestBody String requestBody, @PathVariable String id) throws IOException {
        return BundleMaker.extractBundle(requestBody, id, fhirserverpath);
    }

    @RequestMapping(value = "/extract", method = RequestMethod.POST, consumes = {"application/json"})
    @ResponseBody
    String extract(@RequestBody String requestBody) throws IOException {
        return BundleMaker.extractBundle(requestBody, null, fhirserverpath);
    }

    public static void main(String[] args) {
        SpringApplication.run(BundleMakerRestController.class, args);
    }

}