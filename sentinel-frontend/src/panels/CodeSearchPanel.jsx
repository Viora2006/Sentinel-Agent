import { useMemo, useState } from 'react'
import { API_BASE_URL } from '../api'

function CodeSearchPanel({ onOpenProject, onSessionExpired, projects }) {
  const [query, setQuery] = useState('')
  const [selectedProjectId, setSelectedProjectId] = useState('')
  const [results, setResults] = useState([])
  const [status, setStatus] = useState('')
  const [isSearching, setIsSearching] = useState(false)

  const importedProjects = useMemo(
    () => projects.filter((project) => project.imported),
    [projects],
  )

  async function searchCode(event) {
    event?.preventDefault()
    const trimmedQuery = query.trim()

    if (trimmedQuery.length < 2) {
      setResults([])
      setStatus('Enter at least 2 characters to search.')
      return
    }

    setIsSearching(true)
    setStatus('Searching indexed code...')

    try {
      const params = new URLSearchParams({
        query: trimmedQuery,
        limit: '30',
      })
      if (selectedProjectId) {
        params.set('projectId', selectedProjectId)
      }

      const response = await fetch(`${API_BASE_URL}/code-search?${params.toString()}`, {
        credentials: 'include',
      })
      const data = await response.json().catch(() => ({}))

      if (response.status === 401 || response.status === 403) {
        onSessionExpired?.()
        throw new Error('Session expired. Please sign in again.')
      }

      if (!response.ok) throw new Error(data.message || 'Unable to search code.')

      setResults(Array.isArray(data.results) ? data.results : [])
      setStatus(data.resultCount ? `${data.resultCount} result${data.resultCount === 1 ? '' : 's'} found.` : 'No matches yet. Try parsing a project or searching another term.')
    } catch (error) {
      setResults([])
      setStatus(error.message || 'Unable to search code.')
    } finally {
      setIsSearching(false)
    }
  }

  return (
    <section className="panel-stack">
      <form className="search-panel code-search-panel" onSubmit={searchCode}>
        <label>
          <span>Project</span>
          <select onChange={(event) => setSelectedProjectId(event.target.value)} value={selectedProjectId}>
            <option value="">All imported projects</option>
            {importedProjects.map((project) => (
              <option key={project.id} value={project.id}>{project.name}</option>
            ))}
          </select>
        </label>
        <label>
          <span>Search</span>
          <input
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search files, symbols, calls, dependencies..."
            type="search"
            value={query}
          />
        </label>
        <button className="primary-button" disabled={isSearching} type="submit">
          {isSearching ? 'Searching...' : 'Search'}
        </button>
      </form>

      <p className="status-line" role="status">{status}</p>

      <div className="result-list">
        {results.map((result) => (
          <article className="result-card search-result-card" key={`${result.resultType}-${result.relationshipId || result.symbolId || result.fileId}-${result.score}`}>
            <div className="result-card-topline">
              <span className="badge muted">{formatResultType(result)}</span>
              <span>{result.projectName}</span>
            </div>
            <button className="link-button" onClick={() => onOpenProject?.(result.projectId)} type="button">
              {result.filePath}
            </button>
            <h3>{resultTitle(result)}</h3>
            <p>{result.preview}</p>
            <div className="project-meta">
              {result.startLine && <span>Lines {result.startLine}-{result.endLine || result.startLine}</span>}
              {result.relationshipType && <span>{result.relationshipType}</span>}
              <span>Score {result.score}</span>
            </div>
          </article>
        ))}
      </div>

      {!results.length && !status && (
        <section className="empty-state">
          <p className="eyebrow">Repository understanding</p>
          <h2>Search across stored files, parsed symbols, and code relationships.</h2>
          <p>Parse a project first for the best class, method, and dependency results.</p>
        </section>
      )}
    </section>
  )
}

function formatResultType(result) {
  if (result.resultType === 'RELATIONSHIP') return result.relationshipType || 'RELATIONSHIP'
  return result.symbolType || result.resultType
}

function resultTitle(result) {
  if (result.resultType === 'RELATIONSHIP') {
    return `${result.sourceName} ${result.relationshipType} ${result.targetName}`
  }
  if (result.resultType === 'SYMBOL') {
    return `${result.symbolName} (${result.symbolType})`
  }
  return result.filePath
}

export default CodeSearchPanel
