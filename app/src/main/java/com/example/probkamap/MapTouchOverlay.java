package com.example.probkamap;

import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;

import org.locationtech.jts.geom.LinearRing;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.PolyOverlayWithIW;
import org.osmdroid.views.overlay.Polyline;

import com.example.probkamap.OpenRouteServiceClient;
import com.example.probkamap.algorithms.entity.Dijkstra;
import com.example.probkamap.algorithms.entity.Prim;
import com.graphhopper.util.shapes.GHPoint;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapTouchOverlay extends Overlay implements MapEventsReceiver {

    private MapView mapView;
    private GeoPoint currentPoint;
    private OpenRouteServiceClient openRouteServiceClient;
    private Polyline currentPolyline;
    private List<GeoPoint> currentPoints;
    private Boolean drawingMode = false;
    private static final double CLOSURE_THRESHOLD = 0.01;

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

            if (distanceBetweenPoints(routePoints.get(0), routePoints.get(routePoints.size() - 1)) < CLOSURE_THRESHOLD) // refactor this workaround, we can remove points of the solution if it is way smaller than original set of points, then recalculate
            {
                routePoints.remove(routePoints.size() - 1);
            }

            Prim prim = new Prim(new ArrayList<>());
            Prim.Vertex start = prim.new Vertex(routePoints.get(0));
            prim.graph.add(start);
            Prim.Vertex curVertex = start;
            for (int i = 1; i < routePoints.size(); i++)
            {
                Prim.Vertex vertexA = curVertex;
                Prim.Vertex vertexB = findVertex(routePoints.get(i), prim);
                Prim.Edge ab = prim.new Edge(distanceBetweenPoints(vertexA.getLabel(), vertexB.getLabel()));
                vertexA.addEdge(vertexB, ab);
                vertexB.addEdge(vertexA, ab);
                curVertex = vertexB;
            }
            prim.run();
            Dijkstra.Graph graph = prim.toDijkstra();
            Dijkstra.Node startNode = prim.vertexNode.get(start);
            Dijkstra.Node endNode = prim.vertexNode.get(curVertex);
            Dijkstra dijkstra = new Dijkstra(graph, startNode);
            graph = dijkstra.calculateShortestPathFromSource();
            List<GeoPoint> res = shortestPath(endNode);

            GeoPoint gp = res.remove(0);
            res.add(gp);

            displayRouteGeo(simplifiedRoute, 0xFFFFFF00);
            displayRouteGeo(routePoints, 0xFF000F0F);
            displayRouteGeo(res, 0xFF00FF00);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Prim.Vertex findVertex(GeoPoint point, Prim prim)
    {
        for (Prim.Vertex curVertex: prim.graph)
        {
            if(point.equals(curVertex.getLabel()))
            {
                return curVertex;
            }
        }
        Prim.Vertex vertex = prim.new Vertex(point);
        prim.graph.add(vertex);
        return vertex;
    }

    public List<GeoPoint> shortestPath(Dijkstra.Node endNode)
    {
        List<GeoPoint> res = new ArrayList<>();
        LinkedList<Dijkstra.Node> path = endNode.getShortestPath();
        res.add(endNode.getLabel());
        for (Dijkstra.Node curNode: path)
        {
            res.add(curNode.getLabel());
        }
        return res;
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

        try {
            try {
                Log.d("LRING", getLines(polyline).toString());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        mapView.getOverlayManager().add(polyline);
        mapView.invalidate(); // Перерисовываем карту
    }

    ArrayList<GeoPoint> getLines(Polyline polyline) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        Field field = PolyOverlayWithIW.class.getDeclaredField("mOutline");
        field.setAccessible(true);
        Field field2 = Class.forName("org.osmdroid.views.overlay.LinearRing").getDeclaredField("mOriginalPoints");
        field2.setAccessible(true);
        return (ArrayList<GeoPoint>) field2.get(field.get(polyline));
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
