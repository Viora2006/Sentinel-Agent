import { useState } from 'react'

const importSteps = [
  'Clone repository',
  'Scan file tree',
  'Save files to database',
  'Build code index',
  'Ready for AI retrieval',
]

function RepoImportPanel() {
  const [repoUrl, setRepoUrl] = useState('')
  const [hasStarted, setHasStarted] = useState(false)

  return (
    <section className="panel-stack">
      <div className="glass-panel import-console">
        <label>
          <span>GitHub repo URL</span>
          <input
            onChange={(event) => setRepoUrl(event.target.value)}
            placeholder="https://github.com/org/service"
            type="url"
            value={repoUrl}
          />
        </label>
        <button className="primary-button" onClick={() => setHasStarted(true)} type="button">
          Import Repository
        </button>
      </div>

      <div className="pipeline">
        {importSteps.map((step, index) => (
          <article className={hasStarted || index === 0 ? 'pipeline-step active' : 'pipeline-step'} key={step}>
            <span>{index + 1}</span>
            <h3>{step}</h3>
            <p>{index === 4 ? 'Search and agent retrieval unlock here.' : 'Future worker pipeline stage.'}</p>
          </article>
        ))}
      </div>
    </section>
  )
}

export default RepoImportPanel
