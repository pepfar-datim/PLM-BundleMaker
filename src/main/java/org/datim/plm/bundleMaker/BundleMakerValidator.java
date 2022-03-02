package org.datim.plm.bundleMaker;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.validator.routines.UrlValidator;

import java.security.PublicKey;

public class BundleValidator {
    private String server;

    public BundleValidator(FhirContext context, String serverBase) {
        this.server = serverBase;
        IGenericClient client = context.newRestfulGenericClient(serverBase);
    }

    public boolean urlValidator(String serverUrl){
        UrlValidator fhirUrlValidator = new UrlValidator();
        return fhirUrlValidator.isValid(serverUrl);
    }

}
