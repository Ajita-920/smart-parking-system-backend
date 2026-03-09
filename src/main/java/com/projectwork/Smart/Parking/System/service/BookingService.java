package com.projectwork.Smart.Parking.System.service;


import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingSlotRepository;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ParkingSlotRepository slotRepository;

    public BookingService(BookingRepository bookingRepository,
                          ParkingSlotRepository slotRepository) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
    }

    public Booking createBooking(User user, ParkingSlot slot) {

        if (!slot.getStatus().equals("AVAILABLE")) {
            throw new RuntimeException("Slot not available");
        }

        slot.setStatus("OCCUPIED");
        slotRepository.save(slot);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setStatus("BOOKED");

        return bookingRepository.save(booking);
    }
}
