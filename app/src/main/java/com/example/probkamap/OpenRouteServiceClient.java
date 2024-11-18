package com.example.probkamap;


import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenRouteServiceClient {

    private static final String API_KEY = "5b3ce3597851110001cf62485b16196a277044f2a810ed11d74706ef";

    public List<double[]> parseCoordinates(String jsonString) {
        List<double[]> coordinatesList = new ArrayList<>();

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

        JsonArray featuresArray = jsonObject.getAsJsonArray("features");
        for (int i = 0; i < featuresArray.size(); i++) {
            JsonObject featureObject = featuresArray.get(i).getAsJsonObject();
            JsonObject geometryObject = featureObject.getAsJsonObject("geometry");
            JsonArray coordinatesArray = geometryObject.getAsJsonArray("coordinates");

            for (int j = 0; j < coordinatesArray.size(); j++) {
                JsonArray coordinatePair = coordinatesArray.get(j).getAsJsonArray();
                double longitude = coordinatePair.get(0).getAsDouble();
                double latitude = coordinatePair.get(1).getAsDouble();
                coordinatesList.add(new double[]{longitude, latitude});
            }
        }

        return coordinatesList;
    }

    public void requestRoute(List<GeoPoint> points, String mode, RouteCallback callback) throws JSONException {
        // Создаем и настраиваем Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openrouteservice.org/")
                .build();

        RouteApiService apiService = retrofit.create(RouteApiService.class);

        // Формируем тело запроса
        JSONObject jsonBody = new JSONObject();
        JSONArray coordinatesArray = new JSONArray();
        for (GeoPoint point : points) {
            JSONArray coord = new JSONArray();
            coord.put(point.getLongitude());
            coord.put(point.getLatitude());
            coordinatesArray.put(coord);
        }
        jsonBody.put("coordinates", coordinatesArray);

        JSONArray radiuses = new JSONArray();
        radiuses.put(100000);
        jsonBody.put("radiuses", radiuses);

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody.toString()
        );

        // Создаем вызов Retrofit
        Call<ResponseBody> call = apiService.requestRoute(mode, API_KEY, requestBody);

        // Обрабатываем результат асинхронно
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseStr = response.body().string();
                        List<double[]> coords = parseCoordinates(responseStr);
                        List<GeoPoint> geoPoints = new ArrayList<>();
                        for (double[] coord : coords) {
                            geoPoints.add(new GeoPoint(coord[1], coord[0])); // Поменяйте местами индексы, если порядок широты и долготы не совпадает
                        }
                        callback.onSuccess(geoPoints);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    callback.onFailure(new IOException("Ошибка: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onFailure(t);
            }
        });
    }

}
