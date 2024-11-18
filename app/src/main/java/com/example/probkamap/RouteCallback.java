package com.example.probkamap;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public interface RouteCallback {
    void onSuccess(List<GeoPoint> route);
    void onFailure(Throwable t);
}

