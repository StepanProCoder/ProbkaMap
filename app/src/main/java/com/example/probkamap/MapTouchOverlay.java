package com.example.probkamap;

import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;

import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import com.example.probkamap.OpenRouteServiceClient;
import com.graphhopper.util.shapes.GHPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapTouchOverlay extends Overlay implements MapEventsReceiver {

    private MapView mapView;
    private GeoPoint currentPoint;
    private OpenRouteServiceClient openRouteServiceClient;
    private Polyline currentPolyline;
    private List<GeoPoint> currentPoints;
    private Boolean drawingMode = false;
    private static final double CLOSURE_THRESHOLD = 0.001;

    public MapTouchOverlay(MapView mapView) {
        super();
        this.mapView = mapView;
        this.openRouteServiceClient = new OpenRouteServiceClient();
        currentPoints = new ArrayList<>();
        currentPolyline = new Polyline();
        mapView.getOverlayManager().add(currentPolyline);
    }

    public void setCurrentPoint(GeoPoint currentPoint) {
        this.currentPoint = currentPoint;
    }

    public GeoPoint getCurrentPoint() {
        return currentPoint;
    }

    private void addMarker(GeoPoint point) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        mapView.getOverlays().add(marker);
        mapView.invalidate(); // Перерисовываем карту
    }

    @Override
    public boolean onTouchEvent(MotionEvent event, MapView mapView) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            // Получаем координаты нажатия
//            GeoPoint touchedPoint = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
//            addMarker(touchedPoint);
//            buildRoute(currentPoint, touchedPoint);
//            return true; // Обработали событие нажатия
//        }
//        return false; // Не обрабатывали событие нажатия

        if(!drawingMode)
        {
            return super.onTouchEvent(event, mapView);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                GeoPoint geoPoint = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
                currentPoints.add(geoPoint);
                currentPolyline.setPoints(currentPoints);
                mapView.invalidate();  // Обновляем карту
                return true;
            case MotionEvent.ACTION_UP:
                buildRoute(currentPoints);
                clearRoute();
                return true;
        }
        return false;
    }

    private void buildRoute(List<GeoPoint> waypoints) {
        try {
            List<GeoPoint> simplifiedRoute = RamerDouglasPeucker.simplifyRoute(waypoints);
            List<GeoPoint> routePoints = openRouteServiceClient.requestRoute(simplifiedRoute, "cycling-regular");

//            if (distanceBetweenPoints(routePoints.get(0), routePoints.get(routePoints.size() - 1)) < CLOSURE_THRESHOLD) {
//                routePoints = routePoints.subList(0, routePoints.size() - 1);
//            }

            Graph graph = new Graph();
            for (int i = 0; i < routePoints.size() - 1; i++) {
                graph.addEdge(routePoints.get(i), routePoints.get(i + 1));
            }

            GeoPoint source = routePoints.get(0);
            GeoPoint destination = routePoints.get(routePoints.size() - 1);
            Map<GeoPoint, GeoPoint> mst = PolylineToGraph.findMST(graph, source);
            Graph mstGraph = new Graph(mst);
            Map<GeoPoint, GeoPoint> shortestPaths = PolylineToGraph.calculateShortestPath(mstGraph, source);
            List<GeoPoint> filteredRoute = PolylineToGraph.reconstructPath(shortestPaths, destination);
            List<GeoPoint> mstRoute = PolylineToGraph.reconstructPath(mst, destination);

            displayRouteGeo(simplifiedRoute, 0xFF00FF00);
            displayRouteGeo(routePoints, 0xFF0000FF);
            displayRouteGeo(mstRoute, 0xFF0FF0F0);
            //displayRouteGeo(filteredRoute, 0xF0F00F0F);
            Log.d("RDP", simplifiedRoute.size()+"");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double distanceBetweenPoints(GeoPoint first, GeoPoint second) {
        double latDistance = Math.pow(first.getLatitude() - second.getLatitude(), 2);
        double lonDistance = Math.pow(first.getLongitude() - second.getLongitude(), 2);
        return Math.sqrt(latDistance + lonDistance);
    }

    public void displayRouteGH(List<GHPoint> routePoints, int color) {
        if (routePoints == null || routePoints.isEmpty()) {
            return;
        }

        Polyline polyline = new Polyline();
        polyline.setColor(color); // Blue color
        polyline.setWidth(5); // Line width

        for (GHPoint point : routePoints) {
            GeoPoint geoPoint = new GeoPoint(point.lat, point.lon);
            polyline.addPoint(geoPoint);
        }

        mapView.getOverlayManager().add(polyline);
        mapView.invalidate(); // Перерисовываем карту
    }

    public void displayRouteGeo(List<GeoPoint> routePoints, int color) {
        if (routePoints == null || routePoints.isEmpty()) {
            return;
        }

        Polyline polyline = new Polyline();
        polyline.setColor(color); // Blue color
        polyline.setWidth(5); // Line width

        for (GeoPoint point : routePoints) {
            GeoPoint geoPoint = new GeoPoint(point.getLatitude(), point.getLongitude());
            polyline.addPoint(geoPoint);
        }

        mapView.getOverlayManager().add(polyline);
        mapView.invalidate(); // Перерисовываем карту
    }

    private void clearRoute() {
        if (currentPolyline != null) {
            mapView.getOverlayManager().remove(currentPolyline);
            mapView.invalidate();  // Перерисовываем карту
            currentPoints.clear();
            currentPolyline = new Polyline();
            mapView.getOverlayManager().add(currentPolyline);
        }
    }

    public void clearAllPolylines() {
        List<Overlay> overlaysToRemove = new ArrayList<>();
        for (Overlay overlay : mapView.getOverlayManager()) {
            if (overlay instanceof Polyline) {
                overlaysToRemove.add(overlay);
            }
        }
        mapView.getOverlayManager().removeAll(overlaysToRemove);
        clearRoute();
        mapView.invalidate(); // Перерисовка карты
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    public void setDrawingMode(Boolean drawingMode)
    {
        this.drawingMode = drawingMode;
    }
}
