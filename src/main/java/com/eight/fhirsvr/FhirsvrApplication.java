package com.eight.fhirsvr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@SpringBootApplication
public class FhirsvrApplication {
	public static void main(String[] args){
		//SpringApplication.run(FhirsvrApplication.class, args);

		// read questionnaire adl data excel file >> validate >> POST
		//QuestionnaireResource.readQuestionnaireExcelFile();

		// read questionnaire response adl data excel file >> validate >> POST
		//QuestionnaireResponseResource.readQuestionnaireResponseExcelFile();

		// get QuestionnaireResponseResource answer(7 integer answer);
		//ADLData.readADLData();

		// read person questionnaire response (identifier=[variable] )
		PersonData.getPersonDataAll("16");

		//
		//QuestionnaireData.getQuestionnaireData();

	}
}
