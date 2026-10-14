import { useEffect, useState } from 'react';
import { api, getApiErrorMessage } from '../api/client';
import { AdminStatsResponse, UserResponse } from '../types';
import { Alert } from '../components/Alert';
import { Pill } from '../components/Pill';
import { StatCard } from '../components/StatCard';
import { InlineSpinner, Spinner } from '../components/Spinner';

export function AdminDashboard() {
  const [stats, setStats] = useState<AdminStatsResponse | null>(null);
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const [statsRes, usersRes] = await Promise.all([
          api.get<AdminStatsResponse>('/admin/stats'),
          api.get<UserResponse[]>('/admin/users'),
        ]);
        if (!active) return;
        setStats(statsRes.data);
        setUsers(usersRes.data);
      } catch (err) {
        if (active) setError(getApiErrorMessage(err, 'Could not load admin data'));
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, []);

  async function changeRole(userId: number, role: string) {
    setUpdatingId(userId);
    setError(null);
    try {
      await api.patch(`/admin/users/${userId}/role`, { role });
      setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, role } : u)));
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not update the role'));
    } finally {
      setUpdatingId(null);
    }
  }

  if (loading) return <Spinner label="Loading admin dashboard..." />;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Admin Dashboard</h1>
          <p className="muted">Platform usage statistics and user management.</p>
        </div>
      </div>

      {error ? <Alert kind="error" onClose={() => setError(null)}>{error}</Alert> : null}

      {stats && (
        <>
          <div className="stats-grid">
            <StatCard label="Total users" value={stats.totalUsers} />
            <StatCard label="Candidates" value={stats.candidateUsers} />
            <StatCard label="Resumes analyzed" value={stats.totalResumes} />
            <StatCard label="Interviews" value={stats.totalInterviews} />
            <StatCard label="Completed interviews" value={stats.completedInterviews} />
            <StatCard label="Questions answered" value={stats.questionsAnswered} />
            <StatCard label="Learning plans" value={stats.totalLearningPlans} />
            <StatCard label="Avg. interview score" value={`${Math.round(stats.averageInterviewScore)}%`} accent="var(--primary)" />
          </div>
          <section className="panel">
            <h2 className="panel-title">Interviews by status</h2>
            <div className="status-bars">
              {Object.entries(stats.interviewsByStatus).map(([status, count]) => (
                <div key={status} className="status-bar">
                  <span>{status}</span>
                  <strong>{count}</strong>
                </div>
              ))}
            </div>
          </section>
        </>
      )}

      <section className="panel">
        <h2 className="panel-title">Users</h2>
        {users.length === 0 ? (
          <Alert kind="info">No users found.</Alert>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.fullName}</td>
                  <td>{user.email}</td>
                  <td>
                    <span className="row-gap">
                      <Pill tone={user.role === 'ADMIN' ? 'warning' : 'info'}>{user.role}</Pill>
                      <select
                        value={user.role}
                        onChange={(e) => changeRole(user.id, e.target.value)}
                        disabled={updatingId === user.id}
                        className="select select-sm"
                      >
                        <option value="CANDIDATE">CANDIDATE</option>
                        <option value="ADMIN">ADMIN</option>
                      </select>
                      {updatingId === user.id ? <InlineSpinner size={14} /> : null}
                    </span>
                  </td>
                  <td>{new Date(user.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}