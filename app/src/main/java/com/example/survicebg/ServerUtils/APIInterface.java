package com.example.survicebg.ServerUtils;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

public interface APIInterface {
    @Headers("content-type: application/json")
    @GET("{id}/?__a=1")
    Call<ResponseBody> getData(@Path("id") String id);
}
