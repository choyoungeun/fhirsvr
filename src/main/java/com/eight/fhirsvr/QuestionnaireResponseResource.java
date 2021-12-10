package com.eight.fhirsvr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hl7.fhir.r4.model.*;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class QuestionnaireResponseResource {
    public static void readQuestionnaireResponseExcelFile() {
        System.out.println("step 1: read");
        List<List<String>> qrList = new ArrayList<>();
        List<String> identifierValue = new ArrayList<>();
        int itemTextN = 0;

        // 여기서는 adl1파일의 데이터 정보만 읽어온다
        try {
            FileInputStream file = new FileInputStream("C:\\Users\\LG\\Desktop\\project\\FHIR\\adl_1.xlsx");
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheetAt(0);

            int i = 0, j = 0;

            int rows = sheet.getPhysicalNumberOfRows();
            for (i = 0; i < rows; i++) {
                XSSFRow row = sheet.getRow(i);
                if (row != null) {
                    int cells = row.getPhysicalNumberOfCells();
                    List<String> aRowList = new ArrayList<>();
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
                        if (i >= 3 && (j >= 4 && j <= 17)) {
                            if (cellData == "false") {
                                aRowList.add("-");
                            } else {
                                aRowList.add(cellData);
                            }
                        } else if (i >= 3 && j == 0) {
                            identifierValue.add(cellData);
                        } else if (i == 1 && (j >= 4 && j <= 17) && cellData != "false") {
                            itemTextN ++;
                        }
                    }
                    if (i >= 3) {
                        qrList.add(aRowList);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 읽어온 문자열 데이터를 정수 데이터로 변환
        List<List<Integer>> qrListINT = new ArrayList<>();
        qrListINT = changeListStringToINT(qrList);

        // linkId를 가져온다
        List<String> linkIdList = new ArrayList<>();
        linkIdList = getQuestionnaireResourceLinkId();

        // text 가져오기
        List<String> itemText = new ArrayList<>();
        itemText = getQuestionnaireResourceItemText();

        for (int i = 0; i < qrListINT.size(); i ++){
            createQuestionnaireResponseResource(identifierValue.get(i), linkIdList, itemText, itemTextN, qrListINT.get(i));
        }

    }

    private static List<String> getQuestionnaireResourceItemText() {
        FhirContext ctx = FhirContext.forR4();
        List<String> itemText = new ArrayList<>();

        String url = "http://localhost:4080/Questionnaire?_id=02e33f63-38f5-4f81-b087-b24eed2ad75e";
        HashMap<String, String> header = new HashMap<>();
        header.put("fhirVersion", "4.0");
        header.put("Content-type", "application/fhir+json");
        String queryRes = createRest(url, HttpMethod.GET, header, null);

        //System.out.println("get linkId resource:\n" + queryRes);

        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(queryRes);
            JSONArray entryList = (JSONArray) jsonObject.get("entry");
            JSONObject entry = (JSONObject) jsonParser.parse(entryList.get(0).toString());
            String resource = entry.get("resource").toString();

            //System.out.println("get Resource:\n" + resource);

            IParser iparser = ctx.newJsonParser();
            Questionnaire q = iparser.parseResource(Questionnaire.class, resource);
            for (Questionnaire.QuestionnaireItemComponent item : q.getItem()) {
                itemText.add(item.getText());
                if (item.hasItem()) {
                    itemText.add(item.getItemFirstRep().getText());
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return itemText;
    }

    private static List<String> getQuestionnaireResourceLinkId() {
        FhirContext ctx = FhirContext.forR4();
        List<String> linkIdList = new ArrayList<>();

        String url = "http://localhost:4080/Questionnaire?_id=02e33f63-38f5-4f81-b087-b24eed2ad75e";
        HashMap<String, String> header = new HashMap<>();
        header.put("fhirVersion", "4.0");
        header.put("Content-type", "application/fhir+json");
        String queryRes = createRest(url, HttpMethod.GET, header, null);

        //System.out.println("get linkId resource:\n" + queryRes);

        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(queryRes);
            JSONArray entryList = (JSONArray) jsonObject.get("entry");
            JSONObject entry = (JSONObject) jsonParser.parse(entryList.get(0).toString());
            String resource = entry.get("resource").toString();

            //System.out.println("get Resource:\n" + resource);

            IParser iparser = ctx.newJsonParser();
            Questionnaire q = iparser.parseResource(Questionnaire.class, resource);
            for (Questionnaire.QuestionnaireItemComponent item : q.getItem()) {
                linkIdList.add(item.getLinkId());
                if (item.hasItem()) {
                    linkIdList.add(item.getItemFirstRep().getLinkId());
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return linkIdList;
    }

    private static List<List<Integer>> changeListStringToINT(List<List<String>> listSTR) {
        List<List<Integer>> listINT = new ArrayList<>();

        for (int i = 0; i < listSTR.size(); i++) {
            List<String> aListSTR = listSTR.get(i);
            List<Integer> aListINT = new ArrayList<>();
            for (String str : aListSTR) {
                if (str.equals("S")) {
                    aListINT.add(1);
                } else if (str.equals("F") || str.equals("-")) {
                    aListINT.add(0);
                } else {
                    aListINT.add(changeTimeStringToINT(str));
                }
            }
            listINT.add(aListINT);
        }
        return listINT;
    }

    private static Integer changeTimeStringToINT(String str) {
        int iTime = 0;
        int sec = 0;
        for (int i = 0; i < str.length(); i++) {
            char a = str.charAt(i);
            if (a == ' ') {

            } else if (a == '.') {
                break;
            } else if (a == '분') {
                iTime = sec * 60;
                sec = 0;
            } else if (a == '초') {
                iTime = iTime + sec;
                sec = 0;
            } else {
                sec = a - '0' + sec * 10;
            }
        }
        iTime = iTime + sec;
        return iTime;
    }

    private static void createQuestionnaireResponseResource(String value, List<String> linkIdList, List<String> itemText, int size, List<Integer> answerList) {
        FhirContext ctx = FhirContext.forR4();
        QuestionnaireResponse qr = new QuestionnaireResponse();
        List<QuestionnaireResponse.QuestionnaireResponseItemComponent> qrItems = new ArrayList<>();

        qr.setIdentifier(new Identifier().setValue(value.substring(0, value.length() - 2)));
        qr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);

        int[] uuidIndex = {5, 6, 14, 15, 7, 8, 16, 17, 0, 1, 2, 3, 4, 9, 10, 11, 12, 13};    //18
        int uuidIDX = 0;
        int[] itemAnswerIndex = {2, 2, 2, 2, 0, 1, 1, 0, 0, 1, 1, 1, 1, 0};  //14 - 3 = 11
        int answerIDX = 0;

        for (int i = 0; i < size; i ++){
            QuestionnaireResponse.QuestionnaireResponseItemComponent item = new QuestionnaireResponse.QuestionnaireResponseItemComponent();
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent itemAnswer = new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent();
            item.setLinkId(linkIdList.get(uuidIndex[uuidIDX]));
            item.setText(itemText.get(uuidIndex[uuidIDX]));
            if (answerList.get(answerIDX) == 0) {
                itemAnswer.setValue(new BooleanType(Boolean.FALSE));
            } else {
                itemAnswer.setValue(new BooleanType(Boolean.TRUE));
                if (itemAnswerIndex[answerIDX] == 2) {
                    itemAnswer.addItem(createItemInAnswerInItem(itemText.get(uuidIndex[uuidIDX + 1]), answerList.get(answerIDX), linkIdList.get(uuidIndex[uuidIDX + 1])));
                } else if (itemAnswerIndex[answerIDX] == 1) {
                    itemAnswer.addItem(createItemInAnswerInItem(itemText.get(uuidIndex[uuidIDX + 1]), answerList.get(answerIDX + 1), linkIdList.get(uuidIndex[uuidIDX + 1])));
                }
            }
            if (itemAnswerIndex[answerIDX] == 1 || itemAnswerIndex[answerIDX] == 2) {
                uuidIDX++;
            }
            if (itemAnswerIndex[answerIDX] == 1) {
                answerIDX++;
            }

            item.addAnswer(itemAnswer);
            answerIDX++;
            uuidIDX++;
            qrItems.add(item);
        }

        /*
        for (int i = 0; i < itemText.size(); i ++) {
            QuestionnaireResponse.QuestionnaireResponseItemComponent item = new QuestionnaireResponse.QuestionnaireResponseItemComponent();
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent itemAnswer = new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent();
            item.setLinkId(linkIdList.get(uuidIndex[uuidIDX]));
            item.setText(itemText.get(i));
            if (answerList.get(answerIDX) == 0) {
                itemAnswer.setValue(new BooleanType(Boolean.FALSE));
            } else {
                itemAnswer.setValue(new BooleanType(Boolean.TRUE));
                if (itemAnswerIndex[answerIDX] == 2) {
                    itemAnswer.addItem(createItemInAnswerInItem(itemText.get(i), answerList.get(answerIDX), linkIdList.get(uuidIndex[uuidIDX + 1])));
                } else if (itemAnswerIndex[answerIDX] == 1) {
                    itemAnswer.addItem(createItemInAnswerInItem(itemText.get(i), answerList.get(answerIDX + 1), linkIdList.get(uuidIndex[uuidIDX + 1])));
                }
            }
            if (itemAnswerIndex[answerIDX] == 1 || itemAnswerIndex[answerIDX] == 2) {
                uuidIDX++;
            }
            if (itemAnswerIndex[answerIDX] == 1) {
                answerIDX++;
            }

            item.addAnswer(itemAnswer);
            answerIDX++;
            uuidIDX++;
            qrItems.add(item);
        }
         */
        qr.setItem(qrItems);

        IParser jsonParser = ctx.newJsonParser();
        jsonParser.setPrettyPrint(true);
        String encoded = jsonParser.encodeResourceToString(qr);

        System.out.println("print encoded:\n" + encoded);

        validateQuestionnaireResponseResource(encoded);
    }

    private static void validateQuestionnaireResponseResource(String encoded) {
        String profile = "https://fhir.simplifier.net/ADLQR/StructureDefinition/QuestionnaireResponse";
        String url = String.format("http://localhost:4080/$validate?profile=%s", profile);
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        String res = createRest(url, HttpMethod.POST, header, encoded);
        postQuestionnaireResponseResource(res, encoded);
    }

    private static void postQuestionnaireResponseResource(String res, String encoded) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse(res);
            JSONArray issueList = (JSONArray) jsonObject.get("issue");
            JSONObject issue = (JSONObject) parser.parse(issueList.get(0).toString());
            String severity = issue.get("severity").toString();
            if (severity.equalsIgnoreCase("information")) {
                String url = "http://localhost:4080/QuestionnaireResponse";
                HashMap<String, String> header = new HashMap<>();
                header.put("fhirVersion", "4.0");
                header.put("Content-type", "application/fhir+json");
                String questionnaireResponseResource = createRest(url, HttpMethod.POST, header, encoded);
                //System.out.println(questionnaireResponseResource);
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

    private static QuestionnaireResponse.QuestionnaireResponseItemComponent createItemInAnswerInItem(String s, Integer integer, String linkId) {
        QuestionnaireResponse.QuestionnaireResponseItemComponent item = new QuestionnaireResponse.QuestionnaireResponseItemComponent();
        item.setLinkId(linkId);
        item.setText(s);
        item.addAnswer().setValue(new IntegerType(integer));
        return item;
    }
}
