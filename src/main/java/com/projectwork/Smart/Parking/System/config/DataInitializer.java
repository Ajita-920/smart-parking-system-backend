package com.projectwork.Smart.Parking.System.config;

import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ParkingLocationRepository parkingLocationRepository;

    public DataInitializer(UserRepository userRepository, ParkingLocationRepository parkingLocationRepository) {
        this.userRepository = userRepository;
        this.parkingLocationRepository = parkingLocationRepository;
    }

    @Override
    public void run(String... args) {
        // Creating a sample vendor if not exists
        if (userRepository.findByEmail("vendor@gmail.com").isEmpty()) {
            User vendor = new User();
            vendor.setName("Kathmandu Parking Vendor");
            vendor.setEmail("vendor@gmail.com");
            vendor.setPassword("$2a$10$dummyhash"); // dummy hashed password
            vendor.setPhone("9841000001");
            vendor.setRole("VENDOR");
            userRepository.save(vendor);
        }

        User vendor = userRepository.findByEmail("vendor@gmail.com").get();

        // Adding sample parking locations
        if (parkingLocationRepository.count() == 0) {
            ParkingLocation p1 = new ParkingLocation();
            p1.setName("Amrit Science Campus Parking");
            p1.setAddress("Thamel, Kathmandu");
            p1.setLatitude(27.7172);
            p1.setLongitude(85.3128);
            p1.setTotalSlots(50);
            p1.setAvailableSlots(35);
            p1.setVendor(vendor);
            parkingLocationRepository.save(p1);

            ParkingLocation p2 = new ParkingLocation();
            p2.setName("New Road Parking");
            p2.setAddress("New Road, Kathmandu");
            p2.setLatitude(27.7100);
            p2.setLongitude(85.3200);
            p2.setTotalSlots(30);
            p2.setAvailableSlots(12);
            p2.setVendor(vendor);
            parkingLocationRepository.save(p2);

            System.out.println("✅ Sample parking data inserted successfully!");
        }
    }
}