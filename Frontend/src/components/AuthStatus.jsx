function AuthStatus({ currentUser, onLogout }) {
  return (
    <section className="auth-panel">
      <div className="auth-status">
        <span>
          Signed in as <strong>{currentUser.email}</strong>
          {currentUser.role === 'ADMIN' && <span className="role-badge">Admin</span>}
        </span>
        <button type="button" className="secondary" onClick={onLogout}>
          Log out
        </button>
      </div>
    </section>
  );
}

export default AuthStatus;
