import { useState } from 'react'

const initialForm = {
  name: '',
  repoUrl: '',
  description: '',
}

function NewProjectPanel({ onCreateProject }) {
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState('')

  function updateField(event) {
    const { name, value } = event.target
    setForm((currentForm) => ({ ...currentForm, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    setError('')

    if (!form.name.trim() || !form.repoUrl.trim()) {
      setError('Project name and GitHub repo URL are required.')
      return
    }

    onCreateProject({
      name: form.name.trim(),
      repoUrl: form.repoUrl.trim(),
      description: form.description.trim(),
    })
    setForm(initialForm)
  }

  return (
    <section className="form-layout">
      <form className="glass-panel project-form" onSubmit={handleSubmit}>
        <label>
          <span>Project name</span>
          <input name="name" onChange={updateField} placeholder="sentinel-core" type="text" value={form.name} />
        </label>
        <label>
          <span>GitHub repo URL</span>
          <input
            name="repoUrl"
            onChange={updateField}
            placeholder="https://github.com/org/repository"
            type="url"
            value={form.repoUrl}
          />
        </label>
        <label>
          <span>Description</span>
          <textarea
            name="description"
            onChange={updateField}
            placeholder="What should the agent understand about this codebase?"
            rows="5"
            value={form.description}
          />
        </label>
        <button className="primary-button" type="submit">Create Project</button>
        <p className="status-line">{error}</p>
      </form>

      <aside className="architecture-note">
        <p className="eyebrow">Import-backed storage</p>
        <h2>Projects are saved after import.</h2>
        <p>
          This form prepares the project details, then the import step clones the repo and stores the project
          under your signed-in account.
        </p>
        <code>{'{ projectName, githubUrl, description }'}</code>
      </aside>
    </section>
  )
}

export default NewProjectPanel
