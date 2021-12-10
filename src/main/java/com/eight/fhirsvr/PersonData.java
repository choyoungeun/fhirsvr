package com.eight.fhirsvr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class PersonData {

    public static void getPersonDataAll(String value) {
        FhirContext ctx = FhirContext.forR4();
        String encoded = "";

        String url = "http://localhost:4080/QuestionnaireResponse?identifier=" + value;
        HashMap<String, String> header = new HashMap<>();
        header.put("fhirVersion", "4.0");
        header.put("Content-type", "application/fhir+json");
        System.out.println("url:"+url);
        String queryRes = createRest(url, HttpMethod.GET, header, null);

        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(queryRes);
            JSONArray entryList = (JSONArray) jsonObject.get("entry");
            JSONObject entry = (JSONObject) jsonParser.parse(entryList.get(0).toString());
            String resource = entry.get("resource").toString();

            IParser iparser = ctx.newJsonParser();
            QuestionnaireResponse qr = iparser.parseResource(QuestionnaireResponse.class, resource);
            printPersonQuestionnaireResponseData(qr);
            iparser.setPrettyPrint(true);
            encoded = iparser.encodeResourceToString(qr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //System.out.println("person data:\n" + encoded);
    }

    private static void printPersonQuestionnaireResponseData(QuestionnaireResponse qr) {
        String[][] text = new String[2][11];

        for (int i = 0; i < 11; i ++){
            text[0][i] = "Question: " + qr.getItem().get(i).getText();
            text[1][i] = "Answer: " + qr.getItem().get(i).getAnswerFirstRep().getValueBooleanType().getValue().toString();
            if (qr.getItem().get(i).getAnswerFirstRep().hasItem()){
                text[1][i] = text[1][i] + "(" + qr.getItem().get(i).getAnswerFirstRep().getItemFirstRep().getAnswerFirstRep().getValueIntegerType().getValue() + "초)";
            }
        }

        for (int i = 0; i < 11; i ++) {
            System.out.println(text[0][i] + "\n" + text[1][i]);
        }
    }

    public static void getPersonData(String value) {
        FhirContext ctx = FhirContext.forR4();
        List<Integer> data = new ArrayList<>();

        String url = "http://localhost:4080/QuestionnaireResponse?identifier=" + value;
        HashMap<String, String> header = new HashMap<>();
        header.put("fhirVersion", "4.0");
        header.put("Content-type", "application/fhir+json");
        String queryRes = createRest(url, HttpMethod.GET, header, null);

        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(queryRes);
            JSONArray entryList = (JSONArray) jsonObject.get("entry");
            JSONObject entry = (JSONObject) jsonParser.parse(entryList.get(0).toString());
            String resource = entry.get("resource").toString();

            IParser iparser = ctx.newJsonParser();
            QuestionnaireResponse qr = iparser.parseResource(QuestionnaireResponse.class, resource);
            //printPersonQuestionnaireResponseData(qr);
            data = getData(qr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println("person data:\n" + data);
    }

    private static List<Integer> getData(QuestionnaireResponse qr) {
        List<Integer> data = new ArrayList<>();
        // num: 1,2,3,4,6,9,10 >> index: 0,1,2,3,5,8,9
        int[] needIDX = {0, 1, 2, 3, 5, 8, 9};

        for (int i = 0; i < needIDX.length; i++) {
            if (qr.getItem().get(needIDX[i]).getAnswerFirstRep().getValueBooleanType().getValue() == Boolean.TRUE) {
                int t = qr.getItem().get(needIDX[i]).getAnswerFirstRep().getItemFirstRep().getAnswerFirstRep().getValueIntegerType().getValue();
                data.add(t);
            } else {
                data.add(0);
            }
        }

        return data;
    }

    private static String createRest(String url, HttpMethod method, HashMap<String, String> header, String body) {
        HttpHeaders headers = setHeaders(header);
        HttpEntity request = new HttpEntity(body, headers);

        try {
            return sendRest(url, method, request);
        } catch (HttpServerErrorException | HttpClientErrorException var7) {
            var7.printStackTrace();
            return null;
        }
    }

    private static HttpHeaders setHeaders(HashMap<String, String> header) {
        HttpHeaders headers = new HttpHeaders();
        Iterator var2 = header.keySet().iterator();

        while (var2.hasNext()) {
            String key = (String) var2.next();
            String value = (String) header.get(key);
            headers.add(key, value);
        }

        return headers;
    }

    private static String sendRest(String url, HttpMethod method, HttpEntity<String> request) {
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, method, request, String.class, new Object[0]);
        HttpStatus statusCode = responseEntity.getStatusCode();
        if (!getStatus(statusCode).equals("true")) {
            return null;
        } else {
            return null == responseEntity.getBody() ? "true" : (String) responseEntity.getBody();
        }
    }

    private static String getStatus(HttpStatus statusCode) {
        return !statusCode.equals(HttpStatus.OK) && !statusCode.equals(HttpStatus.CREATED) ? statusCode.toString() : "true";
    }
}
