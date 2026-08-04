# Segment-Based Train Seat Booking System

A booking system for Sri Lanka's Colombo Fort–Badulla line that lets a single reserved seat be booked independently for multiple, non-overlapping legs of the same journey — so a seat vacated partway through the trip becomes bookable again instead of sitting empty for the rest of the route.

Backend: Spring Boot 3.2 (Java 21) + PostgreSQL. Frontend: React + Vite.

## Running the project
/backend/src/main/resources/application.properties here that file add configurations

```bash
docker compose up --build
```

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Both Dockerfiles use multi-stage builds (a Maven image compiles the jar, then it's copied into a slim JRE runtime image), so no local Maven/Node install or wrapper script is required — Docker handles the whole toolchain.

The backend's JWT signing key defaults to a clearly-labeled dev-only placeholder so the stack runs with zero setup. For any real deployment, copy `.env.example` to `.env` and set a real `JWT_SECRET` there — `.env` is gitignored and `docker-compose.yml` reads it automatically.

**Also note:** there is currently no automatic data seeding. Stations, coaches, seats, and the initial admin account all have to be inserted into the database directly (or an admin account created by hand with `role = 'ADMIN'`) before the app is usable end-to-end. Registering a normal account via `POST /api/auth/register` works out of the box and always creates a `USER`-role account.

## Data model

- **Station** — code, name, `ordinal` (position along the route; used for all leg-overlap math).
- **Coach** — code, `reserved` flag, belongs to the train.
- **Seat** — belongs to a `Coach`.
- **SeatBooking** — seat + origin + destination + `travelDate` + owning `User` + fare. This is the unit of segment occupancy.
- **Train / TrainStop** — one train, with an approximate arrival time per station (used for the same-day departure cutoff).
- **User** — email, bcrypt password hash, `role` (`USER` / `ADMIN`).
- **WaitlistEntry** — seat + leg + date + user + `requestedAt` + status, used for FIFO promotion on cancellation.

## Core design decisions

### Segment occupancy via ordinal overlap, not a per-day seat calendar
Each station has an integer `ordinal` (its position on the route). A booking occupies a seat between two ordinals. Availability for a requested leg is just: *does any existing booking for this seat on this date have `origin.ordinal < requestedDestination` and `destination.ordinal > requestedOrigin`?* That single interval-overlap query (`SeatBookingRepository.findOverlappingBookings`) is the entire correctness mechanism for "a seat freed up partway through the trip is bookable again."

**Alternative considered:** a per-segment boolean map (e.g., a bitset of route segments per seat per day). Rejected — it adds a second denormalized structure that has to stay in sync with the bookings table for no real benefit at this route length; the overlap query is O(1) per seat with an index and reads directly from the source of truth.

### Concurrency: `SERIALIZABLE` isolation + retry at the controller, not pessimistic locking
`createBooking` and `cancelBooking` run under `@Transactional(isolation = Isolation.SERIALIZABLE)`. Two concurrent requests racing for the same seat/leg will have one of them aborted by Postgres with a serialization failure.

The important subtlety: Postgres only detects this conflict **at commit time**, which happens *after* the transactional method returns (Spring's proxy commits right after the method body finishes). So the failure can't be caught inside `BookingService` — it has to be caught by whoever *called* the proxied method. `BookingApiController` catches `ConcurrencyFailureException` and retries the whole request (up to 3 times) before giving up with a `409`.

**Alternative considered:** pessimistic locking (`SELECT ... FOR UPDATE` on the seat row, with plain `READ_COMMITTED` isolation). This would avoid retries entirely by serializing conflicting requests at the row level instead of aborting one of them. Not used here because `SERIALIZABLE` + retry was already in place and is simpler to reason about (no explicit lock management), at the cost of retries under contention instead of queuing.

### Fare: distance-based, with demand-based dynamic pricing on top
Base fare is `segments travelled × per-segment rate` (reserved coaches cost more per segment than unreserved). On top of that, `BookingService` computes each coach's occupancy rate for the *specific date and leg* being requested and applies a surge multiplier: ≥80% full → 1.5×, ≥50% → 1.2×, otherwise the plain rate. This is applied consistently everywhere a fare is calculated — quoted in `/api/availability`, charged in `/api/bookings`, and re-priced (at current demand, not the original request time) when a waitlist entry gets promoted.

This is a direct response to the brief's own framing: a reserved seat that sits empty for the rest of the journey can never be resold once the train departs, which is exactly why fares are currently "unfairly" high for partial legs. Surge pricing on nearly-full coaches recaptures some of that otherwise-lost revenue while a quiet coach still books at the plain rate — it doesn't fix the fairness complaint on its own, but it's a step toward the department's stated goal of capturing more of the revenue the current rigid system leaves on the table.

**Alternative considered:** flat time-based pricing (weekday/weekend, or booking-lead-time discounts). Rejected in favor of occupancy-based pricing because it ties directly to the actual problem described in the brief (empty reserved seats vs. overcrowded unreserved coaches), rather than an unrelated pricing lever.

### Auth: stateless JWT, not server-side sessions
Login/register issue a JWT (email + role claim); `JwtAuthFilter` validates it per-request and populates Spring Security's context — no session store. Chosen for fit with a separately-hosted SPA frontend and because it needs no additional infrastructure (no Redis/sticky sessions) for this scale of app. `SecurityConfig` keeps `/api/stations`, `/api/train-schedule`, `/api/availability` public (GET) so browsing doesn't require an account, while booking, cancelling, waitlisting, and `/api/admin/**` all require a valid token (the last one specifically requires `ROLE_ADMIN`).

### Waitlist: FIFO per (seat, date), promotion runs inside the cancellation transaction
Joining the waitlist records intent for a *specific* seat, leg, and date (rejected up front if the seat is actually free — book it directly instead — or if it's already yours). When a booking is cancelled, `cancelBooking` deletes the row and then walks that seat's waitlist in `requestedAt` order, promoting the first entry whose requested leg now fits the freed segment — inside the *same* `SERIALIZABLE` transaction as the delete, so it can't race a concurrent direct booking attempt on that same now-open seat.

**Alternative considered:** a leg/class-level waitlist (queue for "any seat, this route, this date") instead of a specific seat. Rejected — it would need a matching/allocation step at promotion time and doesn't map as cleanly to "first come, first served for the seat that just opened up," which is what was asked for.

### Admin dashboard is occupancy-for-reserved-coaches only
`/api/admin/stats` reports total/date-scoped revenue and bookings, plus a per-coach occupancy breakdown — deliberately scoped to reserved coaches only. Unreserved coaches have no seat assignment at all (first-come-first-served, no `Seat` rows are ever booked against them), so there is nothing meaningful to report per seat for them; showing a fabricated "0%" would misrepresent data the system doesn't actually track.

### Frontend structure: thin `App.jsx` + service layer + presentational components
`api.js` centralizes every backend call; `App.jsx` holds state and handlers and composes one component per section (`SeatMap`, `BookForm`, `MyBookings`, `AdminDashboard`, etc.), each taking data and callback props rather than reaching into global state. This was a deliberate refactor partway through the project once the single-file component grew past ~700 lines and multiple features (auth, waitlist, admin) made it hard to trace what affected what.

## Challenges faced

- **The `SERIALIZABLE` commit-time detection gap.** It wasn't initially obvious that a transaction's conflict surfaces after the method returns, not inside it — the first version of the conflict handling tried (and failed) to catch it inside `BookingService`.
- **Compiler parameter-name reflection.** `@RequestParam`/`@PathVariable` without an explicit name broke at runtime ("Ensure the compiler uses the '-parameters' flag") in several places as new endpoints were added — fixed by always naming them explicitly rather than relying on `-parameters`.
- **Waitlist promotion race safety.** Making sure a promoted waitlist booking couldn't collide with a simultaneous direct booking attempt on the same freed seat meant the promotion logic had to live inside the *same* transaction as the cancellation, not a follow-up step.
- **Iterating on the coach/seat/station "configurability" requirement.** The initial implementation hardcoded coach and station counts directly in a `CommandLineRunner`. That's a real gap against the brief's explicit "configurable, not hardcoded" requirement, and as of this writeup it's an open item — see Known Limitations above.

## Extra credit implemented

- **Seat map visualization** — seats rendered per coach in a row/aisle layout (2 seats | aisle | 2 seats) rather than a flat list, with available/selected/occupied colored states and a demand badge ("Busy"/"High Demand") per coach.
- **Waitlisting** — described above: FIFO per seat+date with automatic promotion on cancellation.
- **Admin dashboard** — revenue and booking totals (all-time and per-date) plus per-coach occupancy, gated to `ROLE_ADMIN`.
- **Clearer booking-conflict handling** — retry-on-conflict for transient `SERIALIZABLE` failures instead of surfacing a raw 500; consistent 400/409 responses via a shared exception handler; the seat map and "My Bookings"/"My Waitlist" views refresh live after a booking or cancellation instead of requiring a manual re-check.
- **Fare logic beyond simple distance-based pricing** — the demand-based surge pricing described above.

## Known limitations / what's left

- No automatic data seeding — a fresh database has no stations, coaches, or admin account.
- No admin UI/API yet for managing stations or coaches, so "configurable, not hardcoded" is only half-solved: the data isn't hardcoded in Java anymore, but there's also no convenient way to configure it besides direct database access.
