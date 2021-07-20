package org.datim.plm.bundleMaker;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;

public class QuestionnaireLookup {

  private List<Questionnaire> questionnaires;
  private String server;

  public QuestionnaireLookup(FhirContext ctx, String serverBase) {
    this.server = serverBase;
    IGenericClient client = ctx.newRestfulGenericClient(serverBase);

    // We'll populate this list
    List<IBaseResource> qs = new ArrayList<>();

    Bundle bundle = client.search().forResource(Questionnaire.class).returnBundle(Bundle.class).execute();
    qs.addAll(BundleUtil.toListOfResources(ctx, bundle));

    while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
      bundle = client.loadPage().next(bundle).execute();
      qs.addAll(BundleUtil.toListOfResources(ctx, bundle));
    }

    questionnaires = qs.stream().map(a -> {
      return (Questionnaire) a;
    }).collect(Collectors.toList());
  }

  /**
   * @return the questionnaires
   */
  public List<Questionnaire> getQuestionnaires() {
    return questionnaires;
  }

  public Questionnaire findById(String id) {
    for (Questionnaire q : questionnaires) {
      if (q.getIdElement().getIdPart().equals(id))
        return q;
    }
    throw new IllegalArgumentException("Questionnaire with ID [" + id + "] not found on server " + this.server);
  }

  public Questionnaire findByUrl(String url) {
    for (Questionnaire q : questionnaires) {
      if (q.getUrl().equals(url))
        return q;
    }
    throw new IllegalArgumentException("Questionnaire with url [" + url + "] not found on server " + this.server);
  }
}
