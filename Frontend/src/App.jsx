import { Fragment, useEffect, useState } from 'react';
import './App.css';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8090/api';

function App() {
  const [stations, setStations] = useState([]);
  const [origin, setOrigin] = useState('');
  const [destination, setDestination] = useState('');
  const [availableSeats, setAvailableSeats] = useState([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [passengerName, setPassengerName] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState('');

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
  }, []);

  const fetchAvailability = async () => {
    if (!origin || !destination || origin === destination) {
      setMessage('Choose a valid origin and destination.');
      return;
    }

    setLoading(true);
    setFetchError('');
    setMessage('');
    setAvailableSeats([]);
    setSelectedSeatIds([]);

    try {
      const response = await fetch(
        `${API_BASE}/availability?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}`
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
      setFetchError('Unable to load seats.');
    } finally {
      setLoading(false);
    }
  };

  const handleBooking = async () => {
    if (selectedSeatIds.length === 0) {
      setMessage('Select at least one seat first.');
      return;
    }
    if (!passengerName.trim()) {
      setMessage('Enter passenger name.');
      return;
    }

    setLoading(true);
    setMessage('');

    try {
      const response = await fetch(`${API_BASE}/bookings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          seatIds: selectedSeatIds,
          originCode: origin,
          destinationCode: destination,
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
          <label>Passenger Name</label>
          <input
            value={passengerName}
            onChange={(e) => setPassengerName(e.target.value)}
            placeholder="Enter passenger name"
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
      </div>
    </div>
  );
}

export default App;
