package org.datim.plm.bundleMaker;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.validator.routines.UrlValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;

import java.util.ArrayList;
import java.util.List;

public class BundleMakerValidator {

    public BundleMakerValidator() {
    }
    //Validate the URL Provided
    public void urlValidator(String serverUrl){
        UrlValidator fhirUrlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
        if(!fhirUrlValidator.isValid(serverUrl)){
            throw new IllegalArgumentException("Invalid URL provided:"+serverUrl);
        }
    }

    // Validate if the data is a JSON object.
    public void jsonObjectValidator(String jsonData){
        try{
            JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
        }
        catch(JsonSyntaxException jsonSyntaxException){
            throw new IllegalArgumentException("Not a valid JSON string provided " + jsonSyntaxException.getMessage());
        }
    }
    //Validate if the server URL provided is a FHIR server
    public void isFHIRServerValid(FhirContext ctx, String serverUrl) {
        urlValidator(serverUrl);
        try {
            IGenericClient client = ctx.newRestfulGenericClient(serverUrl);
            List<IBaseResource> qs = new ArrayList<>();
            Bundle bundle = client.search().forResource(Questionnaire.class).returnBundle(Bundle.class).execute();
            qs.addAll(BundleUtil.toListOfResources(ctx, bundle));
        }
        catch (Exception exception){
            throw new IllegalArgumentException("Not a valid FHIR Server provided:" + exception.getMessage());
        }

    }

}
