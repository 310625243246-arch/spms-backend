# SPMS API Documentation

Base URL: `http://localhost:8080`

All request/response bodies are JSON. Protected endpoints require:

```
Authorization: Bearer <jwt_token>
```

Errors follow this shape (from `GlobalExceptionHandler`):

```json
{
  "timestamp": "2026-09-07T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed.",
  "path": "/api/farmer/bookings",
  "fieldErrors": { "date": "Booking date must be in the future" }
}
```

---

## Auth

### POST /api/auth/login
**Auth:** Public

The `email` field is resolved flexibly: a real email, a Farmer ID (e.g. `FRM-20481`), an Officer ID (e.g. `OFC-1001`), or a phone number all work — matching what each frontend login screen actually asks for.

Request:
```json
{ "email": "farmer@spms.gov.in", "password": "farmer123" }
```

Response `200`:
```json
{
  "token": "eyJhbGciOi...",
  "user": { "id": 4, "name": "Ramesh Kumar", "email": "farmer@spms.gov.in", "role": "FARMER" }
}
```

Errors: `401` invalid credentials.

### POST /api/auth/farmer/register
**Auth:** Public

Request:
```json
{
  "fullName": "Ramesh Kumar",
  "mobile": "9876543210",
  "farmerId": "FRM-20481",
  "village": "Kallakurichi",
  "district": "Kallakurichi",
  "password": "secret123"
}
```
Response: `201 Created`, empty body.
Errors: `400` validation failure, `409` duplicate Farmer ID.

Note: the frontend registration form collects no email, so the backend derives a login identifier `{farmerId}@spms.local` internally — the farmer still logs in with their Farmer ID or mobile number as shown above.

---

## Farmer (role: FARMER)

### GET /api/farmer/dashboard
Response `200`:
```json
{
  "farmer": { "name": "Ramesh Kumar", "farmerId": "FRM-20481", "village": "Kallakurichi", "district": "Kallakurichi", "mobile": "9876543210" },
  "upcomingBooking": { "bookingId": "BK-8841", "centerName": "Kallakurichi Procurement Center 2", "crop": "Paddy", "date": "3 Sept 2026", "timeSlot": "10:00 AM - 10:30 AM", "tokenNumber": "P-103" },
  "queueInfo": { "queuePosition": 3, "totalInQueue": 3, "estimatedWaitMinutes": 10, "status": "Booked" },
  "notifications": [ { "id": "1", "message": "Your slot is confirmed...", "timestamp": "1 Sept, 9:14 AM", "read": false } ],
  "history": [ { "id": "BK-1122", "date": "28 Aug 2026", "crop": "Paddy", "centerName": "Kallakurichi Procurement Center 1", "quantityKg": 820, "amountPaid": 18200, "status": "Completed" } ]
}
```
`upcomingBooking` and `queueInfo` are `null` if the farmer has no active booking.

### GET /api/farmer/procurement-centers
Response `200`: array of `{ id, name, location, district, address, active }` (active centers only).

### GET /api/farmer/crops
Response `200`: array of `{ id, name, description }`.

### POST /api/farmer/bookings
Request:
```json
{ "procurementCenterId": 2, "cropId": 1, "date": "2026-09-10", "startTime": "10:00:00", "quantityKg": 600 }
```
Response `201`:
```json
{
  "bookingId": "BK-4F2A", "tokenNumber": "P-104", "queuePosition": 4, "estimatedWaitMinutes": 15,
  "status": "Booked", "centerName": "Kallakurichi Procurement Center 2", "crop": "Paddy",
  "date": "10 Sep 2026", "timeSlot": "10:00 AM - 10:30 AM"
}
```
Errors: `400` invalid center/date, `404` center or crop not found.

### GET /api/farmer/bookings
Response `200`: array of booking objects (same shape as above).

### GET /api/farmer/bookings/{bookingId}
Response `200`: single booking object. `404` if not found or not owned by the caller.

### PUT /api/farmer/bookings/{bookingId}/cancel
Response `204 No Content`. `400` if already completed.

### GET /api/farmer/procurement-history
Response `200`: array of `{ id, date, crop, centerName, quantityKg, amountPaid, status }` for completed/rejected bookings.

### GET /api/farmer/payments
Response `200`: array of `{ id, amount, paymentDate, status, transactionReference }`.

---

## Officer (role: OFFICER)

### GET /api/officer/dashboard
Response `200`:
```json
{
  "centerName": "Kallakurichi Procurement Center 2",
  "date": "2026-09-07",
  "stats": { "totalBookingsToday": 3, "waitingFarmers": 2, "completedProcurements": 0, "currentToken": "P-101", "quantityProcuredKg": 0 },
  "queue": [ { "tokenNumber": "P-101", "farmerName": "Selvam K", "crop": "Paddy", "slot": "9:30 AM", "status": "Weighing" } ]
}
```

### GET /api/officer/queue
Response `200`: array of queue rows (same shape as `queue` above), for the officer's assigned center, today.

### POST /api/officer/queue/next
Calls the next `WAITING` token, marks it `CALLED`, and moves the booking to `Checked In`.
Response `200`: the updated queue row. `404` if no one is waiting.

### PUT /api/officer/queue/{tokenNumber}/status
Request:
```json
{ "status": "Grading" }
```
Accepted values: `Booked`, `Checked In`, `In Queue`, `Grading`, `Weighing`, `Payment Pending`, `Completed`, `Rejected` (or the enum names, e.g. `GRADING`).
Setting `Completed` automatically creates a demo payment record and a notification.
Response `200`: the updated queue row. `404` if the token doesn't belong to the officer's center.

---

## Admin (role: ADMIN)

### GET /api/admin/dashboard
Response `200`:
```json
{
  "stats": { "totalFarmers": 3, "activeCenters": 3, "bookingsToday": 3, "completedProcurements": 1, "pendingProcurements": 2 },
  "centerActivity": [ { "centerName": "Kallakurichi Procurement Center 1", "bookingsToday": 1, "completed": 1, "pending": 0 } ]
}
```

### GET /api/admin/farmers
Response `200`: array of `{ id, farmerId, name, email, phone, village, district }`.

### GET /api/admin/officers
Response `200`: array of `{ id, officerId, name, email, phone, assignedCenter }`.

### GET /api/admin/bookings
Response `200`: array of `{ bookingId, farmerName, centerName, crop, date, status }`.

### GET /api/admin/procurement-centers
Response `200`: array of `{ id, name, location, district, address, active }`.

### POST /api/admin/procurement-centers
Request: `{ "name": "...", "location": "...", "district": "...", "address": "...", "active": true }`
Response `201`: the created center.

### PUT /api/admin/procurement-centers/{id}
Same body as POST. Response `200`: the updated center. `404` if not found.

### DELETE /api/admin/procurement-centers/{id}
Response `204 No Content`. `404` if not found.

---

## Notifications (any authenticated role)

### GET /api/notifications
Response `200`: array of `{ id, message, timestamp, read }` for the calling user.

### PUT /api/notifications/{id}/read
Response `204 No Content`.

---

## HTTP status codes used throughout

| Code | Meaning |
|---|---|
| 200 | Success |
| 201 | Created |
| 204 | Success, no body (cancel/delete/mark-read) |
| 400 | Bad request / validation failure |
| 401 | Invalid credentials or missing/expired token |
| 403 | Authenticated but wrong role for this endpoint |
| 404 | Resource not found |
| 409 | Duplicate resource (e.g. Farmer ID already registered) |
| 500 | Unexpected server error |
