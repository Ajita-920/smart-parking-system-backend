package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DijkstraServiceTest {

    @Mock
    private ParkingLocationRepository parkingLocationRepository;

    private DijkstraService dijkstraService;

    @BeforeEach
    void setUp() {
        dijkstraService = new DijkstraService(parkingLocationRepository, new GraphService());
    }

    @Test
    void findNearestByRoadDistance_shouldReturnAvailableThamelLocationsSortedByShortestGraphDistance() {
        ParkingLocation yakYeti = buildLocation(
                "Yak Yeti Parking",
                27.71178,
                85.31937,
                3);
        ParkingLocation chhaya = buildLocation(
                "Chhaya Center Parking",
                27.71520,
                85.31250,
                2);
        ParkingLocation jpRoad = buildLocation(
                "JP Road Thamel",
                27.71246,
                85.31010,
                4);
        ParkingLocation outsideThamel = buildLocation(
                "Outside Parking",
                27.70000,
                85.35000,
                10);
        ParkingLocation unavailable = buildLocation(
                "Unavailable Thamel Parking",
                27.71660,
                85.30918,
                0);

        when(parkingLocationRepository.findByDeletedAtIsNull())
                .thenReturn(List.of(yakYeti, chhaya, jpRoad, outsideThamel, unavailable));

        List<ParkingLocationResponseDto> result =
                dijkstraService.findNearestByRoadDistance(27.71521, 85.31249, 5);

        assertEquals(List.of("Chhaya Center Parking", "JP Road Thamel", "Yak Yeti Parking"),
                result.stream().map(ParkingLocationResponseDto::getName).toList());
        assertEquals(List.of(0.0, 0.15, 0.25),
                result.stream().map(ParkingLocationResponseDto::getDistance).toList());
    }

    @Test
    void findNearestByRoadDistance_shouldSnapUserCoordinatesNearChhayaCenterToChhayaGraphNode() {
        ParkingLocation chhaya = buildLocation(
                "Chhaya Center Parking",
                27.71520,
                85.31250,
                2);
        ParkingLocation thamelLot = buildLocation(
                "Thamel Parking Lot",
                27.71660,
                85.30918,
                2);

        when(parkingLocationRepository.findByDeletedAtIsNull()).thenReturn(List.of(thamelLot, chhaya));

        List<ParkingLocationResponseDto> result =
                dijkstraService.findNearestByRoadDistance(27.71519, 85.31252, 1);

        assertEquals(1, result.size());
        assertEquals("Chhaya Center Parking", result.get(0).getName());
        assertEquals(0.0, result.get(0).getDistance());
    }

    private ParkingLocation buildLocation(String name, double latitude, double longitude, int availableSlots) {
        ParkingLocation location = new ParkingLocation();
        ReflectionTestUtils.setField(location, "id", UUID.randomUUID());
        location.setName(name);
        location.setAddress("Thamel, Kathmandu");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setTotalFourWheelerSlots(availableSlots);
        location.setAvailableFourWheelerSlots(availableSlots);
        location.setTotalTwoWheelerSlots(0);
        location.setAvailableTwoWheelerSlots(0);
        location.setFourWheelerRatePerHour(100.0);
        location.setTwoWheelerRatePerHour(50.0);
        return location;
    }
}
