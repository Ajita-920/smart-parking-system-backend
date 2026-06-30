package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Finds parking locations by either direct GPS distance or the Thamel road graph.
 */
@Service
public class DijkstraService {

    private static final int DEFAULT_MAX_SPOTS = 5;
    private static final int DEFAULT_GPS_MAX_SPOTS = 20;

    private final ParkingLocationRepository parkingLocationRepository;
    private final GraphService graphService;

    public DijkstraService(
            ParkingLocationRepository parkingLocationRepository,
            GraphService graphService) {
        this.parkingLocationRepository = parkingLocationRepository;
        this.graphService = graphService;
    }


     // Finds available Thamel parking locations by shortest road-graph distance.

    public List<ParkingLocationResponseDto> findNearestByRoadDistance(
            double userLat,
            double userLon,
            Integer maxSpots) {
        int limit = maxSpots != null && maxSpots > 0 ? maxSpots : DEFAULT_MAX_SPOTS;

        List<ParkingLocation> availableLocations = parkingLocationRepository.findByDeletedAtIsNull()
                .stream()
                .filter(location -> location.getAvailableSlots() != null && location.getAvailableSlots() > 0)
                .filter(location -> ThamelBoundary.isInThamel(location.getLatitude(), location.getLongitude()))
                .toList();

        if (availableLocations.isEmpty()) {
            return Collections.emptyList();
        }

        Node userNode = snapToNearestNode(userLat, userLon);
        Map<Node, Double> distances = dijkstra(userNode, graphService.getGraph());

        return availableLocations.stream()
                .map(location -> {
                    Node parkingNode = snapToNearestNode(location.getLatitude(), location.getLongitude());
                    double shortestDistance = distances.getOrDefault(parkingNode, Double.MAX_VALUE);

                    ParkingLocationResponseDto dto = mapToDto(location);
                    dto.setDistance(Math.round(shortestDistance * 100.0) / 100.0);

                    return dto;
                })
                .sorted(Comparator.comparingDouble(ParkingLocationResponseDto::getDistance))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Finds available parking locations by straight-line GPS distance.
     */
    public List<ParkingLocationResponseDto> findNearestByGpsDistance(
            double userLat,
            double userLon,
            Integer maxSpots) {
        int limit = maxSpots != null && maxSpots > 0 ? maxSpots : DEFAULT_GPS_MAX_SPOTS;

        List<ParkingLocation> availableLocations = parkingLocationRepository.findByDeletedAtIsNull()
                .stream()
                .filter(location -> location.getAvailableSlots() != null && location.getAvailableSlots() > 0)
                .toList();

        if (availableLocations.isEmpty()) {
            return Collections.emptyList();
        }

        return availableLocations.stream()
                .map(location -> {
                    ParkingLocationResponseDto dto = mapToDto(location);
                    double distanceKm = haversine(
                            userLat,
                            userLon,
                            location.getLatitude(),
                            location.getLongitude());
                    dto.setDistance(Math.round(distanceKm * 100.0) / 100.0);
                    return dto;
                })
                .sorted(Comparator.comparingDouble(ParkingLocationResponseDto::getDistance))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Returns the nearest available Thamel parking location.
     */
    public ParkingLocationResponseDto findSingleNearestParking(double latitude, double longitude) {
        List<ParkingLocationResponseDto> closestLocations = findNearestByRoadDistance(latitude, longitude, 1);
        return closestLocations.isEmpty() ? null : closestLocations.get(0);
    }

    /**
     * Maps a ParkingLocation entity into the API response object used by search
     * endpoints.
     */
    private ParkingLocationResponseDto mapToDto(ParkingLocation location) {
        ParkingLocationResponseDto dto = new ParkingLocationResponseDto();

        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setAddress(location.getAddress());

        dto.setLatitude(location.getLatitude());
        dto.setLongitude(location.getLongitude());

        dto.setTotalFourWheelerSlots(location.getTotalFourWheelerSlots());
        dto.setAvailableFourWheelerSlots(location.getAvailableFourWheelerSlots());

        dto.setTotalTwoWheelerSlots(location.getTotalTwoWheelerSlots());
        dto.setAvailableTwoWheelerSlots(location.getAvailableTwoWheelerSlots());

        dto.setTotalSlots(location.getTotalSlots());
        dto.setAvailableSlots(location.getAvailableSlots());

        dto.setTwoWheelerRatePerHour(location.getTwoWheelerRatePerHour());
        dto.setFourWheelerRatePerHour(location.getFourWheelerRatePerHour());

        if (location.getVendor() != null) {
            dto.setVendorId(location.getVendor().getId());
            dto.setVendorName(location.getVendor().getName());
        }

        return dto;
    }

    /**
     * Calculates shortest path distances from a source graph node to every other
     * node.
     */
    private Map<Node, Double> dijkstra(Node source, Map<Node, List<Edge>> graph) {
        Map<Node, Double> distances = new HashMap<>();
        PriorityQueue<NodeDistance> priorityQueue = new PriorityQueue<>(
                Comparator.comparingDouble(nodeDistance -> nodeDistance.distance));

        for (Node node : graph.keySet()) {
            distances.put(node, Double.MAX_VALUE);
        }

        distances.put(source, 0.0);
        priorityQueue.add(new NodeDistance(source, 0.0));

        while (!priorityQueue.isEmpty()) {
            NodeDistance current = priorityQueue.poll();

            Node node = current.node;
            double distance = current.distance;

            for (Edge edge : graph.getOrDefault(node, Collections.emptyList())) {
                double newDistance = distance + edge.distance;

                if (newDistance < distances.getOrDefault(edge.to, Double.MAX_VALUE)) {
                    distances.put(edge.to, newDistance);
                    priorityQueue.add(new NodeDistance(edge.to, newDistance));

                }
            }
        }

        return distances;
    }


     //Snaps arbitrary coordinates to the nearest known graph node.

    private Node snapToNearestNode(double latitude, double longitude) {
        return graphService.getGraph()
                .keySet()
                .stream()
                .min(Comparator.comparingDouble(
                        node -> haversine(latitude, longitude, node.lat, node.lon)))
                .orElseThrow(() -> new IllegalStateException("Graph has no nodes."));
    }

    /**
     * Calculates straight-line distance between two latitude/longitude points in
     * kilometers.
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int earthRadiusInKm = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);

        return earthRadiusInKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }


     // Simple bounding-box check for the Thamel area used by the road-graph search.

    public static class ThamelBoundary {
        private static final double THAMEL_MIN_LAT = 27.7100;
        private static final double THAMEL_MAX_LAT = 27.7250;
        private static final double THAMEL_MIN_LON = 85.3100;
        private static final double THAMEL_MAX_LON = 85.3300;

        public static boolean isInThamel(double lat, double lon) {
            return lat >= THAMEL_MIN_LAT
                    && lat <= THAMEL_MAX_LAT
                    && lon >= THAMEL_MIN_LON
                    && lon <= THAMEL_MAX_LON;
        }
    }

    /**
         * Priority-queue entry for Dijkstra's algorithm.
         */
        private record NodeDistance(Node node, double distance) implements Comparable<NodeDistance> {

        @Override
            public int compareTo(NodeDistance other) {
                return Double.compare(this.distance, other.distance);
            }
        }

    /**
     * Graph vertex with stable identity by id.
     */
    public static class Node {
        private final String id;
        private final double lat;
        private final double lon;

        public Node(String id, double lat, double lon) {
            this.id = id;
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Node node && id.equals(node.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    /**
     * Directed graph edge with distance in kilometers.
     */
    public static class Edge {
        private final Node from;
        private final Node to;
        private final double distance;

        public Edge(Node from, Node to, double distance) {
            this.from = from;
            this.to = to;
            this.distance = distance;
        }
    }
}
