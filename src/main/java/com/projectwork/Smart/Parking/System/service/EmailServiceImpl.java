package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final JavaMailSender mailSender;

    @Override
    @Async
    public void sendBookingConfirmation(BookingResponseDto booking, String toEmail) {
        if (booking == null || toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping booking confirmation email because booking or recipient email is missing.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Booking Confirmed – Smart Parking System");
            helper.setText(buildBookingConfirmationHtml(booking), true);

            mailSender.send(message);
            log.info("Booking confirmation email sent to {} for booking {}", toEmail, booking.getBookingId());
        } catch (MessagingException e) {
            log.error("Failed to send booking confirmation email to {} for booking {}", toEmail, booking.getBookingId(), e);
        } catch (Exception e) {
            log.error("Unexpected error while sending booking confirmation email to {} for booking {}", toEmail, booking.getBookingId(), e);
        }
    }

    private String buildBookingConfirmationHtml(BookingResponseDto booking) {
        String driverName = booking.getDriverName() != null ? booking.getDriverName() : "Valued Customer";
        String parkingLocation = booking.getParkingLocationName() != null ? booking.getParkingLocationName() : "N/A";
        String slotNumber = booking.getSlotNumber() != null ? booking.getSlotNumber() : "N/A";
        String vehicleType = booking.getVehicleType() != null ? booking.getVehicleType().name() : "N/A";
        String paymentMethod = booking.getPaymentMethod() != null ? booking.getPaymentMethod().name() : "N/A";
        String paymentStatus = booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : "N/A";
        String startTime = booking.getStartTime() != null ? booking.getStartTime().format(DATE_TIME_FORMATTER) : "N/A";
        String endTime = booking.getEndTime() != null ? booking.getEndTime().format(DATE_TIME_FORMATTER) : "N/A";
        String totalAmount = booking.getTotalAmount() != null ? "Rs. " + booking.getTotalAmount() : "Rs. 0.00";

        return """
                <!DOCTYPE html>
                <html lang=\"en\">
                <head>
                    <meta charset=\"UTF-8\">
                    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
                    <title>Booking Confirmed</title>
                </head>
                <body style=\"margin:0; padding:0; background-color:#f4f7fb; font-family:Arial, sans-serif; color:#1f2937;\">
                    <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color:#f4f7fb; padding:24px;\">
                        <tr>
                            <td align=\"center\">
                                <table role=\"presentation\" width=\"100%\" max-width=\"640px\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 8px 24px rgba(0,0,0,0.08);\">
                                    <tr>
                                        <td style=\"background-color:#1e3a5f; padding:24px 32px; color:#ffffff;\">
                                            <h1 style=\"margin:0; font-size:24px;\">Smart Parking System</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style=\"padding:32px;\">
                                            <p style=\"margin:0 0 12px; font-size:18px;\">Hello <strong>%s</strong>, your booking has been confirmed!</p>
                                            <p style=\"margin:0 0 24px; color:#4b5563;\">Your parking reservation is ready. Please find your booking details below.</p>
                                            <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"10\" border=\"1\" style=\"border-collapse:collapse; border-color:#d1d5db; font-size:14px;\">
                                                <tr style=\"background-color:#f3f4f6;\">
                                                    <th align=\"left\" style=\"padding:10px; border:1px solid #d1d5db;\">Field</th>
                                                    <th align=\"left\" style=\"padding:10px; border:1px solid #d1d5db;\">Value</th>
                                                </tr>
                                                <tr>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">Booking ID</td>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">%s</td>
                                                </tr>
                                                <tr style=\"background-color:#f9fafb;\">
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">Parking Location</td>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">Slot Number</td>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">%s</td>
                                                </tr>
                                                <tr style=\"background-color:#f9fafb;\">
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">Vehicle Type</td>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">Start Time</td>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">%s</td>
                                                </tr>
                                                <tr style=\"background-color:#f9fafb;\">
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">End Time</td>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">Total Amount</td>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">%s</td>
                                                </tr>
                                                <tr style=\"background-color:#f9fafb;\">
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">Payment Method</td>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">Payment Status</td>
                                                    <td style=\"padding:10px; border:1px solid #d1d5db;\">%s</td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style=\"background-color:#f3f4f6; padding:20px 32px; font-size:13px; color:#6b7280; text-align:center;\">
                                            Thank you for using Smart Parking System. Please do not reply to this email.
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                driverName,
                booking.getBookingId(),
                parkingLocation,
                slotNumber,
                vehicleType,
                startTime,
                endTime,
                totalAmount,
                paymentMethod,
                paymentStatus
        );
    }
}
