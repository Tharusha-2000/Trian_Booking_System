# Segment-Based Train Seat Booking System

This repository contains a segment-based booking system for Sri Lanka's Colombo Fort–Badulla train line. It includes a Spring Boot backend and a React/Vite frontend.

## Features

- Segment-based reserved seat availability
- Station, coach, seat, and booking management
- Concurrent booking protection with transactional seat checks
- Configurable route, coaches, and seats
- Swagger/OpenAPI support for backend API docs
- Docker Compose for one-command setup

## Project Structure

- `backend/` — Spring Boot backend
- `Frontend/` — React/Vite frontend
- `docker-compose.yml` — local environment for backend, frontend, and PostgreSQL

## Run locally with Docker

```bash
docker compose up --build
```

Then open:
- Frontend: `http://localhost:5173`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- API docs JSON: `http://localhost:8080/v3/api-docs`

## Available backend endpoints

- `GET /api/stations`
- `GET /api/availability?origin={code}&destination={code}`
- `POST /api/bookings`

## Core design decisions

- The system models bookings per seat and station ordinal, allowing reuse of the same physical seat across non-overlapping legs.
- Seat availability checks use ordinal overlap logic for correct segment booking.
- Fare is calculated per segment, with reserved seats priced higher than unreserved seats.
- Backend uses transactional isolation to avoid race conditions on concurrent seat bookings.
- Configuration is environment-driven so the route and database can be changed without code modifications.

## Notes

- No database credentials are committed. The backend uses environment variables for PostgreSQL.
- Seats and stations are bootstrapped on application startup if the database is empty.
