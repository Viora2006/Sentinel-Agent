import { useCallback, useEffect, useMemo, useState } from 'react'
import './styles.css'
import AuthPage from './components/AuthPage'
import Header from './components/Header'
import Sidebar from './components/Sidebar'
import AgentTasksPanel from './panels/AgentTasksPanel'
import CodeSearchPanel from './panels/CodeSearchPanel'
import DashboardPanel from './panels/DashboardPanel'
import NewProjectPanel from './panels/NewProjectPanel'
import ProjectDetailPanel from './panels/ProjectDetailPanel'
import ProjectsPanel from './panels/ProjectsPanel'
import RepoImportPanel from './panels/RepoImportPanel'
import SettingsPanel from './panels/SettingsPanel'
import WorkersPanel from './panels/WorkersPanel'
import { API_BASE_URL } from './api'

const panels = {
  dashboard: {
    title: 'Dashboard',
    eyebrow: 'Platform command center',
  },
  newProject: {
    title: 'New Project',
    eyebrow: 'Local project setup',
  },
  projects: {
    title: 'Projects',
    eyebrow: 'Codebases in flight',
  },
  projectDetail: {
    title: 'Project',
    eyebrow: 'Stored files and source',
  },
  repoImport: {
    title: 'Repo Import',
    eyebrow: 'Future ingestion pipeline',
  },
  agentTasks: {
    title: 'Agent Tasks',
    eyebrow: 'Planning and execution queue',
  },
  codeSearch: {
    title: 'Code Search',
    eyebrow: 'Semantic retrieval preview',
  },
  workers: {
    title: 'Workers',
    eyebrow: 'Queue and execution health',
  },
  settings: {
    title: 'Settings',
    eyebrow: 'Workspace preferences',
  },
}

