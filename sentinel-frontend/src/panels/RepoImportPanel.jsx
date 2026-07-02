import { useEffect, useMemo, useState } from 'react'
import { API_BASE_URL } from '../api'

const importSteps = [
  'Clone repository',
  'Scan file tree',
  'Save files to database',
  'Summarize metadata',
  'Ready for future retrieval',
]

function RepoImportPanel({
  initialDescription = '',
  initialProjectName = '',
  initialRepoUrl = '',
  onImportComplete,
  onSessionExpired,
}) {
  const [repoUrl, setRepoUrl] = useState(initialRepoUrl)
  const [projectName, setProjectName] = useState(initialProjectName)
  const [description, setDescription] = useState(initialDescription)
  const [status, setStatus] = useState('')
  const [isImporting, setIsImporting] = useState(false)
  const [result, setResult] = useState(null)

  useEffect(() => {
    setRepoUrl(initialRepoUrl)
    setProjectName(initialProjectName || nameFromUrl(initialRepoUrl))
    setDescription(initialDescription)
  }, [initialDescription, initialProjectName, initialRepoUrl])

  const activeStepCount = useMemo(() => {
    if (result) return importSteps.length
    if (isImporting) return 3
    return repoUrl ? 1 : 0
  }, [isImporting, repoUrl, result])

  async function importRepository() {
    setStatus('')
    setResult(null)

    if (!projectName.trim() || !repoUrl.trim()) {
      setStatus('Project name and GitHub repository URL are required.')
      return
    }

    setIsImporting(true)

    try {
      const data = await sendImportRequest(false)

      setResult(data)
      setStatus(data.message || 'Repository imported successfully.')
      onImportComplete?.(data)
    } catch (error) {
      if (error.code === 'DUPLICATE_PROJECT') {
        const shouldUpdate = window.confirm(
          `${error.existingProjectName || 'This project'} already uses this GitHub URL. Update the existing project with a fresh import?`
        )

        if (!shouldUpdate) {
          setStatus('Import cancelled. Existing project was left unchanged.')
          setIsImporting(false)
          return
        }

        try {
          const data = await sendImportRequest(true)
          setResult(data)
          setStatus(data.message || 'Repository updated successfully.')
          onImportComplete?.(data)
        } catch (retryError) {
          setStatus(retryError.message || 'Unable to update repository.')
        } finally {
          setIsImporting(false)
        }
        return
      }

      setStatus(error.message || 'Unable to import repository.')
    } finally {
      setIsImporting(false)
    }
  }

  async function sendImportRequest(updateExisting) {
    const response = await fetch(`${API_BASE_URL}/projects/import`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          projectName: projectName.trim(),
          githubUrl: repoUrl.trim(),
          description: description.trim(),
          updateExisting,
        }),
      })

      const data = await response.json().catch(() => ({}))

      if (response.status === 401 || response.status === 403) {
      onSessionExpired?.()
      throw new Error('Session expired. Please sign in again.')
      }

      if (!response.ok) {
      const error = new Error(data.message || 'Repository import failed.')
      error.code = data.code
      error.existingProjectId = data.existingProjectId
      error.existingProjectName = data.existingProjectName
      throw error
      }

    return data
  }

  return (
    <section className="panel-stack">
      <div className="glass-panel import-console">
        <label>
          <span>Project name</span>
          <input
            onChange={(event) => setProjectName(event.target.value)}
            placeholder="sentinel-core"
            type="text"
            value={projectName}
          />
        </label>
        <label>
          <span>GitHub repo URL</span>
          <input
            onChange={(event) => {
              setRepoUrl(event.target.value)
              if (!projectName.trim()) {
                setProjectName(nameFromUrl(event.target.value))
              }
            }}
            placeholder="https://github.com/org/service"
            type="url"
            value={repoUrl}
          />
        </label>
        <label>
          <span>Description</span>
          <textarea
            onChange={(event) => setDescription(event.target.value)}
            placeholder="What should this project represent in your workspace?"
            rows="3"
            value={description}
          />
        </label>
        <button className="primary-button" disabled={isImporting} onClick={importRepository} type="button">
          {isImporting ? 'Importing...' : 'Import Repository'}
        </button>
      </div>
      <p className="status-line" role="status">{status}</p>

      <div className="pipeline">
        {importSteps.map((step, index) => (
          <article className={index < activeStepCount ? 'pipeline-step active' : 'pipeline-step'} key={step}>
            <span>{index + 1}</span>
            <h3>{step}</h3>
            <p>{index === 4 ? 'Metadata is stored for later parsing and search.' : 'Repository import pipeline stage.'}</p>
          </article>
        ))}
      </div>

      {result && (
        <article className="glass-panel import-result">
          <div>
            <p className="eyebrow">Imported repository</p>
            <h2>{result.projectName}</h2>
            <a href={result.githubUrl} rel="noreferrer" target="_blank">{result.githubUrl}</a>
          </div>
          <div className="import-stats">
            <span>Total files</span>
            <strong>{result.totalFiles}</strong>
          </div>
          <div className="language-summary">
            {Object.entries(result.languages || {}).map(([language, count]) => (
              <span key={language}>{language}: {count}</span>
            ))}
          </div>
        </article>
      )}
    </section>
  )
}

function nameFromUrl(url) {
  const match = url.trim().match(/github\.com\/[^/]+\/([^/?#]+)(?:\.git)?/i)
  return match ? match[1].replace(/\.git$/i, '') : ''
}

export default RepoImportPanel
