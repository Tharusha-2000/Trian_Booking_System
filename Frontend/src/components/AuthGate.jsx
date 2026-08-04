function AuthGate({
  authMode,
  onModeChange,
  authEmail,
  onEmailChange,
  authPassword,
  onPasswordChange,
  authLoading,
  authMessage,
  onSubmit,
}) {
  return (
    <section className="auth-panel standalone">
      <div className="auth-form">
        <div className="auth-tabs">
          <button
            type="button"
            className={authMode === 'login' ? 'tab active' : 'tab'}
            onClick={() => onModeChange('login')}
          >
            Sign In
          </button>
          <button
            type="button"
            className={authMode === 'register' ? 'tab active' : 'tab'}
            onClick={() => onModeChange('register')}
          >
            Sign Up
          </button>
        </div>
        <div className="auth-fields">
          <input
            type="email"
            value={authEmail}
            onChange={(e) => onEmailChange(e.target.value)}
            placeholder="Email"
          />
          <input
            type="password"
            value={authPassword}
            onChange={(e) => onPasswordChange(e.target.value)}
            placeholder="Password"
          />
          <button type="button" className="primary" onClick={onSubmit} disabled={authLoading}>
            {authLoading ? 'Please wait...' : authMode === 'login' ? 'Sign In' : 'Sign Up'}
          </button>
        </div>
        {authMessage && <div className="alert error">{authMessage}</div>}
      </div>
    </section>
  );
}

export default AuthGate;
