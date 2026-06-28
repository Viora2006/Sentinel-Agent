import { useState } from 'react'

const initialTasks = [
  { title: 'Map authentication flow and identify missing tests', status: 'Planning' },
  { title: 'Validate generated service changes in Docker', status: 'Running tests' },
  { title: 'Review suggested JWT refresh-token implementation', status: 'Needs review' },
  { title: 'Summarize repository architecture for onboarding', status: 'Completed' },
]

function AgentTasksPanel() {
  const [taskText, setTaskText] = useState('')
  const [tasks, setTasks] = useState(initialTasks)

  function createTask() {
    if (!taskText.trim()) return
    setTasks((currentTasks) => [{ title: taskText.trim(), status: 'Planning' }, ...currentTasks])
    setTaskText('')
  }

  return (
    <section className="panel-stack">
      <div className="task-composer">
        <input
          onChange={(event) => setTaskText(event.target.value)}
          placeholder="Ask the agent to do something..."
          type="text"
          value={taskText}
        />
        <button className="primary-button" onClick={createTask} type="button">Create Task</button>
      </div>

      <div className="task-list">
        {tasks.map((task) => (
          <article className="task-item" key={`${task.title}-${task.status}`}>
            <div>
              <h3>{task.title}</h3>
              <p>Eventually backed by Redis, RabbitMQ or Kafka workers.</p>
            </div>
            <span className={`status-badge ${task.status.toLowerCase().replaceAll(' ', '-')}`}>{task.status}</span>
          </article>
        ))}
      </div>
    </section>
  )
}

export default AgentTasksPanel
