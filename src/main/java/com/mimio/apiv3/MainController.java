package com.mimio.apiv3;

import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.gax.rpc.ApiException;
import com.google.api.services.dialogflow.v2.model.*;
import com.google.cloud.dialogflow.v2.*;
import com.google.protobuf.InvalidProtocolBufferException;
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
    private static String projectId = "mimio-es-wmtr";
    private static String languageCode = "en-US";
    private static String sessionId = "123456789";

    @GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        return String.format("Hello %s!", name);
    }

    @GetMapping("/test/audio")
    public String audio(@RequestParam(value = "text", defaultValue = "Hi, there , how are you doing? This is Mimio!") String name) throws URISyntaxException {
        return ResembleController.getAudioURL("Bye, see you later.");
    }

    @GetMapping("/input")
    public String v2(@RequestParam(value="input") String input) throws IOException {
        Map<String, QueryResult> stringQueryResultMap = DialogFlowController.detectIntentTexts(projectId, input, languageCode, sessionId);
        System.out.println(stringQueryResultMap);
        return stringQueryResultMap.toString();
    }

    @GetMapping("/movie")
    public Movie movie(@RequestParam(value="text") String text) throws IOException {
        RestTemplate restTemplate =  new RestTemplate();

        Movie movie = restTemplate.getForObject(
                "https://www.omdbapi.com/?apikey=bdfbd3aa&plot=short&t="+text, Movie.class);
        System.out.println("Movie webhook called at " + new Date());
        System.out.println(movie.toString());
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
        System.out.println("POST webhook called at " + new Date());
        System.out.println(movie.toString());
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

        //Step 2. Process the request
        //Step 3. Build the response message
        System.out.println("=============Webhook BEGINS=======");
        GoogleCloudDialogflowV2IntentMessage msg = new GoogleCloudDialogflowV2IntentMessage();
        GoogleCloudDialogflowV2IntentMessageText text = new GoogleCloudDialogflowV2IntentMessageText();
        ArrayList<String> textList = new ArrayList<>();
        String responseText = "";
        if("MOVIE - custom".equals(request.getQueryResult().getIntent().getDisplayName())) {
            System.out.println("=============MOVIE Webhook =======");
            responseText = getMovieString(request, responseText);
        }else if(BirthdayIntent.equals(request.getQueryResult().getIntent().getDisplayName())){
            System.out.println("=============BIRTHDAY Webhook =======");
            responseText = getBirthdayString(request, responseText);
        }
        textList.add(responseText);
        text.setText(textList);
        msg.setText(text);
        List<GoogleCloudDialogflowV2IntentMessage> msgs = new ArrayList<GoogleCloudDialogflowV2IntentMessage>();
        msgs.add(msg);

        GoogleCloudDialogflowV2WebhookResponse response = new GoogleCloudDialogflowV2WebhookResponse();
        response.setFulfillmentMessages(msgs);
        System.out.println("==========Webhook - ENDS==========");
        return getStringResponse(response);
    }

    private String getMovieString(GoogleCloudDialogflowV2WebhookRequest request, String responseText) {
        Object movieName = request.getQueryResult().getParameters().get("movieName");
        if(movieName !=null){
            RestTemplate restTemplate =  new RestTemplate();
            Movie movie = restTemplate.getForObject(
                    "https://www.omdbapi.com/?apikey=bdfbd3aa&plot=short&t="+movieName, Movie.class);
            if(movie!=null && movie.getPlot()!=null){
                responseText = responseText + "I also like the movie "
                        + movie.getTitle()
                        + ", I think it's an interesting "
                        + movie.getGenre()
                        + " movie."
                        //+movie.getPlot()
                        //+ "I believe it also got a won interesting awards. "+ movie.getAwards()
                        //+ " Pretty cool, right?"
                        ;
            }
        }else {
            responseText = "I haven't heard of this movie.";
        }
        return responseText;
    }
    private String getBirthdayString(GoogleCloudDialogflowV2WebhookRequest request, String responseText) {
        responseText="I had fun birthday celebration with family. Here are some pictures from my birthday celebration.";
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
            System.out.println("Session Path: " + session.toString());


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
                System.err.print(e);
            }
            askResponse.setIntent(queryResult.getIntent().toString());
            try {
                askResponse.setAudio(ResembleController.getAudioURL(askResponse.getResponse()));
            }catch(URISyntaxException e){
                System.err.print(e);
            }

            if(BirthdayIntent.equals(response.getQueryResult().getIntent().getDisplayName())) {
                askResponse.setAsset("https://apiv3-m6yhyee6aa-wl.a.run.app/Photos/5.jpg");
            }

            System.out.println("====================");
            System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
            System.out.format(
                    "Detected Intent: %s (confidence: %f)\n",
                    queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
            System.out.format(
                    "Fulfillment Text: '%s'\n",
                    queryResult.getFulfillmentMessagesCount() > 0
                            ? queryResult.getFulfillmentMessages(0).getText()
                            : "Triggered Default Fallback Intent");

        }
        return queryResult;
    }
}

