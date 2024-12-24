package com.example.probkamap;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import org.json.JSONObject;

public interface RouteApiService {
    @POST("v2/directions/{mode}/geojson")
    Call<ResponseBody> requestRoute(
            @Path("mode") String mode,
            @Body RequestBody body
    );
}
