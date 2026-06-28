import { useMemo, useState } from 'react'
import './styles.css'
import AuthPage from './components/AuthPage'
import Header from './components/Header'
import Sidebar from './components/Sidebar'
import AgentTasksPanel from './panels/AgentTasksPanel'
import CodeSearchPanel from './panels/CodeSearchPanel'
import DashboardPanel from './panels/DashboardPanel'
import NewProjectPanel from './panels/NewProjectPanel'
import ProjectsPanel from './panels/ProjectsPanel'
import RepoImportPanel from './panels/RepoImportPanel'
import SettingsPanel from './panels/SettingsPanel'
import WorkersPanel from './panels/WorkersPanel'

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
  const [token, setToken] = useState(() => localStorage.getItem('ai-code-agent-token'))
  const [username, setUsername] = useState(() => localStorage.getItem('ai-code-agent-user') || '')
  const [activePanel, setActivePanel] = useState('dashboard')
  const [projects, setProjects] = useState([])

  const currentPanel = useMemo(() => panels[activePanel] || panels.dashboard, [activePanel])

  function handleAuthenticated(nextToken, nextUsername) {
    localStorage.setItem('ai-code-agent-token', nextToken)
    localStorage.setItem('ai-code-agent-user', nextUsername)
    setToken(nextToken)
    setUsername(nextUsername)
  }

  async function handleLogout() {
    if (token) {
      try {
        await fetch('http://localhost:8080/api/auth/logout', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        })
      } catch {
        // Local logout should still work if the backend is offline during frontend-only demos.
      }
    }

    localStorage.removeItem('ai-code-agent-token')
    localStorage.removeItem('ai-code-agent-user')
    setToken('')
    setUsername('')
    setActivePanel('dashboard')
  }

  function handleCreateProject(project) {
    setProjects((currentProjects) => [
      {
        ...project,
        id: crypto.randomUUID(),
        createdAt: new Date().toISOString(),
      },
      ...currentProjects,
    ])
    setActivePanel('projects')
  }

  function renderPanel() {
    switch (activePanel) {
      case 'newProject':
        return <NewProjectPanel onCreateProject={handleCreateProject} />
      case 'projects':
        return <ProjectsPanel projects={projects} onCreateFirst={() => setActivePanel('newProject')} />
      case 'repoImport':
        return <RepoImportPanel />
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

  if (!token) {
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

export default App
