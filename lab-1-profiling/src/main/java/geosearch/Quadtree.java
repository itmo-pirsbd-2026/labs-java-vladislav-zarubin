package geosearch;

import java.util.ArrayList;
import java.util.List;

public class Quadtree {
    public enum Variant {
        BASELINE, MEMORY, CPU, FULL
    }

    private static final double WORLD_MIN_LAT = -90.0;
    private static final double WORLD_MIN_LNG = -180.0;
    private static final double WORLD_MAX_LAT = 90.0;
    private static final double WORLD_MAX_LNG = 180.0;
    private static final int NODE_CAPACITY = 8;

    private final Node root = new Node(WORLD_MIN_LAT, WORLD_MIN_LNG, WORLD_MAX_LAT, WORLD_MAX_LNG, NODE_CAPACITY);
    private final boolean directQuadrant;
    private final boolean collectMatchesDirectly;
    private int size;

    public Quadtree(Variant variant) {
        directQuadrant = variant == Variant.CPU || variant == Variant.FULL;
        collectMatchesDirectly = variant == Variant.MEMORY || variant == Variant.FULL;
    }

    public void insert(GeoPoint point) {
        if (!root.insert(point, directQuadrant)) {
            throw new IllegalArgumentException("Точка вне координат");
        }
        size++;
    }

    public List<GeoPoint> searchInBox(double minLat, double minLng, double maxLat, double maxLng) {
        List<GeoPoint> result = new ArrayList<>();
        root.search(minLat, minLng, maxLat, maxLng, result);
        return result;
    }

    public List<GeoPoint> searchInRadius(double lat, double lng, double radius) {
        List<GeoPoint> result = new ArrayList<>();
        double radiusSquared = radius * radius;

        if (collectMatchesDirectly) {
            root.searchInRadius(lat - radius, lng - radius, lat + radius, lng + radius,
                    lat, lng, radiusSquared, result);
            return result;
        }

        List<GeoPoint> candidates = searchInBox(lat - radius, lng - radius, lat + radius, lng + radius);
        for (GeoPoint point : candidates) {
            if (withinRadius(point, lat, lng, radiusSquared)) {
                result.add(point);
            }
        }
        return result;
    }

    public int size() {
        return size;
    }

    private static boolean withinRadius(GeoPoint point, double lat, double lng, double radiusSquared) {
        double dLat = point.lat - lat;
        double dLng = point.lng - lng;
        return dLat * dLat + dLng * dLng <= radiusSquared;
    }

    private static class Node {
        private final double minLat;
        private final double minLng;
        private final double maxLat;
        private final double maxLng;
        private final int capacity;
        private final List<GeoPoint> points = new ArrayList<>();

        private Node southWest;
        private Node southEast;
        private Node northWest;
        private Node northEast;

        Node(double minLat, double minLng, double maxLat, double maxLng, int capacity) {
            this.minLat = minLat;
            this.minLng = minLng;
            this.maxLat = maxLat;
            this.maxLng = maxLng;
            this.capacity = capacity;
        }

        boolean insert(GeoPoint point, boolean directQuadrant) {
            if (!contains(point.lat, point.lng)) {
                return false;
            }
            return insertInside(point, directQuadrant);
        }

        private boolean insertInside(GeoPoint point, boolean directQuadrant) {
            if (southWest == null) {
                double midLat = (minLat + maxLat) / 2.0;
                double midLng = (minLng + maxLng) / 2.0;

                if (points.size() < capacity
                        || midLat == minLat
                        || midLat == maxLat
                        || midLng == minLng
                        || midLng == maxLng) {
                    points.add(point);
                    return true;
                }

                subdivide(midLat, midLng, directQuadrant);
            }

            return insertIntoChildren(point, directQuadrant);
        }

        void search(double searchMinLat, double searchMinLng, double searchMaxLat, double searchMaxLng, List<GeoPoint> result) {
            if (!intersects(searchMinLat, searchMinLng, searchMaxLat, searchMaxLng)) {
                return;
            }

            for (GeoPoint point : points) {
                if (insideBox(point.lat, point.lng, searchMinLat, searchMinLng, searchMaxLat, searchMaxLng)) {
                    result.add(point);
                }
            }

            if (southWest == null) {
                return;
            }

            southWest.search(searchMinLat, searchMinLng, searchMaxLat, searchMaxLng, result);
            southEast.search(searchMinLat, searchMinLng, searchMaxLat, searchMaxLng, result);
            northWest.search(searchMinLat, searchMinLng, searchMaxLat, searchMaxLng, result);
            northEast.search(searchMinLat, searchMinLng, searchMaxLat, searchMaxLng, result);
        }

        void searchInRadius(double searchMinLat, double searchMinLng, double searchMaxLat, double searchMaxLng,
                            double lat, double lng, double radiusSquared, List<GeoPoint> result) {
            if (!intersects(searchMinLat, searchMinLng, searchMaxLat, searchMaxLng)) {
                return;
            }

            for (GeoPoint point : points) {
                if (insideBox(point.lat, point.lng, searchMinLat, searchMinLng, searchMaxLat, searchMaxLng)
                        && withinRadius(point, lat, lng, radiusSquared)) {
                    result.add(point);
                }
            }

            if (southWest == null) {
                return;
            }

            southWest.searchInRadius(searchMinLat, searchMinLng, searchMaxLat, searchMaxLng,
                    lat, lng, radiusSquared, result);
            southEast.searchInRadius(searchMinLat, searchMinLng, searchMaxLat, searchMaxLng,
                    lat, lng, radiusSquared, result);
            northWest.searchInRadius(searchMinLat, searchMinLng, searchMaxLat, searchMaxLng,
                    lat, lng, radiusSquared, result);
            northEast.searchInRadius(searchMinLat, searchMinLng, searchMaxLat, searchMaxLng,
                    lat, lng, radiusSquared, result);
        }

        private boolean contains(double lat, double lng) {
            return insideBox(lat, lng, minLat, minLng, maxLat, maxLng);
        }

        private static boolean insideBox(double lat, double lng, double minLat, double minLng,
                                         double maxLat, double maxLng) {
            return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng;
        }

        private boolean intersects(double searchMinLat, double searchMinLng, double searchMaxLat, double searchMaxLng) {
            return searchMinLat <= maxLat
                    && searchMaxLat >= minLat
                    && searchMinLng <= maxLng
                    && searchMaxLng >= minLng;
        }

        private void subdivide(double midLat, double midLng, boolean directQuadrant) {
            southWest = new Node(minLat, minLng, midLat, midLng, capacity);
            southEast = new Node(minLat, midLng, midLat, maxLng, capacity);
            northWest = new Node(midLat, minLng, maxLat, midLng, capacity);
            northEast = new Node(midLat, midLng, maxLat, maxLng, capacity);

            List<GeoPoint> oldPoints = new ArrayList<>(points);
            points.clear();
            for (GeoPoint point : oldPoints) {
                insertIntoChildren(point, directQuadrant);
            }
        }

        private boolean insertIntoChildren(GeoPoint point, boolean directQuadrant) {
            if (directQuadrant) {
                boolean south = point.lat <= southWest.maxLat;
                boolean west = point.lng <= southWest.maxLng;
                Node child;
                if (south) {
                    child = west ? southWest : southEast;
                } else {
                    child = west ? northWest : northEast;
                }
                return child.insertInside(point, true);
            }

            return southWest.insert(point, false)
                    || southEast.insert(point, false)
                    || northWest.insert(point, false)
                    || northEast.insert(point, false);
        }
    }
}
