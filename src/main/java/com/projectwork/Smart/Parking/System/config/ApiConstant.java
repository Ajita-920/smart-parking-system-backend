package com.projectwork.Smart.Parking.System.config;

public final class ApiConstant {

    private ApiConstant() {
    }

    public static final String API_BASE = "/api";

    // Auth
    public static final String AUTH_BASE = API_BASE + "/auth";
    public static final String AUTH_REGISTER = "/register";
    public static final String AUTH_LOGIN = "/login";

    // Admin
    public static final String ADMIN_BASE = API_BASE + "/admin";
    public static final String ADMIN_BOOKINGS = "/bookings";
    public static final String ADMIN_VENDORS = "/vendors";
    public static final String ADMIN_DRIVERS = "/drivers";
    public static final String ADMIN_DASHBOARD = "/dashboard";

    // Booking
    public static final String BOOKING_BASE = API_BASE + "/bookings";
    public static final String BOOKING_CREATE_LEGACY = "/create";
    public static final String BOOKING_MY_LEGACY = "/mybookings";
    public static final String BOOKING_DEBUG_USER = "/debug/current-user";
    public static final String BOOKING_DEBUG_USER_LEGACY = "/debug-user";

    // Parking
    public static final String PARKING_BASE = API_BASE + "/parking";
    public static final String PARKING_THAMEL_NEARBY_LEGACY = "areas/thamel-nearby";
    public static final String PARKING_THAMEL_NEAREST = "/areas/thamel/nearest";
    public static final String PARKING_THAMEL_AVAILABLE_SLOTS = "/areas/thamel/available-slots";


    // Vendor
    public static final String VENDOR_BASE = API_BASE + "/vendors";
    public static final String VENDOR_PARKING_LOCATIONS = "/view/parking-locations";
    public static final String VENDOR_ADD_PARKING_LEGACY = "/addparking";
    public static final String VENDOR_UPDATE_PARKING_AVAILABLE_SLOTS = "/parking-locations/{id}/available-slots";
    public static final String VENDOR_UPDATE_PARKING_LEGACY = "/updateparking/{id}";
    public static final String VENDOR_DASHBOARD = "/dashboard";
    public static final String VENDOR_DASHBOARD_SUMMARY = "/dashboard/summary";

    // Health
    public static final String HEALTH_BASE = API_BASE + "/health";

    // Payment
    public static final String PAYMENT_BASE = API_BASE + "/payment";
    public static final String PAYMENT_KHALTI_INITIATE = "/khalti/initiate";
    public static final String PAYMENT_KHALTI_BASE = PAYMENT_BASE + "/khalti";
    public static final String PAYMENT_KHALTI_VERIFY = "/verify";
}
