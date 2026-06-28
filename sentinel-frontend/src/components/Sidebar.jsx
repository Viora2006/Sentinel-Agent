const navItems = [
  ['dashboard', 'Dashboard'],
  ['newProject', 'New Project'],
  ['projects', 'Projects'],
  ['repoImport', 'Repo Import'],
  ['agentTasks', 'Agent Tasks'],
  ['codeSearch', 'Code Search'],
  ['workers', 'Workers'],
  ['settings', 'Settings'],
]

function Sidebar({ activePanel, onSelectPanel }) {
  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="brand-mark">A</div>
        <div>
          <strong>AI Code Agent</strong>
          <span>Infrastructure Console</span>
        </div>
      </div>
      <nav className="sidebar-nav" aria-label="Primary">
        {navItems.map(([id, label]) => (
          <button
            className={activePanel === id ? 'active' : ''}
            key={id}
            onClick={() => onSelectPanel(id)}
            type="button"
          >
            <span className="nav-dot" />
            {label}
          </button>
        ))}
      </nav>
      <div className="sidebar-footer">
        <span>Local UI</span>
        <strong>Backend: localhost:8080</strong>
      </div>
    </aside>
  )
}

export default Sidebar