function App() {
  const [isCheckingSession, setIsCheckingSession] = useState(true)
  const [username, setUsername] = useState('')
  const [activePanel, setActivePanel] = useState('dashboard')
  const [projects, setProjects] = useState([])
  const [projectsStatus, setProjectsStatus] = useState('')
  const [repoImportDraft, setRepoImportDraft] = useState({ repoUrl: '', projectName: '', description: '' })
  const [selectedProjectId, setSelectedProjectId] = useState(null)

  const currentPanel = useMemo(() => panels[activePanel] || panels.dashboard, [activePanel])

  const clearPrivateState = useCallback(() => {
    setProjects([])
    setProjectsStatus('')
    setRepoImportDraft({ repoUrl: '', projectName: '', description: '' })
    setSelectedProjectId(null)
  }, [])

  const clearAuthenticatedSession = useCallback(() => {
    setUsername('')
    clearPrivateState()
    setActivePanel('dashboard')
  }, [clearPrivateState])

  useEffect(() => {
    let isCurrent = true

    async function checkSession() {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/me`, {
          credentials: 'include',
        })
        const data = await response.json().catch(() => ({}))

        if (isCurrent && response.ok) {
          setUsername(data.username || '')
        }
      } catch {
        if (isCurrent) {
          clearPrivateState()
        }
      } finally {
        if (isCurrent) {
          setIsCheckingSession(false)
        }
      }
    }

    checkSession()

    return () => {
      isCurrent = false
    }
  }, [clearPrivateState])

  useEffect(() => {
    if (!username) {
      clearPrivateState()
      return
    }

    let isCurrent = true

    async function loadProjects() {
      setProjectsStatus('Loading projects...')

      try {
        const response = await fetch(`${API_BASE_URL}/projects`, {
          credentials: 'include',
        })
        const data = await response.json().catch(() => [])

        if (response.status === 401 || response.status === 403) {
          clearAuthenticatedSession()
          throw new Error('Session expired. Please sign in again.')
        }

        if (!response.ok) {
          throw new Error(data.message || 'Unable to load projects.')
        }

        if (isCurrent) {
          setProjects(data.map(toProjectCard))
          setProjectsStatus('')
        }
      } catch (error) {
        if (isCurrent) {
          setProjects([])
          setProjectsStatus(error.message || 'Unable to load projects.')
        }
      }
    }

    loadProjects()

    return () => {
      isCurrent = false
    }
  }, [clearAuthenticatedSession, clearPrivateState, username])

  function handleAuthenticated(nextUsername) {
    setUsername(nextUsername)
  }

  async function handleLogout() {
    try {
      await fetch(`${API_BASE_URL}/auth/logout`, {
        method: 'POST',
        credentials: 'include',
      })
    } catch {
      // Local logout should still work if the backend is offline during frontend-only demos.
    }

    clearAuthenticatedSession()
  }

  function handleCreateProject(project) {
    setRepoImportDraft({
      repoUrl: project.repoUrl,
      projectName: project.name,
      description: project.description,
    })
    setActivePanel('repoImport')
  }

  function handleImportComplete(importedRepository) {
    setProjects((currentProjects) => {
      const nextProject = toProjectCard(importedRepository)

      const existingProjectIndex = currentProjects.findIndex((project) => project.repoUrl === nextProject.repoUrl)
      if (existingProjectIndex >= 0) {
        return currentProjects.map((project, index) => (index === existingProjectIndex ? { ...project, ...nextProject } : project))
      }

      return [nextProject, ...currentProjects]
    })
  }

  function handleImportRequest(project) {
    setRepoImportDraft({
      repoUrl: project?.repoUrl || '',
      projectName: project?.name || '',
      description: project?.description || '',
    })
    setActivePanel('repoImport')
  }

  function handleOpenProject(projectId) {
    setSelectedProjectId(projectId)
    setActivePanel('projectDetail')
  }

  function renderPanel() {
    switch (activePanel) {
      case 'newProject':
        return <NewProjectPanel onCreateProject={handleCreateProject} />
      case 'projects':
        return (
          <ProjectsPanel
            projects={projects}
            status={projectsStatus}
            onCreateFirst={() => setActivePanel('newProject')}
            onImportProject={handleImportRequest}
            onOpenProject={handleOpenProject}
          />
        )
      case 'projectDetail':
        return (
          <ProjectDetailPanel
            onBack={() => setActivePanel('projects')}
            onSessionExpired={clearAuthenticatedSession}
            projectId={selectedProjectId}
          />
        )
      case 'repoImport':
        return (
          <RepoImportPanel
            initialDescription={repoImportDraft.description}
            initialProjectName={repoImportDraft.projectName}
            initialRepoUrl={repoImportDraft.repoUrl}
            onImportComplete={handleImportComplete}
            onSessionExpired={clearAuthenticatedSession}
          />
        )
      case 'agentTasks':
        return <AgentTasksPanel />
      case 'codeSearch':
        return <CodeSearchPanel />
      case 'workers':
        return <WorkersPanel />
      case 'settings':
        return <SettingsPanel username={username} />
      case 'dashboard':
      default:
        return <DashboardPanel />
    }
  }

  if (isCheckingSession) {
    return (
      <main className="auth-page">
        <section className="empty-state">
          <p className="eyebrow">Checking session</p>
          <h2>Opening your workspace...</h2>
        </section>
      </main>
    )
  }

  if (!username) {
    return <AuthPage onAuthenticated={handleAuthenticated} />
  }

  return (
    <div className="app-shell">
      <Sidebar activePanel={activePanel} onSelectPanel={setActivePanel} />
      <div className="workspace">
        <Header
          eyebrow={currentPanel.eyebrow}
          title={currentPanel.title}
          username={username}
          onLogout={handleLogout}
        />
        <main className="main-panel">{renderPanel()}</main>
      </div>
    </div>
  )
}

function toProjectCard(project) {
  return {
    id: project.projectId,
    name: project.projectName,
    repoName: project.repoName,
    repoUrl: project.githubUrl,
    description: project.description || `Imported repository with ${project.totalFiles} tracked files.`,
    status: project.status === 'IMPORTED' ? 'Imported' : project.status,
    totalFiles: project.totalFiles,
    languages: project.languages || {},
    imported: project.status === 'IMPORTED',
  }
}

export default App
