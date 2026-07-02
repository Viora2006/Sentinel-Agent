import ProjectCard from '../components/ProjectCard'

function ProjectsPanel({ onCreateFirst, onImportProject, onOpenProject, projects, status }) {
  if (!projects.length) {
    return (
      <section className="empty-state">
        <p className="eyebrow">No projects yet</p>
        <h2>Create the first codebase workspace.</h2>
        <p>{status || 'Import a GitHub repository to store it as a project tied to your account.'}</p>
        <button className="primary-button" onClick={onCreateFirst} type="button">New Project</button>
      </section>
    )
  }

  return (
    <section className="project-grid">
      {projects.map((project) => (
        <ProjectCard
          key={project.id}
          onImportProject={onImportProject}
          onOpenProject={onOpenProject}
          project={project}
        />
      ))}
    </section>
  )
}

export default ProjectsPanel
