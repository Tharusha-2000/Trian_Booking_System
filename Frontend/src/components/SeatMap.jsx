import { Fragment } from 'react';

function SeatMap({ availableSeats, selectedSeatIds, onToggleSeat, onJoinWaitlist, loading }) {
  const coaches = availableSeats.reduce((acc, seat) => {
    (acc[seat.coachCode] ||= []).push(seat);
    return acc;
  }, {});

  return (
    <section className="availability">
      <h2>Seat Map</h2>

      <div className="legend">
        <span className="legend-item"><span className="swatch available" /> Available</span>
        <span className="legend-item"><span className="swatch selected" /> Selected</span>
        <span className="legend-item"><span className="swatch occupied" /> Booked — click to join waitlist</span>
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
                <span className="coach-class">
                  {sorted[0]?.reserved ? 'Reserved' : 'Unreserved'}
                  {sorted[0]?.demandLevel && sorted[0].demandLevel !== 'Standard' && (
                    <span className={`demand-badge ${sorted[0].demandLevel === 'High Demand' ? 'high' : 'busy'}`}>
                      {sorted[0].demandLevel}
                    </span>
                  )}
                </span>
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
                          onClick={() => (seat.available ? onToggleSeat(seat.seatId) : onJoinWaitlist(seat))}
                          title={
                            seat.available
                              ? `Available — estimated fare ${seat.estimatedFare} (${seat.demandLevel})`
                              : 'Already booked — click to join the waitlist'
                          }
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
  );
}

export default SeatMap;
