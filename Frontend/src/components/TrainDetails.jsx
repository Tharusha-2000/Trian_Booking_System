import { formatTime } from '../utils';

function TrainDetails({ trainSchedule }) {
  if (!trainSchedule) return null;

  return (
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
  );
}

export default TrainDetails;
