import { useState } from 'react'

const API_BASE_URL = 'http://localhost:8080/api/auth'

function AuthPage({ onAuthenticated }) {
  const [form, setForm] = useState({ username: '', password: '' })
  const [status, setStatus] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  function updateField(event) {
    const { name, value } = event.target
    setForm((currentForm) => ({ ...currentForm, [name]: value }))
  }

  async function submitAuth(mode) {
    setStatus('')

    if (!form.username.trim() || !form.password.trim()) {
      setStatus('Enter a username and password to continue.')
      return
    }

    setIsLoading(true)

    try {
      const response = await fetch(`${API_BASE_URL}/${mode}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: form.username.trim(),
          password: form.password,
        }),
      })

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(data.message || `${mode === 'login' ? 'Login' : 'Registration'} failed.`)
      }

      if (data.token) {
        onAuthenticated(data.token, form.username.trim())
        return
      }

      setStatus(data.message || 'Registration succeeded. You can log in now.')
    } catch (error) {
      setStatus(error.message || 'Unable to reach the auth service at localhost:8080.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="auth-copy">
          <div className="brand-mark">A</div>
          <p className="eyebrow">AI infrastructure platform</p>
          <h1>AI Code Agent</h1>
          <p className="auth-tagline">
            Autonomous infrastructure for understanding, testing, and evolving codebases.
          </p>

          <form className="auth-form" onSubmit={(event) => event.preventDefault()}>
            <label>
              <span>Username</span>
              <input
                autoComplete="username"
                name="username"
                onChange={updateField}
                placeholder="tyler"
                type="text"
                value={form.username}
              />
            </label>
            <label>
              <span>Password</span>
              <input
                autoComplete="current-password"
                name="password"
                onChange={updateField}
                placeholder="••••••••"
                type="password"
                value={form.password}
              />
            </label>
            <div className="auth-actions">
              <button className="primary-button" disabled={isLoading} onClick={() => submitAuth('login')} type="button">
                {isLoading ? 'Connecting...' : 'Login'}
              </button>
              <button className="secondary-button" disabled={isLoading} onClick={() => submitAuth('register')} type="button">
                Register
              </button>
            </div>
          </form>

          <p className="status-line" role="status">
            {status}
          </p>
        </div>

        <aside className="auth-visual" aria-label="Product architecture preview">
          <div className="visual-orbit">
            <span>GitHub</span>
            <strong>Agent Core</strong>
            <span>Docker</span>
          </div>
          <div className="pipeline-list">
            <div>
              <span>01</span>
              Import repositories
            </div>
            <div>
              <span>02</span>
              Build code graphs
            </div>
            <div>
              <span>03</span>
              Plan safe changes
            </div>
            <div>
              <span>04</span>
              Run tests in workers
            </div>
          </div>
        </aside>
      </section>
    </main>
  )
}

export default AuthPage
