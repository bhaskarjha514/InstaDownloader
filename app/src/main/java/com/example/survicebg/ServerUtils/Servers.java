package com.example.survicebg.ServerUtils;

import android.util.Log;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class Servers {
    private Servers(){}
    private static Servers serverObj;
    private static APIInterface apiInterface;
    public static Servers getInstance(){
        if(serverObj==null){
            serverObj = new Servers();
        }
        return  serverObj;
    }
    static {
        apiInterface = APIClient.getClient().create(APIInterface.class);
    }
    public void fetchData(final String url,final ResponseListener responseListener){
        if(apiInterface==null){
            apiInterface = APIClient.getClient().create(APIInterface.class);
        }
        final Call<ResponseBody> resp =  apiInterface.getData(url);
        resp.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String res = response.body().string();
                    if (response.code()==200){
                        Log.d("Resp",res);
                        responseListener.responseObject(res,"");
                    }
                }catch (Exception e){e.printStackTrace();}
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });

    }
}
