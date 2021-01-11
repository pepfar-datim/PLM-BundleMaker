# PLM-BundleMaker

**Repo Owner:** Vlad Shioshvili [@vshioshvili](https://github.com/vshioshvili)

FHIR bundle maker component of the patient level monitoring (PLM)



# BundleMaker

## Overview

BundleMaker app is a middleware data transformation component, written in Java as a RESTful web application, using [Spring Boot](https://spring.io/projects/spring-boot) framework. It accepts and converts a single Fast Healthcare Interoperability Resources (FHIR) Bundle of QuestionnaireResponse (QR) resources or a single QR resource into a Bundle of resources as identified by [questionnaire-definitions](https://www.hl7.org/fhir/questionnaire-definitions.html#Questionnaire.item.definition) of [questionnaire](https://www.hl7.org/fhir/questionnaire.html) items in the supllied QuestionnaireResponse.


The BundleMaker relies on [HAPI FHIR](https://hapifhir.io/) library for both parsing and generating FHIR resources. 

## Constraints

- Only json content is consumed and genarated; xml support can be easily added, by implementing XML parser use;
- It is limited to STU4 of FHIR, and is not backwards compatible with STU3 and earlier.
- It is currently limited to a small set of base and clinical resource types:
  - [Patient](https://www.hl7.org/fhir/patient.html)
  - [Encounter](https://www.hl7.org/fhir/encounter.html)
  - [Condition](https://www.hl7.org/fhir/condition.html)
  - [Observation](https://www.hl7.org/fhir/observation.html)
  - [MedicationStatement](https://www.hl7.org/fhir/medicationStatement.html)
- Generated resources are not persisted, and are only returned as a response
- While it can process it, the app is not intended to be used with a mix of QuestionnaireResponses responding to different Questionnaires in one bundle.

## Use

### Development

Maven is used as the software project mananagement tool.

### Building

Use `mvn package` to build the executable jar file. Maven will build executable jar with dependencies and place them in the target directory.

`application.properties` file in the root of the project defines the port number to use for the web app. Default value is 9000

### Running

Current version of the BundleMaker is a standalone restulf web application that expects a single configuration at the runtime - location of the folder where questionnaires are located:

`java -jar bundleMaker-0.0.1.jar --questionnaire.path=PATH_TO_QUESTIONNAIRES` --server.port=9000


### API

Current version of the application exposes a general API endpoint `http://localhost:9000/extract` for accepting POST requests, and expects the body of the request to be a `application/json` type, containing a single QR or a bundle QRs. Generated bundle will be assigned `Unknown` as the profile URI, as there is currently no way to identify bundle profile from the questionnaire or the questionnaire response.

To address the above limitation, for the proof of concept, to address lack of resource bundle profile identifier in the questionnaire, specific endpoints are defined for TX_PVLS and FP_CONTR_NEW resource bundles, and assign profile URI to generated bundle.

- `/extract/TX_PVLS`: generates resource bundle for TX_PVLS indicator as defined in [TX_PVLS structure definition](https://github.com/pepfar-datim/PLM/blob/master/TX_PVLS%20FHIR%20Profile/TX_PVLS_Bundle.StructureDefinition.xml)
- `/extract/FP_CONTR_NE`

Response of the call contains the bundle of resources that matches the profile.

## TODO

- Proper error checking:
  - validate input QuestionnaireReponse to ensure that the content corresponds to the Questionnaire
- Support for XML formatted data
  - auto detect input based on the content type
  - 
- Add support for reading questionnaires from a FHIR server
- Support of multiple profiles in the same bundle
- Add support for additional resource types
