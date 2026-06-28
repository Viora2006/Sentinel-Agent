import DashboardCard from '../components/DashboardCard'

const capabilities = [
  {
    title: 'Repository Import',
    label: 'GI',
    description: 'Connect a GitHub repository, clone it, and prepare the source tree for indexing.',
  },
  {
    title: 'Codebase Indexing',
    label: 'IX',
    description: 'Parse files, persist metadata, and build navigable structure for large codebases.',
  },
  {
    title: 'AI Planning Agent',
    label: 'AI',
    description: 'Turn user goals into reviewed implementation plans before any code is changed.',
  },
  {
    title: 'Docker Test Execution',
    label: 'DK',
    description: 'Run tests in isolated containers so generated changes can be validated safely.',
  },
  {
    title: 'Worker Queue',
    label: 'WQ',
    description: 'Coordinate background indexing, analysis, and execution jobs through queue workers.',
  },
  {
    title: 'Semantic Code Search',
    label: 'SE',
    description: 'Use embeddings and code-aware parsing to retrieve relevant files and methods.',
  },
  {
    title: 'GitHub PR Automation',
    label: 'PR',
    description: 'Package accepted agent work into branches and pull requests when ready.',
  },
  {
    title: 'Observability',
    label: 'OB',
    description: 'Track queue health, worker throughput, failures, and system-level performance.',
  },
]

function DashboardPanel() {
  return (
    <section className="panel-stack">
      <div className="hero-band">
        <div>
          <p className="eyebrow">Long-term buildout</p>
          <h2>Infrastructure for agents that understand real repositories.</h2>
          <p>
            This console is the frontend foundation for repo import, code intelligence, queued workers,
            isolated execution, and pull-request automation.
          </p>
        </div>
        <div className="system-metrics" aria-label="Mock platform metrics">
          <span>Queue depth</span>
          <strong>18</strong>
          <span>Indexed files</span>
          <strong>42.7k</strong>
          <span>Worker health</span>
          <strong>92%</strong>
        </div>
      </div>

      <div className="card-grid">
        {capabilities.map((capability) => (
          <DashboardCard key={capability.title} {...capability} />
        ))}
      </div>
    </section>
  )
}

export default DashboardPanel
