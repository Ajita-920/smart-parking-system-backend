package com.projectwork.Smart.Parking.System.config;

public final class ApiConstant {

    private ApiConstant() {
    }

    public static final String API_BASE = "/api";

    // ─── Auth ────────────────────────────────────────────────────────────────
    public static final String AUTH_BASE = API_BASE + "/auth";
    public static final String AUTH_REGISTER = "/register"; // POST
    public static final String AUTH_LOGIN = "/login"; // POST
    public static final String AUTH_REFRESH = "/refresh";
    public static final String AUTH_LOGOUT = "/logout";
    public static final String AUTH_LOGOUT_ALL = "/logout-all";

    // ─── Bookings ─────────────────────────────────────────────────────────────
    // POST /api/bookings → create booking
    // GET /api/bookings → all bookings (ADMIN)
    // GET /api/bookings/me → current user's bookings
    // GET /api/bookings/{id} → single booking
    public static final String BOOKING_BASE = API_BASE + "/bookings";
    public static final String BOOKING_ME = "/me";
    public static final String BOOKING_BY_ID = "/{id}";

    // ─── Parking ──────────────────────────────────────────────────────────────
    // GET /api/parking → all available (?area=thamel&available=true)
    // GET /api/parking/nearby → N closest (?lat=&lng=&limit=5)
    // GET /api/parking/nearest → single nearest (?lat=&lng=)
    // GET /api/parking/mine → vendor's own locations (VENDOR)
    // POST /api/parking → add location (VENDOR)
    // PUT /api/parking/{id} → full update (VENDOR)
    // PATCH /api/parking/{id}/slots → update available slots only (VENDOR)
    // DELETE /api/parking/{id} → remove location (VENDOR)
    public static final String PARKING_BASE = API_BASE + "/parking";
    public static final String PARKING_NEARBY = "/nearby";
    public static final String PARKING_NEAREST = "/nearest";
    public static final String PARKING_MINE = "/mine";
    public static final String PARKING_BY_ID = "/{id}";
    public static final String PARKING_SLOTS = "/{id}/slots";
    // Admin
    public static final String ADMIN_VENDORS = "/vendors";
    public static final String ADMIN_DRIVERS = "/drivers";

    // User
    public static final String USER_BASE = API_BASE + "/users";
    public static final String USER_ME = "/myDetail";
    public static final String USER_PROFILE = "/update/profile";

    // Booking
    public static final String BOOKING_CANCEL = "/{bookingId}/cancel";
    public static final String BOOKING_DEBUG_USER_LEGACY = "debug-user";

    // Parking
    public static final String PARKING_MAP_SPACES = "/map-spaces";
    public static final String PARKING_NEARBY_GPS = "/nearby-gps";
    public static final String PARKING_THAMEL_CLOSEST = "/areas/closetothamel";
    public static final String PARKING_THAMEL_NEAREST = "/areas/nearby";
    public static final String PARKING_THAMEL_AVAILABLE_SLOTS = "/areas/thamel/available-slots";

    // ─── Vendors ──────────────────────────────────────────────────────────────
    // GET /api/vendors/dashboard → vendor dashboard (VENDOR)
    public static final String VENDOR_BASE = API_BASE + "/vendors";
    public static final String VENDOR_PARKING_LOCATIONS = "/parking-locations";
    public static final String VENDOR_ADD_PARKING_LEGACY = "/addparking";
    public static final String VENDOR_UPDATE_PARKING_LEGACY = "/updateparking/{id}";
    public static final String VENDOR_MANAGE_SLOT = "/parking-locations/{id}/slot-management";
    public static final String VENDOR_UPDATE_PARKING_RATE = "/parking-locations/{id}/rates";
    public static final String VENDOR_DASHBOARD = "/dashboard";
    public static final String VENDOR_BOOKING_STATUS = "/bookings/{bookingId}/status";

    // ─── Admin ────────────────────────────────────────────────────────────────
    // GET /api/admin/dashboard → platform stats
    // GET /api/admin/bookings → all bookings
    // GET /api/admin/users → all users (?role=VENDOR|DRIVER)
    public static final String ADMIN_BASE = API_BASE + "/admin";
    public static final String ADMIN_DASHBOARD = "/dashboard";
    public static final String ADMIN_BOOKINGS = "/bookings";
    public static final String ADMIN_USERS = "/users";

    // ─── Payments ─────────────────────────────────────────────────────────────
    // POST /api/payments/khalti/initiate → start Khalti payment
    // GET /api/payments/khalti/verify → verify callback (?pidx=)
    public static final String PAYMENT_BASE = API_BASE + "/payments";
    public static final String PAYMENT_KHALTI_INITIATE = "/khalti/initiate";
    public static final String PAYMENT_KHALTI_VERIFY = "/khalti/verify";

    // ─── Health ───────────────────────────────────────────────────────────────
    public static final String HEALTH_BASE = API_BASE + "/health";
}
