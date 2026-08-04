const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api';

const authHeaders = (token) => ({ Authorization: `Bearer ${token}` });

// Spring returns error bodies as plain text (ResponseEntity.body(String)), not JSON,
// so parse defensively rather than assuming a { message } shape.
async function readErrorMessage(response, fallback) {
  const raw = await response.text();
  if (!raw) return fallback;
  try {
    const parsed = JSON.parse(raw);
    return (parsed && parsed.message) || (typeof parsed === 'string' ? parsed : fallback);
  } catch {
    return raw;
  }
}

export const api = {
  async getStations() {
    const response = await fetch(`${API_BASE}/stations`);
    if (!response.ok) throw new Error('Station fetch failed');
    return response.json();
  },

  async getTrainSchedule() {
    const response = await fetch(`${API_BASE}/train-schedule`);
    if (!response.ok) throw new Error('Train schedule fetch failed');
    return response.json();
  },

  async getAvailability(origin, destination, travelDate) {
    const response = await fetch(
      `${API_BASE}/availability?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}&date=${encodeURIComponent(travelDate)}`
    );
    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to load availability'));
    }
    return response.json();
  },

  async login(email, password) {
    const response = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Authentication failed'));
    }
    return response.json();
  },

  async register(email, password) {
    const response = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Authentication failed'));
    }
    return response.json();
  },

  async createBooking(token, payload) {
    const response = await fetch(`${API_BASE}/bookings`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders(token) },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      // Booking is all-or-nothing: on any failure none of the selected seats were
      // actually saved, so callers can leave the current selection as-is.
      throw new Error(await readErrorMessage(response, 'Booking failed'));
    }
    return response.json();
  },

  async getMyBookings(token) {
    const response = await fetch(`${API_BASE}/my-bookings`, { headers: authHeaders(token) });
    if (!response.ok) throw new Error('Failed to load bookings');
    return response.json();
  },

  async cancelBooking(token, id) {
    const response = await fetch(`${API_BASE}/bookings/${id}`, {
      method: 'DELETE',
      headers: authHeaders(token),
    });
    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Cancel failed'));
    }
  },

  async joinWaitlist(token, payload) {
    const response = await fetch(`${API_BASE}/waitlist`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders(token) },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to join waitlist'));
    }
    return response.json();
  },

  async getMyWaitlist(token) {
    const response = await fetch(`${API_BASE}/my-waitlist`, { headers: authHeaders(token) });
    if (!response.ok) throw new Error('Failed to load waitlist');
    return response.json();
  },

  async leaveWaitlist(token, id) {
    const response = await fetch(`${API_BASE}/waitlist/${id}`, {
      method: 'DELETE',
      headers: authHeaders(token),
    });
    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to leave waitlist'));
    }
  },

  async getAdminStats(token, date) {
    const response = await fetch(`${API_BASE}/admin/stats?date=${encodeURIComponent(date)}`, {
      headers: authHeaders(token),
    });
    if (!response.ok) throw new Error('Failed to load admin stats');
    return response.json();
  },
};
