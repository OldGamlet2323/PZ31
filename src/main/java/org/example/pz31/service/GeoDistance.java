package org.example.pz31.service;


public final class GeoDistance {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private static final double KM_PER_DEG_LAT = 111.32;

    private GeoDistance() {
    }

    /**
     * Відстань між двома точками по поверхні сфери за формулою Haversine, у кілометрах.
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public static double[] boundingBox(double lat, double lon, double radiusKm) {
        double latDelta = radiusKm / KM_PER_DEG_LAT;

        // На високих широтах градус довготи «коротший»; біля полюсів cos→0.
        double cosLat = Math.cos(Math.toRadians(lat));
        double lonDelta = Math.abs(cosLat) < 1e-6
                ? 180.0 // біля полюса беремо всю довготу
                : radiusKm / (KM_PER_DEG_LAT * cosLat);

        double minLat = Math.max(-90.0, lat - latDelta);
        double maxLat = Math.min(90.0, lat + latDelta);
        double minLon = Math.max(-180.0, lon - lonDelta);
        double maxLon = Math.min(180.0, lon + lonDelta);

        return new double[]{minLat, maxLat, minLon, maxLon};
    }
}
