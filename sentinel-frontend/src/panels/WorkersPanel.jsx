const workers = [
  { name: 'Worker 1', status: 'idle' },
  { name: 'Worker 2', status: 'running' },
  { name: 'Worker 3', status: 'offline' },
]

const stats = [
  ['Queue depth', '24'],
  ['Jobs completed', '1,284'],
  ['Failed jobs', '7'],
]

function WorkersPanel() {
  return (
    <section className="panel-stack">
      <div className="stats-grid">
        {stats.map(([label, value]) => (
          <article className="stat-card" key={label}>
            <span>{label}</span>
            <strong>{value}</strong>
          </article>
        ))}
      </div>

      <div className="worker-grid">
        {workers.map((worker) => (
          <article className="worker-card" key={worker.name}>
            <div className={`worker-light ${worker.status}`} />
            <div>
              <h3>{worker.name}</h3>
              <p>{worker.status}</p>
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}

export default WorkersPanel
