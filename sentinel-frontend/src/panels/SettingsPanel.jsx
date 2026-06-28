function SettingsPanel({ username }) {
  return (
    <section className="settings-grid">
      <article className="settings-card">
        <span>Username</span>
        <strong>{username || 'Not set'}</strong>
      </article>
      <article className="settings-card">
        <span>API status</span>
        <strong>Waiting for backend</strong>
      </article>
      <article className="settings-card">
        <span>Model selection</span>
        <strong>OpenAI model placeholder</strong>
      </article>
      <article className="settings-card">
        <span>Theme</span>
        <strong>Dark infrastructure</strong>
      </article>
    </section>
  )
}

export default SettingsPanel
