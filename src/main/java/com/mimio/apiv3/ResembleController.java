package com.mimio.apiv3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

public class ResembleController {

    private static Logger logger = LoggerFactory.getLogger(ResembleController.class);


    public static String getAudioURL(String text) throws URISyntaxException {
        URI uri = new URI(
                "https://app.resemble.ai/api/v1/projects/63f19659/clips/sync");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("miv9rB7tEwE5qe4QZixjIAtt");

        ResembleRequest resembleRequest = new ResembleRequest();
        ResembleRequest.Data data = new ResembleRequest.Data();
        data.setBody(text);
        data.setTitle("mimio" + System.currentTimeMillis());
        data.setVoice("d8a68443");

        resembleRequest.setData(data);
        resembleRequest.setOutput_format("mp3");
        resembleRequest.setPhoneme_timestamps(false);
        resembleRequest.setPrecision("PCM_16");
        resembleRequest.setRaw(false);

        HttpEntity<ResembleRequest> httpEntity = new HttpEntity<>(resembleRequest, headers);

        RestTemplate restTemplate = new RestTemplate();
        String audioUrl = restTemplate.postForObject(uri, httpEntity, String.class);
        logger.info("Audio URL:" + audioUrl);

        return audioUrl;
    }
}
