function BookForm({ availableSeats, selectedSeatIds, loading, onBook }) {
  const totalFare = availableSeats
    .filter((seat) => selectedSeatIds.includes(seat.seatId))
    .reduce((sum, seat) => sum + seat.estimatedFare, 0);

  return (
    <section className="book-form">
      <h2>Book Seats</h2>
      <p className="hint">
        {selectedSeatIds.length === 0
          ? 'No seats selected yet.'
          : `${selectedSeatIds.length} seat(s) selected — estimated total fare ${totalFare}.`}
      </p>
      <button
        type="button"
        className="primary"
        onClick={onBook}
        disabled={loading || selectedSeatIds.length === 0}
      >
        {loading ? 'Booking...' : `Book ${selectedSeatIds.length || ''} Seat${selectedSeatIds.length === 1 ? '' : 's'}`}
      </button>
    </section>
  );
}

export default BookForm;
