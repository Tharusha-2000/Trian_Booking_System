import { Fragment, useEffect, useState } from 'react';
import './App.css';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8090/api';

const todayIsoDate = () => {
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
};

const formatTime = (hhmm) => {
  if (!hhmm) return '';
  const [h, m] = hhmm.split(':').map(Number);
  const period = h >= 12 ? 'PM' : 'AM';
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return `${hour12}:${String(m).padStart(2, '0')} ${period}`;
};

function App() {
  const [stations, setStations] = useState([]);
  const [origin, setOrigin] = useState('');
  const [destination, setDestination] = useState('');
  const [travelDate, setTravelDate] = useState(todayIsoDate());
  const [trainSchedule, setTrainSchedule] = useState(null);
  const [availableSeats, setAvailableSeats] = useState([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [passengerName, setPassengerName] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState('');

  const [authToken, setAuthToken] = useState(() => localStorage.getItem('authToken') || '');
  const [currentUser, setCurrentUser] = useState(() => {
    const stored = localStorage.getItem('currentUser');
    return stored ? JSON.parse(stored) : null;
  });
  const [authMode, setAuthMode] = useState('login');
  const [authEmail, setAuthEmail] = useState('');
  const [authPassword, setAuthPassword] = useState('');
  const [authMessage, setAuthMessage] = useState('');
  const [authLoading, setAuthLoading] = useState(false);

  const [myBookings, setMyBookings] = useState([]);
  const [myBookingsLoading, setMyBookingsLoading] = useState(false);

  const authHeaders = (token) => ({ Authorization: `Bearer ${token}` });

  const loadMyBookings = async (token) => {
    setMyBookingsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/my-bookings`, { headers: authHeaders(token) });
      if (!response.ok) {
        throw new Error('Failed to load bookings');
      }
      const data = await response.json();
      setMyBookings(data);
    } catch (error) {
      console.error('My bookings fetch error:', error);
    } finally {
      setMyBookingsLoading(false);
    }
  };

  useEffect(() => {
    const loadStations = async () => {
      try {

        const response = await fetch(`${API_BASE}/stations`);

        if (!response.ok) {
          throw new Error('Station fetch failed');
        }
        const data = await response.json();
        setStations(data);
        if (data.length >= 2) {
          setOrigin(data[0].code);
          setDestination(data[1].code);
        }
      } catch (error) {
        console.error('Station fetch error:', error);
        setFetchError('Unable to load stations.');
      }
    };

    loadStations();

    const loadTrainSchedule = async () => {
      try {
        const response = await fetch(`${API_BASE}/train-schedule`);
        if (!response.ok) {
          throw new Error('Train schedule fetch failed');
        }
        const data = await response.json();
        setTrainSchedule(data);
      } catch (error) {
        console.error('Train schedule fetch error:', error);
      }
    };

    loadTrainSchedule();

    if (authToken) {
      loadMyBookings(authToken);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleAuthSubmit = async () => {
    if (!authEmail.trim() || !authPassword) {
      setAuthMessage('Enter both email and password.');
      return;
    }

    setAuthLoading(true);
    setAuthMessage('');

    try {
      const response = await fetch(`${API_BASE}/auth/${authMode === 'login' ? 'login' : 'register'}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: authEmail, password: authPassword }),
      });

      if (!response.ok) {
        const raw = await response.text();
        throw new Error(raw || 'Authentication failed');
      }

      const data = await response.json();
      const user = { email: data.email, role: data.role };
      localStorage.setItem('authToken', data.token);
      localStorage.setItem('currentUser', JSON.stringify(user));
      setAuthToken(data.token);
      setCurrentUser(user);
      setAuthEmail('');
      setAuthPassword('');
      loadMyBookings(data.token);
    } catch (error) {
      console.error('Auth error:', error);
      setAuthMessage(error.message || 'Authentication failed');
    } finally {
      setAuthLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('currentUser');
    setAuthToken('');
    setCurrentUser(null);
    setMyBookings([]);
  };

  const handleCancelBooking = async (booking) => {
    try {
      const response = await fetch(`${API_BASE}/bookings/${booking.id}`, {
        method: 'DELETE',
        headers: authHeaders(authToken),
      });
      if (!response.ok) {
        const raw = await response.text();
        throw new Error(raw || 'Cancel failed');
      }
      setMyBookings((bookings) => bookings.filter((b) => b.id !== booking.id));
      setMessage(`Cancelled seat ${booking.seat.seatNumber} for ${booking.travelDate}.`);

      // If the cancelled trip matches what's currently on screen, refresh the seat
      // map so the now-freed seat shows as available immediately.
      if (
        booking.travelDate === travelDate &&
        booking.origin.code === origin &&
        booking.destination.code === destination
      ) {
        fetchAvailability();
      }
    } catch (error) {
      console.error('Cancel booking error:', error);
      setMessage(error.message || 'Failed to cancel booking.');
    }
  };

  const fetchAvailability = async () => {
    if (!origin || !destination || origin === destination) {
      setMessage('Choose a valid origin and destination.');
      return;
    }
    if (!travelDate) {
      setMessage('Choose a travel date.');
      return;
    }

    setLoading(true);
    setFetchError('');
    setMessage('');
    setAvailableSeats([]);
    setSelectedSeatIds([]);

    try {
      const response = await fetch(
        `${API_BASE}/availability?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}&date=${encodeURIComponent(travelDate)}`
      );
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to load availability');
      }
      const data = await response.json();
      setAvailableSeats(data);
      if (data.length === 0) {
        setMessage('No seats available for this leg.');
      }
    } catch (error) {
      console.error('Availability fetch error:', error);
      setFetchError(error.message || 'Unable to load seats.');
    } finally {
      setLoading(false);
    }
  };

  const handleBooking = async () => {
    if (!authToken) {
      setMessage('Sign in to book a seat.');
      return;
    }
    if (selectedSeatIds.length === 0) {
      setMessage('Select at least one seat first.');
      return;
    }

    setLoading(true);
    setMessage('');

    try {
      const response = await fetch(`${API_BASE}/bookings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...authHeaders(authToken) },
        body: JSON.stringify({
          seatIds: selectedSeatIds,
          originCode: origin,
          destinationCode: destination,
          travelDate,
          passengerName,
        }),
      });

      if (!response.ok) {
        // Error bodies come back as plain text (ResponseEntity.body(String)), not JSON,
        // so parse defensively rather than assuming a { message } shape.
        const raw = await response.text();
        let errorMessage = raw || 'Booking failed';
        try {
          const parsed = JSON.parse(raw);
          errorMessage = (parsed && parsed.message) || (typeof parsed === 'string' ? parsed : errorMessage);
        } catch {
          // Not JSON; use the raw text as-is.
        }
        // Booking is all-or-nothing: on any failure none of the selected seats were
        // actually saved, so leave the current selection as-is rather than guessing
        // which seat caused it.
        throw new Error(errorMessage);
      }

      const bookings = await response.json();
      const totalFare = bookings.reduce((sum, booking) => sum + booking.fare, 0);
      const seatNumbers = bookings.map((booking) => booking.seat.seatNumber).join(', ');
      setMessage(
        `Booked ${bookings[0].passengerName} on seat(s) ${seatNumbers} for a total fare of ${totalFare}`
      );
      setAvailableSeats((seats) =>
        seats.map((seat) =>
          selectedSeatIds.includes(seat.seatId) ? { ...seat, available: false } : seat
        )
      );
      setSelectedSeatIds([]);
      setPassengerName('');
      loadMyBookings(authToken);
    } catch (error) {
      console.error('Booking error:', error);
      setMessage(error.message || 'Booking failed');
    } finally {
      setLoading(false);
    }
  };

  const coaches = availableSeats.reduce((acc, seat) => {
    (acc[seat.coachCode] ||= []).push(seat);
    return acc;
  }, {});

  return (
    <div className="app">
      <div className="container">
        <header className="header">
          <h1>Segment-Based Seat Booking</h1>
          <p>Reserve one seat per segment on the Colombo Fort → Badulla route.</p>
        </header>

        {!currentUser ? (
          <section className="auth-panel standalone">
            <div className="auth-form">
              <div className="auth-tabs">
                <button
                  type="button"
                  className={authMode === 'login' ? 'tab active' : 'tab'}
                  onClick={() => setAuthMode('login')}
                >
                  Sign In
                </button>
                <button
                  type="button"
                  className={authMode === 'register' ? 'tab active' : 'tab'}
                  onClick={() => setAuthMode('register')}
                >
                  Sign Up
                </button>
              </div>
              <div className="auth-fields">
                <input
                  type="email"
                  value={authEmail}
                  onChange={(e) => setAuthEmail(e.target.value)}
                  placeholder="Email"
                />
                <input
                  type="password"
                  value={authPassword}
                  onChange={(e) => setAuthPassword(e.target.value)}
                  placeholder="Password"
                />
                <button type="button" className="primary" onClick={handleAuthSubmit} disabled={authLoading}>
                  {authLoading ? 'Please wait...' : authMode === 'login' ? 'Sign In' : 'Sign Up'}
                </button>
              </div>
              {authMessage && <div className="alert error">{authMessage}</div>}
            </div>
          </section>
        ) : (
          <>
            <section className="auth-panel">
              <div className="auth-status">
                <span>
                  Signed in as <strong>{currentUser.email}</strong>
                  {currentUser.role === 'ADMIN' && <span className="role-badge">Admin</span>}
                </span>
                <button type="button" className="secondary" onClick={handleLogout}>
                  Log out
                </button>
              </div>
            </section>

            {trainSchedule && (
              <section className="train-details">
                <div className="train-badge">{trainSchedule.trainName}</div>
                <table className="schedule-table">
                  <thead>
                    <tr>
                      <th>Station</th>
                      <th>Approx. Time</th>
                    </tr>
                  </thead>
                  <tbody>
                    {trainSchedule.stops.map((stop) => (
                      <tr key={stop.stationCode}>
                        <td>{stop.stationName}</td>
                        <td>{formatTime(stop.approximateTime)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </section>
            )}

            <section className="route-form">
              <div className="field">
                <label>Origin</label>
                <select value={origin || ''} onChange={(e) => setOrigin(e.target.value)}>
                  <option value="" disabled>
                    Select origin
                  </option>
                  {stations.map((station) => (
                    <option key={station.id} value={station.code}>
                      {station.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label>Destination</label>
                <select value={destination || ''} onChange={(e) => setDestination(e.target.value)}>
                  <option value="" disabled>
                    Select destination
                  </option>
                  {stations.map((station) => (
                    <option key={station.id} value={station.code}>
                      {station.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label>Travel Date</label>
                <input
                  type="date"
                  value={travelDate}
                  min={todayIsoDate()}
                  onChange={(e) => setTravelDate(e.target.value)}
                />
              </div>

              <button type="button" className="primary" onClick={fetchAvailability} disabled={loading}>
                {loading ? 'Loading...' : 'Check Availability'}
              </button>
            </section>

            {fetchError && <div className="alert error">{fetchError}</div>}
            {message && <div className="alert">{message}</div>}

            <section className="availability">
              <h2>Seat Map</h2>

              <div className="legend">
                <span className="legend-item"><span className="swatch available" /> Available</span>
                <span className="legend-item"><span className="swatch selected" /> Selected</span>
                <span className="legend-item"><span className="swatch occupied" /> Booked for this leg</span>
              </div>

              {availableSeats.length === 0 && !loading && (
                <p className="hint">Choose a route and check availability to see the seat map.</p>
              )}

              {Object.entries(coaches)
                .sort(([a], [b]) => a.localeCompare(b))
                .map(([coachCode, seats]) => {
                  const sorted = [...seats].sort((a, b) =>
                    a.seatNumber.localeCompare(b.seatNumber, undefined, { numeric: true })
                  );
                  const rows = [];
                  for (let i = 0; i < sorted.length; i += 4) {
                    rows.push(sorted.slice(i, i + 4));
                  }

                  return (
                    <div className="coach" key={coachCode}>
                      <div className="coach-header">
                        <span className="coach-name">Coach {coachCode}</span>
                        <span className="coach-class">{sorted[0]?.reserved ? 'Reserved' : 'Unreserved'}</span>
                      </div>
                      <div className="coach-body">
                        {rows.map((row, rowIndex) => (
                          <div className="seat-row" key={rowIndex}>
                            {row.map((seat, seatIndex) => (
                              <Fragment key={seat.seatId}>
                                <button
                                  type="button"
                                  className={`seat ${seat.available ? 'available' : 'occupied'} ${
                                    selectedSeatIds.includes(seat.seatId) ? 'selected' : ''
                                  }`}
                                  onClick={() => {
                                    if (!seat.available) {
                                      setMessage(`Seat ${seat.seatNumber} is already booked for this leg — pick another seat.`);
                                      return;
                                    }
                                    setSelectedSeatIds((ids) =>
                                      ids.includes(seat.seatId)
                                        ? ids.filter((id) => id !== seat.seatId)
                                        : [...ids, seat.seatId]
                                    );
                                  }}
                                  title={seat.available ? 'Available' : 'Already booked for this leg'}
                                >
                                  {seat.seatNumber.split('-')[1] || seat.seatNumber}
                                </button>
                                {seatIndex === 1 && <span className="aisle" />}
                              </Fragment>
                            ))}
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })}
            </section>

            <section className="book-form">
              <h2>Book Seats</h2>
              <p className="hint">
                {selectedSeatIds.length === 0
                  ? 'No seats selected yet.'
                  : `${selectedSeatIds.length} seat(s) selected.`}
              </p>
              <label>Passenger Name (optional)</label>
              <input
                value={passengerName}
                onChange={(e) => setPassengerName(e.target.value)}
                placeholder={currentUser.email}
              />
              <button
                type="button"
                className="primary"
                onClick={handleBooking}
                disabled={loading || selectedSeatIds.length === 0}
              >
                {loading ? 'Booking...' : `Book ${selectedSeatIds.length || ''} Seat${selectedSeatIds.length === 1 ? '' : 's'}`}
              </button>
            </section>

            <section className="my-bookings">
              <h2>My Bookings</h2>
              {myBookingsLoading && <p className="hint">Loading your bookings...</p>}
              {!myBookingsLoading && myBookings.length === 0 && (
                <p className="hint">You have no bookings yet.</p>
              )}
              {myBookings.length > 0 && (
                <table className="bookings-table">
                  <thead>
                    <tr>
                      <th>Seat</th>
                      <th>Route</th>
                      <th>Date</th>
                      <th>Fare</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {myBookings.map((booking) => (
                      <tr key={booking.id}>
                        <td>{booking.seat.seatNumber}</td>
                        <td>{booking.origin.code} → {booking.destination.code}</td>
                        <td>{booking.travelDate}</td>
                        <td>{booking.fare}</td>
                        <td>
                          <button type="button" className="secondary" onClick={() => handleCancelBooking(booking)}>
                            Cancel
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
          </>
        )}
      </div>
    </div>
  );
}

export default App;
