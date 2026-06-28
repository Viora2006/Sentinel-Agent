import { useState } from 'react'

const results = [
  {
    title: 'UserService.login()',
    file: 'src/main/java/app/auth/UserService.java',
    summary: 'Authenticates credentials, returns a signed JWT, and records login telemetry.',
  },
  {
    title: 'JwtService.generateToken()',
    file: 'src/main/java/app/security/JwtService.java',
    summary: 'Creates claims, applies expiration, and signs tokens for authenticated users.',
  },
  {
    title: 'SecurityConfig.filterChain()',
    file: 'src/main/java/app/security/SecurityConfig.java',
    summary: 'Defines protected routes, stateless sessions, and authentication filters.',
  },
]

function CodeSearchPanel() {
  const [query, setQuery] = useState('')

  return (
    <section className="panel-stack">
      <div className="search-panel">
        <input
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search the codebase semantically..."
          type="search"
          value={query}
        />
        <button className="primary-button" type="button">Search</button>
      </div>

      <p className="panel-note">
        Future versions will combine JavaParser or tree-sitter with embeddings for retrieval-aware code search.
      </p>

      <div className="result-list">
        {results.map((result) => (
          <article className="result-card" key={result.title}>
            <span>{result.file}</span>
            <h3>{result.title}</h3>
            <p>{result.summary}</p>
          </article>
        ))}
      </div>
    </section>
  )
}

export default CodeSearchPanel
