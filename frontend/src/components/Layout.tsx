import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const links = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/resumes', label: 'Resumes' },
  { to: '/job-roles', label: 'Job Roles' },
  { to: '/interviews', label: 'Interviews' },
  { to: '/learning-plans', label: 'Learning' },
  { to: '/progress', label: 'Progress' },
];

export function Navbar() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/dashboard" className="brand">
          <span className="brand-mark">HS</span>
          <span className="brand-name">HireSense</span>
        </Link>
        <div className="nav-links">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
            >
              {link.label}
            </NavLink>
          ))}
          {isAdmin ? (
            <NavLink
              to="/admin"
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
            >
              Admin
            </NavLink>
          ) : null}
        </div>
        <div className="nav-user">
          <Link to="/profile" className="nav-profile">
            <span className="avatar">{user?.fullName?.charAt(0).toUpperCase() ?? 'U'}</span>
            <span className="nav-user-name">{user?.fullName}</span>
            <span className="role-badge">{user?.role}</span>
          </Link>
          <button type="button" className="btn btn-ghost btn-sm" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </div>
    </nav>
  );
}

export function Layout() {
  const { aiConfigured } = useAuth();
  return (
    <div className="app-shell">
      <Navbar />
      <main className="content">
        {aiConfigured ? null : (
          <div className="dev-notice">
            Development mode: AI features use a built-in mock provider. Set an AI provider
            key to enable live analysis.
          </div>
        )}
        <Outlet />
      </main>
      <footer className="footer">
        HireSense &middot; Resume analysis, mock interviews, and learning plans
      </footer>
    </div>
  );
}