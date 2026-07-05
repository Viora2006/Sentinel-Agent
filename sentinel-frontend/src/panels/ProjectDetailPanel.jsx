import { useEffect, useMemo, useState } from 'react'
import { API_BASE_URL } from '../api'

function ProjectDetailPanel({ onBack, onSessionExpired, projectId }) {
  const [project, setProject] = useState(null)
  const [files, setFiles] = useState([])
  const [selectedFileId, setSelectedFileId] = useState(null)
  const [selectedFile, setSelectedFile] = useState(null)
  const [symbols, setSymbols] = useState([])
  const [relationships, setRelationships] = useState([])
  const [status, setStatus] = useState('')
  const [parseStatus, setParseStatus] = useState('')
  const [isParsing, setIsParsing] = useState(false)

  useEffect(() => {
    if (!projectId) return

    let isCurrent = true

    async function loadProject() {
      setStatus('Loading project...')
      setSelectedFile(null)

      try {
        const [projectResponse, filesResponse, symbolsResponse, relationshipsResponse] = await Promise.all([
          fetch(`${API_BASE_URL}/projects/${projectId}`, {
            credentials: 'include',
          }),
          fetch(`${API_BASE_URL}/projects/${projectId}/files`, {
            credentials: 'include',
          }),
          fetch(`${API_BASE_URL}/projects/${projectId}/code/symbols`, {
            credentials: 'include',
          }),
          fetch(`${API_BASE_URL}/projects/${projectId}/code/relationships`, {
            credentials: 'include',
          }),
        ])

        const projectData = await projectResponse.json().catch(() => ({}))
        const filesData = await filesResponse.json().catch(() => [])
        const symbolsData = await symbolsResponse.json().catch(() => [])
        const relationshipsData = await relationshipsResponse.json().catch(() => [])

        if (
          projectResponse.status === 401 ||
          projectResponse.status === 403 ||
          filesResponse.status === 401 ||
          filesResponse.status === 403 ||
          symbolsResponse.status === 401 ||
          symbolsResponse.status === 403 ||
          relationshipsResponse.status === 401 ||
          relationshipsResponse.status === 403
        ) {
          onSessionExpired?.()
          throw new Error('Session expired. Please sign in again.')
        }

        if (!projectResponse.ok) throw new Error(projectData.message || 'Unable to load project.')
        if (!filesResponse.ok) throw new Error(filesData.message || 'Unable to load project files.')
        if (!symbolsResponse.ok) throw new Error(symbolsData.message || 'Unable to load code symbols.')
        if (!relationshipsResponse.ok) throw new Error(relationshipsData.message || 'Unable to load code relationships.')

        if (isCurrent) {
          setProject(projectData)
          setFiles(filesData)
          setSymbols(Array.isArray(symbolsData) ? symbolsData : [])
          setRelationships(Array.isArray(relationshipsData) ? relationshipsData : [])
          setSelectedFileId(filesData[0]?.fileId || null)
          setStatus('')
          setParseStatus('')
        }
      } catch (error) {
        if (isCurrent) {
          setStatus(error.message || 'Unable to load project.')
        }
      }
    }

    loadProject()

    return () => {
      isCurrent = false
    }
  }, [onSessionExpired, projectId])

  useEffect(() => {
    if (!projectId || !selectedFileId) {
      setSelectedFile(null)
      return
    }

    let isCurrent = true

    async function loadFile() {
      try {
        const response = await fetch(`${API_BASE_URL}/projects/${projectId}/files/${selectedFileId}`, {
          credentials: 'include',
        })
        const data = await response.json().catch(() => ({}))

        if (response.status === 401 || response.status === 403) {
          onSessionExpired?.()
          throw new Error('Session expired. Please sign in again.')
        }

        if (!response.ok) throw new Error(data.message || 'Unable to load file.')

        if (isCurrent) {
          setSelectedFile(data)
        }
      } catch (error) {
        if (isCurrent) {
          setStatus(error.message || 'Unable to load file.')
        }
      }
    }

    loadFile()

    return () => {
      isCurrent = false
    }
  }, [onSessionExpired, projectId, selectedFileId])

  const languageSummary = useMemo(() => Object.entries(project?.languages || {}), [project])
  const selectedFileSymbols = useMemo(
    () => symbols.filter((symbol) => symbol.fileId === selectedFileId),
    [selectedFileId, symbols],
  )

  async function refreshCodeAnalysis() {
    const [symbolsResponse, relationshipsResponse] = await Promise.all([
      fetch(`${API_BASE_URL}/projects/${projectId}/code/symbols`, {
        credentials: 'include',
      }),
      fetch(`${API_BASE_URL}/projects/${projectId}/code/relationships`, {
        credentials: 'include',
      }),
    ])

    const symbolsData = await symbolsResponse.json().catch(() => [])
    const relationshipsData = await relationshipsResponse.json().catch(() => [])

    if (
      symbolsResponse.status === 401 ||
      symbolsResponse.status === 403 ||
      relationshipsResponse.status === 401 ||
      relationshipsResponse.status === 403
    ) {
      onSessionExpired?.()
      throw new Error('Session expired. Please sign in again.')
    }

    if (!symbolsResponse.ok) throw new Error(symbolsData.message || 'Unable to load code symbols.')
    if (!relationshipsResponse.ok) throw new Error(relationshipsData.message || 'Unable to load code relationships.')

    setSymbols(Array.isArray(symbolsData) ? symbolsData : [])
    setRelationships(Array.isArray(relationshipsData) ? relationshipsData : [])
  }

  async function handleParseProject() {
    if (!projectId || isParsing) return

    setIsParsing(true)
    setParseStatus('Parsing Java source files...')

    try {
      const response = await fetch(`${API_BASE_URL}/projects/${projectId}/code/parse`, {
        credentials: 'include',
        method: 'POST',
      })
      const data = await response.json().catch(() => ({}))

      if (response.status === 401 || response.status === 403) {
        onSessionExpired?.()
        throw new Error('Session expired. Please sign in again.')
      }

      if (!response.ok) throw new Error(data.message || 'Unable to parse project.')

      await refreshCodeAnalysis()
      setParseStatus(`Parsed ${data.parsedFiles} file${data.parsedFiles === 1 ? '' : 's'} and indexed ${data.symbolsCreated} symbol${data.symbolsCreated === 1 ? '' : 's'}.`)
    } catch (error) {
      setParseStatus(error.message || 'Unable to parse project.')
    } finally {
      setIsParsing(false)
    }
  }

  if (!projectId) {
    return (
      <section className="empty-state">
        <p className="eyebrow">No project selected</p>
        <h2>Open a project from the Projects page.</h2>
        <button className="primary-button" onClick={onBack} type="button">Back to Projects</button>
      </section>
    )
  }

  return (
    <section className="panel-stack">
      <div className="glass-panel project-detail-header">
        <div>
          <p className="eyebrow">Project database</p>
          <h2>{project?.projectName || 'Project'}</h2>
          {project?.githubUrl && <a href={project.githubUrl} rel="noreferrer" target="_blank">{project.githubUrl}</a>}
          <p>{project?.description || 'No description added yet.'}</p>
        </div>
        <div className="project-detail-actions">
          <button className="primary-button compact" disabled={isParsing || !project} onClick={handleParseProject} type="button">
            {isParsing ? 'Parsing...' : 'Parse Code'}
          </button>
          <button className="secondary-button compact" onClick={onBack} type="button">Back</button>
          <span className="badge">{project?.status || 'Loading'}</span>
        </div>
      </div>

      <p className="status-line" role="status">{status}</p>
      <p className="status-line" role="status">{parseStatus}</p>

      <div className="project-detail-layout">
        <aside className="glass-panel file-browser">
          <div className="file-browser-summary">
            <strong>{files.length}</strong>
            <span>stored files</span>
          </div>
          <div className="language-summary">
            {languageSummary.slice(0, 6).map(([language, count]) => (
              <span key={language}>{language}: {count}</span>
            ))}
          </div>
          <div className="file-list">
            {files.map((file) => (
              <button
                className={file.fileId === selectedFileId ? 'file-row active' : 'file-row'}
                key={file.fileId}
                onClick={() => setSelectedFileId(file.fileId)}
                type="button"
              >
                <span>{file.filePath}</span>
                <small>{file.language}</small>
              </button>
            ))}
          </div>
        </aside>

        <article className="glass-panel file-preview">
          {selectedFile ? (
            <>
              <div className="file-preview-header">
                <div>
                  <p className="eyebrow">{selectedFile.language} / {selectedFile.fileType}</p>
                  <h3>{selectedFile.filePath}</h3>
                </div>
                <span>{formatBytes(selectedFile.sizeBytes)}</span>
              </div>
              <pre>{selectedFile.content || 'No text content stored for this file.'}</pre>
            </>
          ) : (
            <div className="empty-file">
              <h3>Select a file</h3>
              <p>Stored source content will appear here.</p>
            </div>
          )}
        </article>
      </div>

      <section className="glass-panel code-analysis-panel">
        <div className="code-analysis-header">
          <div>
            <p className="eyebrow">Code index</p>
            <h3>Parsed project structure</h3>
          </div>
          <div className="analysis-stats">
            <span>{symbols.length} symbols</span>
            <span>{relationships.length} links</span>
            <span>{selectedFileSymbols.length} in file</span>
          </div>
        </div>

        <div className="code-analysis-grid">
          <div className="symbol-list">
            {selectedFileSymbols.length > 0 ? (
              selectedFileSymbols.slice(0, 80).map((symbol) => (
                <div className="symbol-row" key={symbol.symbolId}>
                  <span className="badge muted">{symbol.type}</span>
                  <div>
                    <strong>{symbol.name}</strong>
                    <small>{symbol.signature || symbol.filePath}</small>
                  </div>
                  <span className="line-range">
                    {symbol.startLine ? `${symbol.startLine}-${symbol.endLine || symbol.startLine}` : 'n/a'}
                  </span>
                </div>
              ))
            ) : (
              <div className="empty-file compact-empty">
                <h3>No parsed symbols yet</h3>
                <p>Parse the project after importing a Java repository.</p>
              </div>
            )}
          </div>

          <div className="relationship-list">
            {relationships.slice(0, 40).map((relationship) => (
              <div className="relationship-row" key={relationship.relationshipId}>
                <span>{relationship.sourceName}</span>
                <strong>{relationship.relationshipType}</strong>
                <span>{relationship.targetName}</span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </section>
  )
}

function formatBytes(bytes = 0) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

export default ProjectDetailPanel
