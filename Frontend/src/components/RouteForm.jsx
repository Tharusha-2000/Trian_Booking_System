import { todayIsoDate } from '../utils';

function RouteForm({
  stations,
  origin,
  onOriginChange,
  destination,
  onDestinationChange,
  travelDate,
  onTravelDateChange,
  loading,
  onCheckAvailability,
}) {
  return (
    <section className="route-form">
      <div className="field">
        <label>Origin</label>
        <select value={origin || ''} onChange={(e) => onOriginChange(e.target.value)}>
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
        <select value={destination || ''} onChange={(e) => onDestinationChange(e.target.value)}>
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
          onChange={(e) => onTravelDateChange(e.target.value)}
        />
      </div>

      <button type="button" className="primary" onClick={onCheckAvailability} disabled={loading}>
        {loading ? 'Loading...' : 'Check Availability'}
      </button>
    </section>
  );
}

export default RouteForm;
