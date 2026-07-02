function ProjectCard({ onImportProject, onOpenProject, project }) {
  const status = project.status || 'Not imported yet'

  return (
    <article className="project-card">
      <div className="project-card-header">
        <div>
          <h3>{project.name}</h3>
          <a href={project.repoUrl} rel="noreferrer" target="_blank">
            {project.repoUrl}
          </a>
        </div>
        <span className={project.imported ? 'badge' : 'badge muted'}>{status}</span>
      </div>
      <p>{project.description || 'No description added yet.'}</p>
      {project.imported && (
        <div className="project-meta">
          <span>{project.totalFiles} files</span>
          {Object.entries(project.languages || {}).slice(0, 4).map(([language, count]) => (
            <span key={language}>{language}: {count}</span>
          ))}
        </div>
      )}
      <div className="button-row">
        <button className="secondary-button compact" onClick={() => onOpenProject?.(project.id)} type="button">Open</button>
        <button className="secondary-button compact" onClick={() => onImportProject?.(project)} type="button">
          {project.imported ? 'Re-import' : 'Import Repo'}
        </button>
        <button className="primary-button compact" type="button">Ask Agent</button>
      </div>
    </article>
  )
}

export default ProjectCard
