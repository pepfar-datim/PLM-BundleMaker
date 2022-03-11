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
import org.hl7.fhir.r4.model.QuestionnaireResponse;

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

    public void isQuestionnaireResponsesValid(ArrayList<QuestionnaireResponse> questionnaireResponses){

        for (QuestionnaireResponse qr : questionnaireResponses) {
            for(QuestionnaireResponse.QuestionnaireResponseItemComponent qrItemComponent: qr.getItem()){
                if(!qrItemComponent.hasLinkId()){
                    throw new IllegalArgumentException("Not a valid QuestionnaireResponse - Missing Some LinkId ");
                }
                checkQuestionnaireResponseItemHasLinkId(qrItemComponent);
            }
        }
    }

    private void checkQuestionnaireResponseItemHasLinkId(QuestionnaireResponse.QuestionnaireResponseItemComponent itemComponent) {

        if(itemComponent.hasItem()) {
            for(QuestionnaireResponse.QuestionnaireResponseItemComponent qRItemComponent : itemComponent.getItem()) {
                if(qRItemComponent.hasItem()){
                    if(!qRItemComponent.hasLinkId()){
                        throw new IllegalArgumentException("Not a valid QuestionnaireResponse - Missing Some LinkId ");
                    }
                    checkQuestionnaireResponseItemHasLinkId(qRItemComponent);
                }else {
                    if(!qRItemComponent.hasLinkId()){
                        throw new IllegalArgumentException("Not a valid QuestionnaireResponse - Missing Some LinkId");
                    }
                }
            }
        }
        else {
            if(!itemComponent.hasLinkId()){
                throw new IllegalArgumentException("Not a valid QuestionnaireResponse - Missing Some LinkId ");
            }
        }
    }

    public void isQuestionnaireValid(Questionnaire questionnaire) {
        if(questionnaire.hasItem()) {
            for(Questionnaire.QuestionnaireItemComponent questionnaireItemComponent : questionnaire.getItem()) {
                if(!questionnaireItemComponent.hasLinkId() || !questionnaireItemComponent.hasDefinition()){
                    throw new IllegalArgumentException("Not a valid Questionnaire - Missing Some LinkId and Definition");
                }
                checkQuestionnaireItemValidity(questionnaireItemComponent);
            }
        }
    }

    private void checkQuestionnaireItemValidity(Questionnaire.QuestionnaireItemComponent itemComponent) {
        if(itemComponent.hasItem()) {
            for(Questionnaire.QuestionnaireItemComponent questionnaireItemComponent : itemComponent.getItem()) {
                    if(questionnaireItemComponent.hasItem()){
                        if(!questionnaireItemComponent.hasLinkId() || !questionnaireItemComponent.hasDefinition()){
                            throw new IllegalArgumentException("Not a valid Questionnaire - Missing Some LinkId and Definition");
                        }
                        checkQuestionnaireItemValidity(questionnaireItemComponent);
                    }else {
                        if(!questionnaireItemComponent.hasLinkId() || !questionnaireItemComponent.hasDefinition()){
                            throw new IllegalArgumentException("Not a valid Questionnaire - Missing Some LinkId and Definition");
                        }
                    }
            }
        }else {
            if(!itemComponent.hasLinkId() || !itemComponent.hasDefinition()){
                throw new IllegalArgumentException("Not a valid Questionnaire - Missing Some LinkId and Definition");
            }
        }

    }

}
