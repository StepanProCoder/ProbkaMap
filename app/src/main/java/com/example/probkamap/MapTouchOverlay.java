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
import com.example.probkamap.algorithms.entity.DFS;
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

    private Polyline editablePolyline;
    private List<GeoPoint> editablePoints;
    private Boolean drawingMode = false;
    private Boolean editingMode = false;
    private static final double CLOSURE_THRESHOLD = 0.01;
    private List<Marker> markers = new ArrayList<>();
    private Marker activeMarker = null;


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

        if (editingMode) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Проверяем, попали ли в маркер
                    for (Marker marker : markers) {
                        if (marker.isDraggable() && marker.hitTest(event, mapView)) {
                            activeMarker = marker;
                            return true; // Начинаем перетаскивание
                        }
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (activeMarker != null) {
                        // Обновляем положение маркера
                        GeoPoint newPosition = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
                        activeMarker.setPosition(newPosition);

                        // Обновляем привязанную точку в editablePoints
                        int index = markers.indexOf(activeMarker);
                        if (index != -1) {
                            editablePoints.set(index, newPosition);
                            editablePolyline.setPoints(editablePoints);
                            mapView.invalidate();
                        }
                        return true; // Продолжаем перетаскивание
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (activeMarker != null) {
                        // Завершаем перетаскивание
                        activeMarker = null;
                        return true; // Обработали завершение
                    }
                    break;
            }
            return true;
        }

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


    private void breakCycleInGraph(DFS dfs, Prim.Vertex start, Prim.Vertex end)
    {
        for (Prim.Vertex vertex: dfs.getGraph())
        {
            if(isPointInsideRectangle(start.getLabel(), end.getLabel(), vertex.getLabel()))
            {
                for (Map.Entry<Prim.Vertex, Prim.Edge> entry: vertex.getEdges().entrySet())
                {
                    Prim.Vertex point = entry.getKey();
                    if(dfs.isReachableWithoutDirectEdge(vertex, point))
                    {
                        vertex.getEdges().remove(point);
                        point.getEdges().remove(vertex);
                        return;
                    }
                }
            }
        }
    }

    private List<GeoPoint> calcRoute(List<GeoPoint> routePoints)
    {
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

        DFS dfs = new DFS(prim.graph);
        breakCycleInGraph(dfs, start, curVertex);

        prim.run();
        Dijkstra.Graph graph = prim.toDijkstra();
        Dijkstra.Node startNode = prim.vertexNode.get(start);
        Dijkstra.Node endNode = prim.vertexNode.get(curVertex);
        Dijkstra dijkstra = new Dijkstra(graph, startNode);
        graph = dijkstra.calculateShortestPathFromSource();
        List<GeoPoint> res = shortestPath(endNode);

        GeoPoint gp = res.remove(0);
        res.add(gp);

        return res;
    }

    private void buildRoute(List<GeoPoint> waypoints) {
        try {
            List<GeoPoint> simplifiedRoute = RamerDouglasPeucker.simplifyRoute(waypoints);
            openRouteServiceClient.requestRoute(simplifiedRoute, "cycling-regular", new RouteCallback() {
                @Override
                public void onSuccess(List<GeoPoint> route) {
                    //displayRouteGeo(simplifiedRoute, 0xFF000000);
                    //displayRouteGeo(route, 0xFF0000FF);
                    displayRouteGeo(calcRoute(route), 0xFFFF0000);
                }

                @Override
                public void onFailure(Throwable t) {

                }
            });

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

    public boolean isPointInsideRectangle(GeoPoint point1, GeoPoint point2, GeoPoint targetPoint) {
        // Найдем минимальные и максимальные значения долготы и широты
        double minLongitude = Math.min(point1.getLongitude(), point2.getLongitude());
        double maxLongitude = Math.max(point1.getLongitude(), point2.getLongitude());
        double minLatitude = Math.min(point1.getLatitude(), point2.getLatitude());
        double maxLatitude = Math.max(point1.getLatitude(), point2.getLatitude());

        // Получаем координаты целевой точки
        double targetLongitude = targetPoint.getLongitude();
        double targetLatitude = targetPoint.getLatitude();

        // Проверяем, находится ли точка в пределах прямоугольника
        return targetLongitude >= minLongitude && targetLongitude <= maxLongitude &&
                targetLatitude >= minLatitude && targetLatitude <= maxLatitude;
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

        editablePoints = routePoints;
        editablePolyline = polyline;

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

    private void clearMarkers()
    {
        // Удаляем старые маркеры
        for (Marker marker : markers) {
            mapView.getOverlays().remove(marker);
        }
        markers.clear();
    }

    private void updateMarkers() {
        clearMarkers();

        // Добавляем новые маркеры на каждой точке полилинии
        for (GeoPoint point : editablePoints) {
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setDraggable(true);

            markers.add(marker);
            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();
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

    public void setEditingMode(Boolean editingMode)
    {
        this.editingMode = editingMode;
        if (!editingMode) {
            clearMarkers();
        } else {
            // Создаем маркеры при входе в режим редактирования
            updateMarkers();
        }
        //setModified(false); // Сбрасываем флаг изменений
        mapView.invalidate();
    }
}
