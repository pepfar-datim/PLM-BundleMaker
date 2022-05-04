# PLM-BundleMaker

**Repo Owner:** Vlad Shioshvili [@vshioshvili](https://github.com/vshioshvili)

FHIR bundle maker component of the patient level monitoring (PLM)



# BundleMaker

## Overview

BundleMaker app is a middleware data transformation component, written in Java as a RESTful web application, using [Spring Boot](https://spring.io/projects/spring-boot) framework. It implements definition based extraction of resources from questionnaire responses into referenced FHIR resources. It accepts and converts a single Fast Healthcare Interoperability Resources (FHIR) Bundle of QuestionnaireResponse (QR) resources or a single QR resource into a transaction type bundle of resources as identified by [questionnaire-definitions](https://www.hl7.org/fhir/questionnaire-definitions.html#Questionnaire.item.definition) of [questionnaire](https://www.hl7.org/fhir/questionnaire.html) items in the supllied QuestionnaireResponse.

Currently, only creation of new resources is supported. Pre-population and updating using context resources is not supported.

The BundleMaker relies on [HAPI FHIR](https://hapifhir.io/) library for both parsing and generating FHIR resources. 

## Constraints

- Only json content is consumed and genarated; xml support can be easily added, by implementing XML parser use;
- It is limited to STU4 of FHIR, and is not backwards compatible with STU3 and earlier.
- While all FHIR resource types are selected, focus is only on a select demographic and clinical resources, and others have not been verified. Supported resource types are:
  - [Patient](https://www.hl7.org/fhir/patient.html)
  - [Encounter](https://www.hl7.org/fhir/encounter.html)
  - [Condition](https://www.hl7.org/fhir/condition.html)
  - [Observation](https://www.hl7.org/fhir/observation.html)
  - [MedicationStatement](https://www.hl7.org/fhir/medicationStatement.html)
- Generated resources are not persisted, and are only returned as a response


## Use

### Development

Maven is used as the software project mananagement tool.

### Building

Use `mvn clean package` to build the executable jar file. Maven will build executable jar with dependencies and place them in the target directory.

### Running
#### Using Jar
Current version of the BundleMaker is a standalone restful web application that expects a single configuration at the runtime - path to the FHIR server where it can locate Questionnaire resources to use during extraction. By default, application will run on port 8080. Alternative port can be specified using `server.port` option.

`java -jar bundleMaker-0.0.1.jar --fhirserverpath=PATH_TO_FHIR_SERVER` --server.port=9000

#### Using Docker Compose
Prerequisites are Docker and Docker Compose.
Build the bundleMaker docker image 

`./build-docker-image.sh `

Configure the environment variable *fhirserverpath* with the FHIR Server URL on `docker-compose.yml`. 

To start the bundleMaker application, `docker-compose up`

### API

Application implements QuestionnaireResponseExtract operation (http://hl7.org/fhir/uv/sdc/OperationDefinition/QuestionnaireResponse-extract
).

It supports POST requests to either 'type' or 'object' endpoints.

`http://localhost:9000/Questionnaire/$extract` or `http://localhost:9000/Questionnaire/{id}/$extract`

If using object level endpoint, qustionnaire ID as it appears in the referenced FHIR server should be used, and it will be verified against the submitted QuestionnaireResponse.

Currently only `application/json` type input is supported and is expected as the body of the request. Input can contain either a singular resource or a bundle of QuestionnaireResponse resources.

Output is a transaction type bundle containing all the generated resources.

## TODO

- Proper error checking:
  - validate input QuestionnaireReponse to ensure that the content corresponds to the Questionnaire
- Support for XML formatted data
  - auto detect input based on the content type

- add checking for extractable questionnaire being used: http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-extract
- add support for item context http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext
- add support for creating context resources, and support pre-populating and updating resources
- add support for queries and expressions
- harden error checking and handling

http://build.fhir.org/ig/HL7/sdc/extraction.html
