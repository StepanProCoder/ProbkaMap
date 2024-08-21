package com.example.probkamap;

import android.util.Log;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.graphhopper.util.shapes.GHPoint;
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

//    public List<GHPoint> requestRoute(double startLat, double startLon, double endLat, double endLon) throws IOException {
//        OkHttpClient client = new OkHttpClient();
//
//        String url = "https://api.openrouteservice.org/v2/directions/cycling-regular?api_key=" + API_KEY +
//                "&start=" + startLon + "," + startLat +
//                "&end=" + endLon + "," + endLat;
//
//        Request request = new Request.Builder()
//                .url(url)
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
//            String responseStr = response.body().string();
//            Log.d("ROUTE", responseStr);
//            List<double[]> coords = parseCoordinates(responseStr);
//
//            List<GHPoint> ghPoints = new ArrayList<>();
//            for (double[] coord : coords) {
//                ghPoints.add(new GHPoint(coord[1], coord[0])); // Поменяйте местами индексы, если порядок широты и долготы не совпадает
//                Log.d("ROUTE", coord[1] + " " + coord[0]);
//            }
//            return ghPoints;
//        }
//    }

    public List<GeoPoint> requestRoute(List<GeoPoint> points) throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();

        // Формируем строку координат
        StringBuilder coordinates = new StringBuilder();
        for (GeoPoint point : points) {
            if (coordinates.length() > 0) {
                coordinates.append("|");
            }
            coordinates.append(point.getLongitude()).append(",").append(point.getLatitude());
        }

        String url = "https://api.openrouteservice.org/v2/directions/cycling-regular/geojson";
        JSONObject jsonBody = new JSONObject();
        JSONArray coordinatesArray = new JSONArray();
        for (GeoPoint point : points) {
            JSONArray coord = new JSONArray();
            coord.put(point.getLongitude());
            coord.put(point.getLatitude());
            coordinatesArray.put(coord);
        }
        jsonBody.put("coordinates", coordinatesArray);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(okhttp3.RequestBody.create(jsonBody.toString(), okhttp3.MediaType.get("application/json; charset=utf-8")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            String responseStr = response.body().string();
            List<double[]> coords = parseCoordinates(responseStr);

            List<GeoPoint> geoPoints = new ArrayList<>();
            for (double[] coord : coords) {
                geoPoints.add(new GeoPoint(coord[1], coord[0])); // Поменяйте местами индексы, если порядок широты и долготы не совпадает
            }
            return geoPoints;
        }
    }
//
//    public List<double[]> parseCoordinates(String jsonString) throws JSONException {
//        // Парсинг JSON ответа
//        Log.d("JSON", jsonString);
//        JSONObject jsonObject = new JSONObject(jsonString);
//        String polyline = jsonObject.getJSONArray("routes").getJSONObject(0).getString("geometry");
//
//        // Декодирование полилинии
//        List<double[]> decodedCoordinates = decodePolyline(polyline);
//
//        return decodedCoordinates;
//    }
//
//    private List<double[]> decodePolyline(String encoded) {
//        List<double[]> poly = new ArrayList<>();
//        int index = 0, len = encoded.length();
//        int lat = 0, lng = 0;
//
//        while (index < len) {
//            int b, shift = 0, result = 0;
//            do {
//                b = encoded.charAt(index++) - 63;
//                result |= (b & 0x1f) << shift;
//                shift += 5;
//            } while (b >= 0x20);
//            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//            lat += dlat;
//
//            shift = 0;
//            result = 0;
//            do {
//                b = encoded.charAt(index++) - 63;
//                result |= (b & 0x1f) << shift;
//                shift += 5;
//            } while (b >= 0x20);
//            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//            lng += dlng;
//
//            double latitude = lat / 1E5;
//            double longitude = lng / 1E5;
//            poly.add(new double[]{latitude, longitude});
//        }
//
//        return poly;
//    }

}
