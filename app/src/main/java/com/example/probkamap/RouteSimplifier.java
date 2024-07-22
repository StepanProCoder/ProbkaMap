package com.example.probkamap;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class RouteSimplifier {

    public static double perpendicularDistance(GeoPoint point, GeoPoint lineStart, GeoPoint lineEnd) {
        double dx = lineEnd.getLongitude() - lineStart.getLongitude();
        double dy = lineEnd.getLatitude() - lineStart.getLatitude();

        double mag = Math.sqrt(dx * dx + dy * dy);
        if (mag > 0.0) {
            dx /= mag;
            dy /= mag;
        }

        double pvx = point.getLongitude() - lineStart.getLongitude();
        double pvy = point.getLatitude() - lineStart.getLatitude();

        double pvDot = dx * pvx + dy * pvy;

        double ax = pvDot * dx;
        double ay = pvDot * dy;

        double rx = pvx - ax;
        double ry = pvy - ay;

        return Math.sqrt(rx * rx + ry * ry);
    }

    public static List<GeoPoint> ramerDouglasPeucker(List<GeoPoint> points, double epsilon) {
        if (points == null || points.size() < 3) {
            return points;
        }

        int index = -1;
        double dmax = 0.0;

        for (int i = 1; i < points.size() - 1; i++) {
            double d = perpendicularDistance(points.get(i), points.get(0), points.get(points.size() - 1));
            if (d > dmax) {
                index = i;
                dmax = d;
            }
        }

        if (dmax > epsilon) {
            List<GeoPoint> recResults1 = ramerDouglasPeucker(points.subList(0, index + 1), epsilon);
            List<GeoPoint> recResults2 = ramerDouglasPeucker(points.subList(index, points.size()), epsilon);

            List<GeoPoint> result = new ArrayList<>(recResults1);
            result.remove(result.size() - 1);
            result.addAll(recResults2);

            return result;
        } else {
            List<GeoPoint> result = new ArrayList<>();
            result.add(points.get(0));
            result.add(points.get(points.size() - 1));
            return result;
        }
    }
}