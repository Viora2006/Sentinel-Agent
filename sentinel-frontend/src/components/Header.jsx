function Header({ eyebrow, onLogout, title, username }) {
  return (
    <header className="top-header">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
      </div>
      <div className="header-actions">
        <div className="user-pill">
          <span>{username?.slice(0, 1).toUpperCase() || 'U'}</span>
          {username || 'User'}
        </div>
        <button className="secondary-button compact" onClick={onLogout} type="button">
          Logout
        </button>
      </div>
    </header>
  )
}

export default Header
