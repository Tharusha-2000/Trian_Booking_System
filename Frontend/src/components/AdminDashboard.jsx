function AdminDashboard({ adminDate, onDateChange, adminStats, adminLoading, adminError }) {
  return (
    <section className="admin-panel">
      <h2>Admin Dashboard</h2>

      <div className="admin-totals">
        <div className="stat-tile">
          <span className="stat-label">Total Revenue (all-time)</span>
          <span className="stat-value">{adminStats ? adminStats.totalRevenue : '—'}</span>
        </div>
        <div className="stat-tile">
          <span className="stat-label">Total Bookings (all-time)</span>
          <span className="stat-value">{adminStats ? adminStats.totalBookings : '—'}</span>
        </div>
      </div>

      <div className="field admin-date-field">
        <label>Occupancy / Revenue for date</label>
        <input type="date" value={adminDate} onChange={(e) => onDateChange(e.target.value)} />
      </div>

      {adminError && <div className="alert error">{adminError}</div>}
      {adminLoading && <p className="hint">Loading admin stats...</p>}

      {adminStats && !adminLoading && (
        <>
          <div className="admin-totals">
            <div className="stat-tile">
              <span className="stat-label">Revenue on {adminStats.date}</span>
              <span className="stat-value">{adminStats.revenueForDate}</span>
            </div>
            <div className="stat-tile">
              <span className="stat-label">Bookings on {adminStats.date}</span>
              <span className="stat-value">{adminStats.bookingsForDate}</span>
            </div>
          </div>

          <table className="bookings-table">
            <thead>
              <tr>
                <th>Coach</th>
                <th>Booked / Total Seats</th>
                <th>Occupancy</th>
              </tr>
            </thead>
            <tbody>
              {adminStats.coachOccupancy.map((coach) => (
                <tr key={coach.coachCode}>
                  <td>{coach.coachCode}</td>
                  <td>{coach.bookedSeats} / {coach.totalSeats}</td>
                  <td>{coach.occupancyPercent.toFixed(0)}%</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="hint">
            Unreserved coaches (U1–U5) have no seat assignment, so occupancy isn't tracked per seat for them.
          </p>
        </>
      )}
    </section>
  );
}

export default AdminDashboard;
