import ProjectCard from '../components/ProjectCard'

function ProjectsPanel({ onCreateFirst, projects }) {
  if (!projects.length) {
    return (
      <section className="empty-state">
        <p className="eyebrow">No projects yet</p>
        <h2>Create the first codebase workspace.</h2>
        <p>Projects created in this demo are kept in local React state until the backend endpoint exists.</p>
        <button className="primary-button" onClick={onCreateFirst} type="button">New Project</button>
      </section>
    )
  }

  return (
    <section className="project-grid">
      {projects.map((project) => (
        <ProjectCard key={project.id} project={project} />
      ))}
    </section>
  )
}

export default ProjectsPanel
