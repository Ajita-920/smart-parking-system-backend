package com.projectwork.Smart.Parking.System.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GraphService {

    private Map<DijkstraService.Node, List<DijkstraService.Edge>> graph;

    public GraphService() {
        buildGraph();
    }

    private void buildGraph() {
        graph = new HashMap<>();

        // ---------- REAL PARKING LOCATIONS IN THAMEL ----------
        DijkstraService.Node thamelLot    = new DijkstraService.Node("ThamelParkingLot", 27.71660, 85.30918);
        DijkstraService.Node yakYeti      = new DijkstraService.Node("YakYetiParking", 27.71178, 85.31937);
        DijkstraService.Node chhaya       = new DijkstraService.Node("ChhayaCenterParking", 27.71520, 85.31250);
        DijkstraService.Node jpRoad       = new DijkstraService.Node("JPRoadThamel", 27.71246, 85.30995);

        // ---------- CONNECT NODES WITH ROAD DISTANCES (KM) ----------
        graph.put(thamelLot, List.of(
                new DijkstraService.Edge(thamelLot, chhaya, 0.2),
                new DijkstraService.Edge(thamelLot, jpRoad, 0.3)
        ));

        graph.put(yakYeti, List.of(
                new DijkstraService.Edge(yakYeti, chhaya, 0.25),
                new DijkstraService.Edge(yakYeti, jpRoad, 0.35)
        ));

        graph.put(chhaya, List.of(
                new DijkstraService.Edge(chhaya, thamelLot, 0.2),
                new DijkstraService.Edge(chhaya, yakYeti, 0.25),
                new DijkstraService.Edge(chhaya, jpRoad, 0.15)
        ));

        graph.put(jpRoad, List.of(
                new DijkstraService.Edge(jpRoad, thamelLot, 0.3),
                new DijkstraService.Edge(jpRoad, yakYeti, 0.35),
                new DijkstraService.Edge(jpRoad, chhaya, 0.15)
        ));
    }

    public Map<DijkstraService.Node, List<DijkstraService.Edge>> getGraph() {
        return graph;
    }
}