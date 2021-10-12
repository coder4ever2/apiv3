package com.mimio.apiv3;

import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.gax.rpc.ApiException;
import com.google.api.services.dialogflow.v2.model.*;
import com.google.cloud.dialogflow.v2.*;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.*;

@CrossOrigin
@RestController
public class MainController {
    public static final Random RANDOM = new Random();
    public static final String BirthdayIntent = "user_ask_Bday";
    public static final String MOVIE_CUSTOM = "MOVIE - custom";
    private static String projectId = "mimio-es-wmtr";
    private static String languageCode = "en-US";
    private static String sessionId = "123456789";

    private static Logger logger = LoggerFactory.getLogger(MainController.class);


    @GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        logger.info("hello api");
        return String.format("Hello %s!", name);
    }

    @GetMapping("/test/audio")
    public String audio(@RequestParam(value = "text", defaultValue = "Hi, there , how are you doing? This is Mimio!") String name) throws URISyntaxException {
        return ResembleController.getAudioURL("Bye, see you later.");
    }

    @GetMapping("/input")
    public String v2(@RequestParam(value="input") String input) throws IOException {
        Map<String, QueryResult> stringQueryResultMap = DialogFlowController.detectIntentTexts(projectId, input, languageCode, sessionId);
        logger.info(String.valueOf(stringQueryResultMap));
        return stringQueryResultMap.toString();
    }

    @GetMapping("/movie")
    public Movie movie(@RequestParam(value="text") String text) throws IOException {
        RestTemplate restTemplate =  new RestTemplate();

        Movie movie = restTemplate.getForObject(
                "https://www.omdbapi.com/?apikey=bdfbd3aa&plot=short&t="+text, Movie.class);
        logger.info("Movie webhook called at " + new Date());
        logger.info(movie.toString());
        return movie;
    }
    @PostMapping(
            value = "/webhook",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, String> webhook(@RequestBody WebhookRequest request) throws InvalidProtocolBufferException {
        System.out.print(request.toString());
        RestTemplate restTemplate =  new RestTemplate();

        Movie movie = restTemplate.getForObject(
                "https://www.omdbapi.com/?apikey=bdfbd3aa&plot=short&t=3_idiots", Movie.class);
        logger.info("POST webhook called at " + new Date());
        logger.info(movie.toString());
        return Collections.singletonMap("key2", "value2");
    }
    @PostMapping(
            value = "/test",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> example() {
        return Collections.singletonMap("key", "value");

    }

    private final static JacksonFactory jacksonFactory = new JacksonFactory();

    @PostMapping(value = "/dialogflow-webhook", produces = {MediaType.APPLICATION_JSON_VALUE})
    public String dialogflowWebhook(@RequestBody String rawData) throws IOException {
        //Step 1. Parse the request
        GoogleCloudDialogflowV2WebhookRequest request = jacksonFactory
                .createJsonParser(rawData)
                .parse(GoogleCloudDialogflowV2WebhookRequest.class);

        logger.info("Received webhook request" + request.toPrettyString());

        //Step 2. Process the request
        //Step 3. Build the response message
        logger.info("=============Webhook BEGINS=======");
        GoogleCloudDialogflowV2IntentMessage msg = new GoogleCloudDialogflowV2IntentMessage();
        GoogleCloudDialogflowV2IntentMessageText text = new GoogleCloudDialogflowV2IntentMessageText();
        ArrayList<String> textList = new ArrayList<>();
        String responseText = "";
        if(MOVIE_CUSTOM.equals(request.getQueryResult().getIntent().getDisplayName())) {
            logger.info("=============MOVIE Webhook =======");
            responseText = getMovieString(request, responseText);
        }else if(BirthdayIntent.equals(request.getQueryResult().getIntent().getDisplayName())){
            logger.info("=============BIRTHDAY Webhook =======");
            responseText = getBirthdayString(request, responseText);
        }
        textList.add(responseText);
        text.setText(textList);
        msg.setText(text);
        List<GoogleCloudDialogflowV2IntentMessage> msgs = new ArrayList<GoogleCloudDialogflowV2IntentMessage>();
        msgs.add(msg);

        GoogleCloudDialogflowV2WebhookResponse response = new GoogleCloudDialogflowV2WebhookResponse();
        response.setFulfillmentMessages(msgs);
        logger.info("==========Webhook - ENDS==========");
        return getStringResponse(response);
    }

    private String getMovieString(GoogleCloudDialogflowV2WebhookRequest request, String responseText) {
        Object movieName = request.getQueryResult().getParameters().get("movieName");
        if(movieName !=null){
            RestTemplate restTemplate =  new RestTemplate();
            Movie movie = restTemplate.getForObject(
                    "https://www.omdbapi.com/?apikey=bdfbd3aa&plot=short&t="+movieName, Movie.class);
            if(movie!=null && movie.getPlot()!=null){
                responseText = responseText + "Seems like a good "
                        //+ movie.getTitle()
                        //+ ", I think it's an interesting "
                        + movie.getGenre()
                        + " movie. "
                        //+movie.getPlot()
                        //+ "I believe it also got a won interesting awards. "+ movie.getAwards()
                        //+ " Pretty cool, right?"
                        ;
            }
        }else {
            responseText = "I haven't heard of this movie. ";
        }
        try{
            responseText += String.valueOf(request.getQueryResult().getFulfillmentMessages().get(0).getText().getText().get(0));
            if(request.getQueryResult().getFulfillmentMessages().size()>1) {
                responseText += " ";
                responseText += String.valueOf(request.getQueryResult().getFulfillmentMessages().get(1).getText().getText().get(0));
            }
        }catch(Exception e){
            logger.error(e.getMessage());
        }

        return responseText;
    }
    private String getBirthdayString(GoogleCloudDialogflowV2WebhookRequest request, String responseText) {
        responseText="I had fun birthday celebration with family. Here are some pictures from my birthday celebration.";
        try{
            responseText = String.valueOf(request.getQueryResult().getFulfillmentMessages().get(0).getText().getText().get(0));
            if(request.getQueryResult().getFulfillmentMessages().size()>1) {
                responseText += " ";
                responseText += String.valueOf(request.getQueryResult().getFulfillmentMessages().get(1).getText().getText().get(0));
            }
        }catch(Exception e){
            logger.error(e.getMessage());
        }
        return responseText;
    }

    private String getStringResponse(Object response) throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonGenerator jsonGenerator = jacksonFactory.createJsonGenerator(stringWriter);
        jsonGenerator.enablePrettyPrint();
        jsonGenerator.serialize(response);
        jsonGenerator.flush();
        return stringWriter.toString();
    }

    @PostMapping(value = "/ask", produces = {MediaType.APPLICATION_JSON_VALUE})
    public AskResponse ask(@RequestBody String rawData) throws IOException {
        Map<String, String> map = jacksonFactory
                .createJsonParser(rawData)
                .parse(Map.class);
        String userQuery = map.get("text");
        AskResponse askResponse = new AskResponse();
        detectIntentTexts(userQuery, askResponse);
        return askResponse;
    }

    // DialogFlow API Detect Intent sample with text inputs.
    public static QueryResult detectIntentTexts(String text, AskResponse askResponse)
            throws IOException, ApiException {
        QueryResult queryResult;
        // Instantiates a client
        try (SessionsClient sessionsClient = SessionsClient.create()) {
            // Set the session name using the sessionId (UUID) and projectID (my-project-id)
            SessionName session = SessionName.of(projectId, sessionId);
            logger.info("Session Path: " + session.toString());


            // Set the text (hello) and language code (en-US) for the query
            TextInput.Builder textInput =
                    TextInput.newBuilder().setText(text).setLanguageCode(languageCode);

            // Build the query with the TextInput
            QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

            // Performs the detect intent request
            DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);

            // Display the query result
            queryResult = response.getQueryResult();


            askResponse.setResponse(queryResult.getFulfillmentText());
            try {
                String responseText = String.valueOf(queryResult.getFulfillmentMessages(0).getText().getText(0));
                if(queryResult.getFulfillmentMessagesCount()>1){
                    responseText = responseText + " "+ String.valueOf(queryResult.getFulfillmentMessages(1).getText().getText(0));
                }
                askResponse.setResponse(responseText);
                askResponse.setAdditionalContext(queryResult.getFulfillmentText());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            askResponse.setIntent(queryResult.getIntent().toString());
            try {
                askResponse.setAudio(ResembleController.getAudioURL(askResponse.getResponse()));
                logger.info("Set audio response");
            }catch(Exception e){
                logger.error("Resemble Error");
                logger.error(e.getMessage());
                logger.error(e.getCause().toString());
            }

            if(BirthdayIntent.equals(response.getQueryResult().getIntent().getDisplayName())) {
                askResponse.setAssets(new String[]{
                        "https://apiv3-m6yhyee6aa-wl.a.run.app/5.jpg"
                        ,"https://apiv3-m6yhyee6aa-wl.a.run.app/6.jpg",
                        "https://apiv3-m6yhyee6aa-wl.a.run.app/7.jpg"});
                askResponse.setAsset("https://apiv3-m6yhyee6aa-wl.a.run.app/5.jpg");
            }

            logger.info("====================");
            logger.info("Query Text: "+ queryResult.getQueryText());
            logger.info(
                    "Detected Intent: " + queryResult.getIntent().getDisplayName() +" "+ queryResult.getIntentDetectionConfidence());
            logger.info(
                    "Fulfillment Text: " +
                            (queryResult.getFulfillmentMessagesCount() > 0
                            ? queryResult.getFulfillmentMessages(0).getText()
                            : "Triggered Default Fallback Intent"));

        }
        return queryResult;
    }
}

