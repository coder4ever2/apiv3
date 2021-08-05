package com.mimio.apiv3;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResembleRequest {

    public ResembleRequest(){}

    @JsonProperty("data")
    private Data data;

    @JsonProperty("phoneme_timestamps")
    private boolean phoneme_timestamps;
    private boolean raw;
    private String precision;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public boolean isPhoneme_timestamps() {
        return phoneme_timestamps;
    }

    public void setPhoneme_timestamps(boolean phoneme_timestamps) {
        this.phoneme_timestamps = phoneme_timestamps;
    }

    public boolean isRaw() {
        return raw;
    }

    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    public String getPrecision() {
        return precision;
    }

    public void setPrecision(String precision) {
        this.precision = precision;
    }

    public String getOutput_format() {
        return output_format;
    }

    public void setOutput_format(String output_format) {
        this.output_format = output_format;
    }

    @JsonProperty("output_format")
    private String output_format;
    /*
     "phoneme_timestamps": true,
             "raw": false,
             "precision": "PCM_16",
             "output_format": "mp3"

     "data": {
        "body": "Hi, how are you doing today?",
                "voice": "584fae8e",
                "title": "hello"
    },
    */


    public static class Data {
        public Data(){}
        private String body;
        private String voice;
        private String title;

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getVoice() {
            return voice;
        }

        public void setVoice(String voice) {
            this.voice = voice;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
