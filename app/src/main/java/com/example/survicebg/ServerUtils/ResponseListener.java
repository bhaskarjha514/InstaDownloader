package com.example.survicebg.ServerUtils;

import org.json.JSONException;

public interface ResponseListener {
    public void responseObject(Object data, String type) throws JSONException;
}
