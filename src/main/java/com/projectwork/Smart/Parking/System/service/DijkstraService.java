package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DijkstraService {

    private final ParkingLocationRepository parkingLocationRepository;
    private final GraphService graphService;

    public DijkstraService(ParkingLocationRepository parkingLocationRepository, GraphService graphService) {
        this.parkingLocationRepository = parkingLocationRepository;
        this.graphService = graphService;
    }

    private static final int DEFAULT_MAX_SPOTS = 5;

   //finding nearest slots
    public List<ParkingLocationResponseDto> findClosestInThamel(double userLat, double userLon, Integer maxSpots) {
        int limit = (maxSpots != null && maxSpots > 0) ? maxSpots : DEFAULT_MAX_SPOTS;

        List<ParkingLocation> available = parkingLocationRepository.findByAvailableSlotsGreaterThan(0);

        List<ParkingLocation> inThamel = available.stream()
                .filter(loc -> AreaRestriction.isInThamel(loc.getLatitude(), loc.getLongitude()))
                .collect(Collectors.toList());

        if (inThamel.isEmpty()) return Collections.emptyList();

      //mapping to nearest location
        Node userNode = findNearestGraphNode(userLat, userLon);

        // Run Dijkstra
        Map<Node, Double> distances = dijkstra(userNode, graphService.getGraph());

       //mappingdijkstra
        List<ParkingLocationResponseDto> dtos = inThamel.stream()
                .map(loc -> {
                    Node parkingNode = findNearestGraphNode(loc.getLatitude(), loc.getLongitude());
                    double shortestDistance = distances.getOrDefault(parkingNode, Double.MAX_VALUE);

                    ParkingLocationResponseDto dto = mapToDto(loc);
                    dto.setDistance(Math.round(shortestDistance * 100.0) / 100.0);
                    return dto;
                })
                .sorted(Comparator.comparingDouble(ParkingLocationResponseDto::getDistance))
                .limit(limit)
                .collect(Collectors.toList());

        return dtos;
    }

    public ParkingLocationResponseDto findNearestParking(double latitude, double longitude) {
        List<ParkingLocationResponseDto> closest = findClosestInThamel(latitude, longitude, 1);
        return closest.isEmpty() ? null : closest.get(0);
    }

//algorithm
    private Map<Node, Double> dijkstra(Node source, Map<Node, List<Edge>> graph) {
        Map<Node, Double> distances = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));

        for (Node node : graph.keySet()) distances.put(node, Double.MAX_VALUE);
        distances.put(source, 0.0);
        pq.add(new NodeDistance(source, 0.0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            Node node = current.node;
            double dist = current.distance;

            for (Edge e : graph.getOrDefault(node, Collections.emptyList())) {
                double newDist = dist + e.distance;
                if (newDist < distances.get(e.to)) {
                    distances.put(e.to, newDist);
                    pq.add(new NodeDistance(e.to, newDist));
                }
            }
        }

        return distances;
    }

    private Node findNearestGraphNode(double lat, double lon) {
        return graphService.getGraph().keySet().stream()
                .min(Comparator.comparingDouble(n -> haversine(lat, lon, n.lat, n.lon)))
                .orElseThrow(() -> new IllegalStateException("Graph has no nodes"));
    }

    private ParkingLocationResponseDto mapToDto(ParkingLocation loc) {
        ParkingLocationResponseDto dto = new ParkingLocationResponseDto();
        dto.setId(loc.getId());
        dto.setName(loc.getName());
        dto.setAddress(loc.getAddress());
        dto.setLatitude(loc.getLatitude());
        dto.setLongitude(loc.getLongitude());
        dto.setAvailableSlots(loc.getAvailableSlots());
        dto.setVendorName(loc.getVendor() != null ? loc.getVendor().getName() : "Unknown");
        return dto;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

 //specific area only
    public static class AreaRestriction {
        private static final double THAMEL_MIN_LAT = 27.7100;
        private static final double THAMEL_MAX_LAT = 27.7250;
        private static final double THAMEL_MIN_LON = 85.3100;
        private static final double THAMEL_MAX_LON = 85.3300;

        public static boolean isInThamel(double lat, double lon) {
            return lat >= THAMEL_MIN_LAT && lat <= THAMEL_MAX_LAT &&
                    lon >= THAMEL_MIN_LON && lon <= THAMEL_MAX_LON;
        }
    }
//
    private static class NodeDistance {
        Node node;
        double distance;
        NodeDistance(Node node, double distance) { this.node = node; this.distance = distance; }
    }

    public static class Node {
        String id;
        double lat;
        double lon;
        public Node(String id, double lat, double lon) { this.id = id; this.lat = lat; this.lon = lon; }
        @Override public boolean equals(Object o) { return o instanceof Node n && id.equals(n.id); }
        @Override public int hashCode() { return id.hashCode(); }
    }

    public static class Edge {
        Node from;
        Node to;
        double distance;
        public Edge(Node from, Node to, double distance) { this.from = from; this.to = to; this.distance = distance; }
    }
}