package com.eight.fhirsvr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Questionnaire;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class QuestionnaireResource {
    public static void readQuestionnaireExcelFile() {
        List<String> qList = new ArrayList<String>();
        int[] qListNum = {15, 22, 29, 34, 39, 44, 48, 52, 57}; // repetition: 39,57 > index: 4,8

        try {
            FileInputStream file = new FileInputStream("C:\\Users\\LG\\Desktop\\project\\FHIR\\adl_2.xlsx");
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheetAt(0);

            int i = 0, j = 0, k = 0;

            int rows = sheet.getPhysicalNumberOfRows();
            for (i = 0; i < rows; i++) {
                XSSFRow row = sheet.getRow(i);
                if (row != null) {
                    int cells = row.getPhysicalNumberOfCells();
                    for (j = 0; j <= cells; j++) {
                        XSSFCell cell = row.getCell(j);
                        String cellData = "";
                        if (cell == null) {
                            continue;
                        } else {
                            switch (cell.getCellType()) {
                                case FORMULA:
                                    cellData = cell.getCellFormula();
                                    break;
                                case NUMERIC:
                                    cellData = cell.getNumericCellValue() + "";
                                    break;
                                case STRING:
                                    cellData = cell.getStringCellValue() + "";
                                    break;
                                case BLANK:
                                    cellData = cell.getBooleanCellValue() + "";
                                    break;
                                case ERROR:
                                    cellData = cell.getErrorCellValue() + "";
                                    break;
                            }
                        }
                        // get adl data
                        if (i > 2 && j == 2 && cellData != "false") {
                            if (k < qListNum.length && qListNum[k] == i) {
                                qList.add(cellData);
                                if (k == 4 || k == 8) {
                                    qList.add(cellData);
                                }
                                k++;
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        createQuestionnaireResource(qList);
    }

    public static void createQuestionnaireResource(List<String> list) {
        FhirContext ctx = FhirContext.forR4();
        Questionnaire q = new Questionnaire();
        List<Questionnaire.QuestionnaireItemComponent> qItems = new ArrayList<>();

        int[] itemINitemIndex = {1, 4, 5, 6, 7, 9, 10};

        q.setUrl("https://fhir.simplifier.net/ADLQ/StructureDefinition/Questionnaire");
        String value = RandomStringUtils.randomNumeric(7);
        q.addIdentifier().setValue(value);
        q.setStatus(Enumerations.PublicationStatus.DRAFT);

        // questionnaire item
        for (String str : list) {
            Questionnaire.QuestionnaireItemComponent item = new Questionnaire.QuestionnaireItemComponent();
            String uuid = UUID.randomUUID().toString();
            item.setLinkId(uuid);
            item.setText(str);
            item.setType(Questionnaire.QuestionnaireItemType.BOOLEAN);
            qItems.add(item);
        }
        q.setItem(qItems);
        // questionnaire item.item
        for (int i = 0; i < itemINitemIndex.length; i++) {
            Questionnaire.QuestionnaireItemComponent item = new Questionnaire.QuestionnaireItemComponent();
            String uuid = UUID.randomUUID().toString();
            item.setLinkId(uuid);
            item.setText(list.get(itemINitemIndex[i]));
            item.setType(Questionnaire.QuestionnaireItemType.INTEGER);
            qItems.get(itemINitemIndex[i]).addItem(item);
        }

        IParser jsonParser = ctx.newJsonParser();
        jsonParser.setPrettyPrint(true);
        String encoded = jsonParser.encodeResourceToString(q);

        //System.out.println("encoded:\n" + encoded);
        validateQuestionnaireResource(encoded);
    }

    public static void validateQuestionnaireResource(String encoded) {
        String profile = "https://fhir.simplifier.net/ADLQ/StructureDefinition/Questionnaire";
        String url = String.format("http://localhost:4080/$validate?profile=%s", profile);
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        String res = createRest(url, HttpMethod.POST, header, encoded);
        //System.out.println("res:\n"+res);
        postQuestionnaireResource(res, encoded);
    }

    private static void postQuestionnaireResource(String res, String encoded) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse(res);
            JSONArray issueList = (JSONArray) jsonObject.get("issue");
            JSONObject issue = (JSONObject) parser.parse(issueList.get(0).toString());
            String severity = issue.get("severity").toString();
            if (severity.equalsIgnoreCase("information")) {
                String url = "http://localhost:4080/Questionnaire";
                HashMap<String, String> header = new HashMap<>();
                header.put("fhirVersion", "4.0");
                header.put("Content-type", "application/fhir+json");
                String questionnaireResource = createRest(url, HttpMethod.POST, header, encoded);
                //System.out.println("post:\n" + questionnaireResource);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
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

