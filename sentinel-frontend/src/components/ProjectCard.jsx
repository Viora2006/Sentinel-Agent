function ProjectCard({ project }) {
  return (
    <article className="project-card">
      <div className="project-card-header">
        <div>
          <h3>{project.name}</h3>
          <a href={project.repoUrl} rel="noreferrer" target="_blank">
            {project.repoUrl}
          </a>
        </div>
        <span className="badge muted">Not imported yet</span>
      </div>
      <p>{project.description || 'No description added yet.'}</p>
      <div className="button-row">
        <button className="secondary-button compact" type="button">Open</button>
        <button className="secondary-button compact" type="button">Import Repo</button>
        <button className="primary-button compact" type="button">Ask Agent</button>
      </div>
    </article>
  )
}

export default ProjectCard
