package com.projectwork.Smart.Parking.System.controller;


import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.ParkingSlotRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import com.projectwork.Smart.Parking.System.service.BookingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final ParkingSlotRepository slotRepository;

    public BookingController(BookingService bookingService,
                             UserRepository userRepository,
                             ParkingSlotRepository slotRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.slotRepository = slotRepository;
    }

    @PostMapping("/create")
    public Booking createBooking(@RequestParam Long userId,
                                 @RequestParam Long slotId) {

        User user = userRepository.findById(userId).orElseThrow();
        ParkingSlot slot = slotRepository.findById(slotId).orElseThrow();

        return bookingService.createBooking(user, slot);
    }
}
