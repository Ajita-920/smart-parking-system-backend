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
    public static final String BOOKING_CANCEL = "/{bookingId}/cancel";
    public static final String BOOKING_DEBUG_USER_LEGACY = "debug-user";

    // ─── Parking ──────────────────────────────────────────────────────────────
    // GET /api/parking-locations → all available (?area=thamel&available=true)
    // GET /api/parking-locations/nearby → N closest (?lat=&lng=&limit=5)
    // GET /api/parking-locations/nearest → single nearest (?lat=&lng=)
    // GET /api/parking-locations/mine → vendor's own locations (VENDOR)
    // POST /api/parking-locations → add location (VENDOR)
    // PUT /api/parking-locations/{id} → full update (VENDOR)
    // PATCH /api/parking-locations/{id}/slots → update available slots only (VENDOR)
    // DELETE /api/parking-locations/{id} → remove location (VENDOR)
    public static final String PARKING_BASE = API_BASE + "/parking-locations";
    public static final String PARKING_NEARBY = "/nearby";
    public static final String PARKING_NEAREST = "/nearest";
    public static final String PARKING_MINE = "/mine";
    public static final String PARKING_BY_ID = "/{id}";
    public static final String PARKING_SLOTS = "/{id}/slots";
    public static final String PARKING_ALL_SLOTS = "/{id}/slots/all";

    // User
    public static final String USER_BASE = API_BASE + "/users";
    public static final String USER_ME = "/me";
   
    // ─── Vendors ──────────────────────────────────────────────────────────────
    // GET /api/vendors/dashboard → vendor dashboard (VENDOR)
    public static final String VENDOR_BASE = API_BASE + "/vendors";
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
    public static final String ADMIN_USER_BAN = "/users/{id}/ban";
    public static final String ADMIN_USER_UNBAN = "/users/{id}/unban";
    public static final String ADMIN_USER_BY_ID = "/users/{id}";
    public static final String ADMIN_VENDOR_APPROVE = "/vendors/{id}/approve";
    public static final String ADMIN_VENDOR_BY_ID = "/vendors/{id}";

    // ─── Payments ─────────────────────────────────────────────────────────────
    // POST /api/payments/khalti/initiate → start Khalti payment
    // GET /api/payments/khalti/verify → verify callback (?pidx=)
    public static final String PAYMENT_BASE = API_BASE + "/payments";
    public static final String PAYMENT_KHALTI_INITIATE = "/khalti/initiate";
    public static final String PAYMENT_KHALTI_VERIFY = "/khalti/verify";

    // ─── Health ───────────────────────────────────────────────────────────────
    public static final String HEALTH_BASE = API_BASE + "/health";
}
