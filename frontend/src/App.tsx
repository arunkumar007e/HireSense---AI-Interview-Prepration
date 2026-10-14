import { Routes, Route } from 'react-router-dom';
import { Layout } from './components/Layout';
import { AdminRoute, GuestRoute, ProtectedRoute } from './components/Routes';
import { Landing } from './pages/Landing';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Dashboard } from './pages/Dashboard';
import { Profile } from './pages/Profile';
import { ResumeUpload } from './pages/ResumeUpload';
import { JobRoles } from './pages/JobRoles';
import { Interviews } from './pages/Interviews';
import { InterviewSetup } from './pages/InterviewSetup';
import { MockInterview } from './pages/MockInterview';
import { InterviewResult } from './pages/InterviewResult';
import { LearningPlans } from './pages/LearningPlans';
import { ProgressPage } from './pages/ProgressPage';
import { AdminDashboard } from './pages/AdminDashboard';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route
        path="/login"
        element={
          <GuestRoute>
            <Login />
          </GuestRoute>
        }
      />
      <Route
        path="/register"
        element={
          <GuestRoute>
            <Register />
          </GuestRoute>
        }
      />

      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/resumes" element={<ResumeUpload />} />
        <Route path="/job-roles" element={<JobRoles />} />
        <Route path="/interviews" element={<Interviews />} />
        <Route path="/interviews/setup" element={<InterviewSetup />} />
        <Route path="/interviews/:sessionId" element={<MockInterview />} />
        <Route path="/interviews/:sessionId/result" element={<InterviewResult />} />
        <Route path="/learning-plans" element={<LearningPlans />} />
        <Route path="/progress" element={<ProgressPage />} />
        <Route
          path="/admin"
          element={
            <AdminRoute>
              <AdminDashboard />
            </AdminRoute>
          }
        />
      </Route>

      <Route path="*" element={<Landing />} />
    </Routes>
  );
}