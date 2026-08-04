import { useEffect, useState } from 'react';
import './App.css';
import { api } from './api';
import { todayIsoDate } from './utils';
import AuthGate from './components/AuthGate';
import AuthStatus from './components/AuthStatus';
import TrainDetails from './components/TrainDetails';
import RouteForm from './components/RouteForm';
import SeatMap from './components/SeatMap';
import BookForm from './components/BookForm';
import MyBookings from './components/MyBookings';
import MyWaitlist from './components/MyWaitlist';
import AdminDashboard from './components/AdminDashboard';

function App() {
  const [stations, setStations] = useState([]);
  const [origin, setOrigin] = useState('');
  const [destination, setDestination] = useState('');
  const [travelDate, setTravelDate] = useState(todayIsoDate());
  const [trainSchedule, setTrainSchedule] = useState(null);
  const [availableSeats, setAvailableSeats] = useState([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [passengerName] = useState('');
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

  const [myWaitlist, setMyWaitlist] = useState([]);
  const [myWaitlistLoading, setMyWaitlistLoading] = useState(false);

  const [adminDate, setAdminDate] = useState(todayIsoDate());
  const [adminStats, setAdminStats] = useState(null);
  const [adminLoading, setAdminLoading] = useState(false);
  const [adminError, setAdminError] = useState('');

  const loadMyBookings = async (token) => {
    setMyBookingsLoading(true);
    try {
      setMyBookings(await api.getMyBookings(token));
    } catch (error) {
      console.error('My bookings fetch error:', error);
    } finally {
      setMyBookingsLoading(false);
    }
  };

  const loadMyWaitlist = async (token) => {
    setMyWaitlistLoading(true);
    try {
      setMyWaitlist(await api.getMyWaitlist(token));
    } catch (error) {
      console.error('My waitlist fetch error:', error);
    } finally {
      setMyWaitlistLoading(false);
    }
  };

  const loadAdminStats = async (token, date) => {
    setAdminLoading(true);
    setAdminError('');
    try {
      setAdminStats(await api.getAdminStats(token, date));
    } catch (error) {
      console.error('Admin stats fetch error:', error);
      setAdminError(error.message || 'Failed to load admin stats.');
    } finally {
      setAdminLoading(false);
    }
  };

  useEffect(() => {
    api.getStations()
      .then((data) => {
        setStations(data);
        if (data.length >= 2) {
          setOrigin(data[0].code);
          setDestination(data[1].code);
        }
      })
      .catch((error) => {
        console.error('Station fetch error:', error);
        setFetchError('Unable to load stations.');
      });

    api.getTrainSchedule()
      .then(setTrainSchedule)
      .catch((error) => console.error('Train schedule fetch error:', error));

    if (authToken) {
      loadMyBookings(authToken);
      loadMyWaitlist(authToken);
      if (currentUser?.role === 'ADMIN') {
        loadAdminStats(authToken, todayIsoDate());
      }
    }
    
  }, []);

  const handleAuthSubmit = async () => {
    if (!authEmail.trim() || !authPassword) {
      setAuthMessage('Enter both email and password.');
      return;
    }

    setAuthLoading(true);
    setAuthMessage('');

    try {
      const data = authMode === 'login'
        ? await api.login(authEmail, authPassword)
        : await api.register(authEmail, authPassword);

      const user = { email: data.email, role: data.role };
      localStorage.setItem('authToken', data.token);
      localStorage.setItem('currentUser', JSON.stringify(user));
      setAuthToken(data.token);
      setCurrentUser(user);
      setAuthEmail('');
      setAuthPassword('');
      loadMyBookings(data.token);
      loadMyWaitlist(data.token);
      if (user.role === 'ADMIN') {
        loadAdminStats(data.token, adminDate);
      }
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
    setMyWaitlist([]);
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
      const data = await api.getAvailability(origin, destination, travelDate);
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

  const handleCancelBooking = async (booking) => {
    try {
      await api.cancelBooking(authToken, booking.id);
      setMyBookings((bookings) => bookings.filter((b) => b.id !== booking.id));
      setMessage(`Cancelled seat ${booking.seat.seatNumber} for ${booking.travelDate}.`);
      loadMyWaitlist(authToken);

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

  const handleJoinWaitlist = async (seat) => {
    if (!authToken) {
      setMessage('Sign in to join the waitlist.');
      return;
    }

    try {
      const entry = await api.joinWaitlist(authToken, {
        seatId: seat.seatId,
        originCode: origin,
        destinationCode: destination,
        travelDate,
        passengerName,
      });
      setMessage(`Added to the waitlist for seat ${entry.seatNumber} — you are #${entry.position} in line.`);
      loadMyWaitlist(authToken);
    } catch (error) {
      console.error('Join waitlist error:', error);
      setMessage(error.message || 'Failed to join waitlist.');
    }
  };

  const handleLeaveWaitlist = async (entry) => {
    try {
      await api.leaveWaitlist(authToken, entry.id);
      setMyWaitlist((entries) => entries.filter((e) => e.id !== entry.id));
    } catch (error) {
      console.error('Leave waitlist error:', error);
      setMessage(error.message || 'Failed to leave waitlist.');
    }
  };

  const handleToggleSeat = (seatId) => {
    setSelectedSeatIds((ids) => (ids.includes(seatId) ? ids.filter((id) => id !== seatId) : [...ids, seatId]));
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
      const bookings = await api.createBooking(authToken, {
        seatIds: selectedSeatIds,
        originCode: origin,
        destinationCode: destination,
        travelDate,
        passengerName,
      });

      const totalFare = bookings.reduce((sum, booking) => sum + booking.fare, 0);
      const seatNumbers = bookings.map((booking) => booking.seat.seatNumber).join(', ');
      setMessage(`Booked ${bookings[0].passengerName} on seat(s) ${seatNumbers} for a total fare of ${totalFare}`);
      setAvailableSeats((seats) =>
        seats.map((seat) => (selectedSeatIds.includes(seat.seatId) ? { ...seat, available: false } : seat))
      );
      setSelectedSeatIds([]);
      loadMyBookings(authToken);
    } catch (error) {
      console.error('Booking error:', error);
      setMessage(error.message || 'Booking failed');
    } finally {
      setLoading(false);
    }
  };

  const handleAdminDateChange = (date) => {
    setAdminDate(date);
    loadAdminStats(authToken, date);
  };

  return (
    <div className="app">
      <div className="container">
        <header className="header">
          <h1>Segment-Based Seat Booking</h1>
          <p>Reserve one seat per segment on the Colombo Fort → Badulla route.</p>
        </header>

        {!currentUser ? (
          <AuthGate
            authMode={authMode}
            onModeChange={setAuthMode}
            authEmail={authEmail}
            onEmailChange={setAuthEmail}
            authPassword={authPassword}
            onPasswordChange={setAuthPassword}
            authLoading={authLoading}
            authMessage={authMessage}
            onSubmit={handleAuthSubmit}
          />
        ) : (
          <>
            <AuthStatus currentUser={currentUser} onLogout={handleLogout} />

            <TrainDetails trainSchedule={trainSchedule} />

            {currentUser.role === 'USER' && (
              <>
                <RouteForm
                  stations={stations}
                  origin={origin}
                  onOriginChange={setOrigin}
                  destination={destination}
                  onDestinationChange={setDestination}
                  travelDate={travelDate}
                  onTravelDateChange={setTravelDate}
                  loading={loading}
                  onCheckAvailability={fetchAvailability}
                />

                {fetchError && <div className="alert error">{fetchError}</div>}
                {message && <div className="alert">{message}</div>}

                <SeatMap
                  availableSeats={availableSeats}
                  selectedSeatIds={selectedSeatIds}
                  onToggleSeat={handleToggleSeat}
                  onJoinWaitlist={handleJoinWaitlist}
                  loading={loading}
                />

                <BookForm
                  availableSeats={availableSeats}
                  selectedSeatIds={selectedSeatIds}
                  loading={loading}
                  onBook={handleBooking}
                />

                <MyBookings bookings={myBookings} loading={myBookingsLoading} onCancel={handleCancelBooking} />

                <MyWaitlist waitlist={myWaitlist} loading={myWaitlistLoading} onLeave={handleLeaveWaitlist} />
              </>
            )}

            {currentUser.role === 'ADMIN' && (
              <AdminDashboard
                adminDate={adminDate}
                onDateChange={handleAdminDateChange}
                adminStats={adminStats}
                adminLoading={adminLoading}
                adminError={adminError}
              />
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default App;
