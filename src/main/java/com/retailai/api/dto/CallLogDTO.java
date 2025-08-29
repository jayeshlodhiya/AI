package com.retailai.api.dto;

import java.util.List;

public class CallLogDTO {
    public String id;
    public String phone_number;
    public String call_cost;
    public Integer call_duration_in_sec;
    public Object call_sentiment; // or String if you know the type
    public String created_at;
    public String assistant_name;
    public String call_transcribe; // <-- strongly typed
    public String recording_url;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getCall_cost() {
        return call_cost;
    }

    public void setCall_cost(String call_cost) {
        this.call_cost = call_cost;
    }

    public Integer getCall_duration_in_sec() {
        return call_duration_in_sec;
    }

    public void setCall_duration_in_sec(Integer call_duration_in_sec) {
        this.call_duration_in_sec = call_duration_in_sec;
    }

    public Object getCall_sentiment() {
        return call_sentiment;
    }

    public void setCall_sentiment(Object call_sentiment) {
        this.call_sentiment = call_sentiment;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getAssistant_name() {
        return assistant_name;
    }

    public void setAssistant_name(String assistant_name) {
        this.assistant_name = assistant_name;
    }

    public String getCall_transcribe() {
        return call_transcribe;
    }

    public void setCall_transcribe(String call_transcribe) {
        this.call_transcribe = call_transcribe;
    }

    public String getRecording_url() {
        return recording_url;
    }

    public void setRecording_url(String recording_url) {
        this.recording_url = recording_url;
    }

// ... add other fields you return ...
}