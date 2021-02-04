package com.example.survicebg.ServerUtils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FetchData {
    private FetchData(){}
    private static FetchData fetchDataObj;
    private static Call call;
    private static OkHttpClient okHttpClient = new OkHttpClient();
    public static FetchData getInstance(){
        if (fetchDataObj==null){
            fetchDataObj = new FetchData();
        }
        return fetchDataObj;
    }
    public void fetchJson(Context context, String URL,ResponseListener responseListener){
        final Request request = new Request.Builder()
                .url(URL)
                .method("GET", null)
                .build();
        call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                try {
                    responseListener.responseObject("error","");
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String imageUrl, videoUrl;
                try{
                    final JSONObject jsonObject = new JSONObject(response.body().string()).getJSONObject("graphql").getJSONObject("shortcode_media");
                    String type = jsonObject.getString("__typename");
                    if(type.equals("GraphImage")){
                        imageUrl = jsonObject.getString("display_url");
                        responseListener.responseObject(imageUrl,"GraphImage");
//                        downloadImage(imageUrl);
                    }else if (type.equals("GraphVideo")){
                        videoUrl = jsonObject.getString("video_url");
                        responseListener.responseObject(videoUrl,"GraphVideo");
//                        downloadVideo(videoUrl);
                    }else if (type.equals("GraphSidecar")){
                        imageUrl = jsonObject.getString("display_url");
                        responseListener.responseObject(imageUrl,"GraphImage");
                    }

                }catch (Exception e){
                    try {
                        responseListener.responseObject("error","");
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }
                    e.printStackTrace();
                }
            }
        });


    }
}
