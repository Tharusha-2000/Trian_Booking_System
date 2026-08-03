import { useEffect, useState } from 'react';
import './App.css';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8090/api';

function App() {
  const [stations, setStations] = useState([]);
  const [origin, setOrigin] = useState('');
  const [destination, setDestination] = useState('');
  const [availableSeats, setAvailableSeats] = useState([]);
  const [selectedSeatId, setSelectedSeatId] = useState(null);
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
    setSelectedSeatId(null);

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
    if (!selectedSeatId) {
      setMessage('Select a seat first.');
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
          seatId: selectedSeatId,
          originCode: origin,
          destinationCode: destination,
          passengerName,
        }),
      });
      const result = await response.json();
      if (!response.ok) {
        throw new Error(result.message || 'Booking failed');
      }

      setMessage(
        `Booked ${result.passengerName} on seat ${result.seat.seatNumber} for fare ${result.fare}`
      );
      setAvailableSeats((seats) => seats.filter((seat) => seat.seatId !== selectedSeatId));
      setSelectedSeatId(null);
      setPassengerName('');
    } catch (error) {
      console.error('Booking error:', error);
      setMessage(error.message || 'Booking failed');
    } finally {
      setLoading(false);
    }
  };

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
          <h2>Available Seats</h2>
          <div className="seat-grid">
            {availableSeats.map((seat) => (
              <button
                key={seat.seatId}
                type="button"
                className={`seat-card ${selectedSeatId === seat.seatId ? 'selected' : ''}`}
                onClick={() => setSelectedSeatId(seat.seatId)}
              >
                <span className="seat-label">{seat.seatNumber}</span>
                <span className="coach-code">{seat.coachCode}</span>
                <span className="seat-type">{seat.reserved ? 'Reserved' : 'Unreserved'}</span>
              </button>
            ))}
          </div>
        </section>

        <section className="book-form">
          <h2>Book a Seat</h2>
          <label>Passenger Name</label>
          <input
            value={passengerName}
            onChange={(e) => setPassengerName(e.target.value)}
            placeholder="Enter passenger name"
          />
          <button type="button" className="primary" onClick={handleBooking} disabled={loading || !selectedSeatId}>
            {loading ? 'Booking...' : 'Book Seat'}
          </button>
        </section>
      </div>
    </div>
  );
}

export default App;
