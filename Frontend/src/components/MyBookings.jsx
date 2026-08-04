function MyBookings({ bookings, loading, onCancel }) {
  return (
    <section className="my-bookings">
      <h2>My Bookings</h2>
      {loading && <p className="hint">Loading your bookings...</p>}
      {!loading && bookings.length === 0 && <p className="hint">You have no bookings yet.</p>}
      {bookings.length > 0 && (
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
            {bookings.map((booking) => (
              <tr key={booking.id}>
                <td>{booking.seat.seatNumber}</td>
                <td>{booking.origin.code} → {booking.destination.code}</td>
                <td>{booking.travelDate}</td>
                <td>{booking.fare}</td>
                <td>
                  <button type="button" className="secondary" onClick={() => onCancel(booking)}>
                    Cancel
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

export default MyBookings;
