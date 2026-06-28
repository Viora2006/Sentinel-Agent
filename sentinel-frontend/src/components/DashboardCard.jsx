function DashboardCard({ description, label, title }) {
  return (
    <article className="dashboard-card">
      <div className="card-topline">
        <span className="card-glyph">{label}</span>
        <span className="badge">Coming soon</span>
      </div>
      <h3>{title}</h3>
      <p>{description}</p>
    </article>
  )
}

export default DashboardCard
