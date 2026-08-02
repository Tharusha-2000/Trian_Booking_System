import { useState } from 'react';
import './App.css';

function App() {
  const [name, setName] = useState('Guest');

  return (
    <div className="app">
      <header className="app-header">
        <h1>Trian Booking System</h1>
        <p>Welcome, {name}.</p>
        <label>
          Your name:
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Enter your name"
          />
        </label>
      </header>
    </div>
  );
}

export default App;
