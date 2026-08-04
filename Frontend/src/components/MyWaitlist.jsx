function MyWaitlist({ waitlist, loading, onLeave }) {
  return (
    <section className="my-bookings">
      <h2>My Waitlist</h2>
      <p className="hint">
        First-come-first-served: if the current holder cancels, the longest-waiting entry for that
        seat is automatically confirmed.
      </p>
      {loading && <p className="hint">Loading your waitlist...</p>}
      {!loading && waitlist.length === 0 && <p className="hint">You are not on any waitlist.</p>}
      {waitlist.length > 0 && (
        <table className="bookings-table">
          <thead>
            <tr>
              <th>Seat</th>
              <th>Route</th>
              <th>Date</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {waitlist.map((entry) => (
              <tr key={entry.id}>
                <td>{entry.seatNumber}</td>
                <td>{entry.originCode} → {entry.destinationCode}</td>
                <td>{entry.travelDate}</td>
                <td>{entry.status === 'WAITING' ? `Waiting (#${entry.position})` : entry.status}</td>
                <td>
                  {entry.status === 'WAITING' && (
                    <button type="button" className="secondary" onClick={() => onLeave(entry)}>
                      Leave
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

export default MyWaitlist;
