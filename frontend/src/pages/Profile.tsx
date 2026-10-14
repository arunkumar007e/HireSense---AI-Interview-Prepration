import { FormEvent, useEffect, useState } from 'react';
import { api, getApiErrorMessage } from '../api/client';
import { UserProfile } from '../types';
import { Alert } from '../components/Alert';
import { InlineSpinner, Spinner } from '../components/Spinner';

const emptyProfile = {
  headline: '',
  summary: '',
  experienceYears: 0,
  education: '',
  location: '',
  phone: '',
  githubUrl: '',
  linkedinUrl: '',
  portfolioUrl: '',
};

export function Profile() {
  const [form, setForm] = useState<typeof emptyProfile>(emptyProfile);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    let active = true;
    api
      .get<UserProfile>('/users/me/profile')
      .then((response) => {
        if (!active) return;
        const profile = response.data;
        setForm({
          headline: profile?.headline ?? '',
          summary: profile?.summary ?? '',
          experienceYears: profile?.experienceYears ?? 0,
          education: profile?.education ?? '',
          location: profile?.location ?? '',
          phone: profile?.phone ?? '',
          githubUrl: profile?.githubUrl ?? '',
          linkedinUrl: profile?.linkedinUrl ?? '',
          portfolioUrl: profile?.portfolioUrl ?? '',
        });
      })
      .catch((err) => {
        if (active) setError(getApiErrorMessage(err, 'Could not load your profile'));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  function update<K extends keyof typeof emptyProfile>(key: K, value: string) {
    setForm((prev) => ({
      ...prev,
      [key]: key === 'experienceYears' ? (value === '' ? 0 : Number(value)) : value,
    }));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setSaved(false);
    setError(null);
    try {
      await api.put<UserProfile>('/users/me/profile', form);
      setSaved(true);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not save your profile'));
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <Spinner label="Loading your profile..." />;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>My Profile</h1>
          <p className="muted">Keep your professional profile up to date. This powers resume and interview features.</p>
        </div>
      </div>

      {error ? <Alert kind="error" onClose={() => setError(null)}>{error}</Alert> : null}
      {saved ? <Alert kind="success" onClose={() => setSaved(false)}>Profile saved successfully.</Alert> : null}

      <form onSubmit={handleSubmit} className="panel form-grid">
        <label className="field span-2">
          <span>Headline</span>
          <input
            value={form.headline}
            onChange={(e) => update('headline', e.target.value)}
            placeholder="e.g. Senior Backend Developer"
            maxLength={200}
          />
        </label>
        <label className="field span-2">
          <span>Summary</span>
          <textarea
            value={form.summary}
            onChange={(e) => update('summary', e.target.value)}
            placeholder="Write a brief professional summary"
            rows={4}
            maxLength={5000}
          />
        </label>
        <label className="field">
          <span>Experience (years)</span>
          <input
            type="number"
            min={0}
            max={60}
            value={form.experienceYears}
            onChange={(e) => update('experienceYears', e.target.value)}
          />
        </label>
        <label className="field">
          <span>Education</span>
          <input
            value={form.education}
            onChange={(e) => update('education', e.target.value)}
            placeholder="e.g. B.E. Computer Science"
            maxLength={200}
          />
        </label>
        <label className="field">
          <span>Location</span>
          <input
            value={form.location}
            onChange={(e) => update('location', e.target.value)}
            placeholder="e.g. Bengaluru, India"
            maxLength={200}
          />
        </label>
        <label className="field">
          <span>Phone</span>
          <input
            value={form.phone}
            onChange={(e) => update('phone', e.target.value)}
            placeholder="Your contact number"
            maxLength={100}
          />
        </label>
        <label className="field">
          <span>GitHub</span>
          <input
            value={form.githubUrl}
            onChange={(e) => update('githubUrl', e.target.value)}
            placeholder="https://github.com/your-handle"
            maxLength={500}
          />
        </label>
        <label className="field">
          <span>LinkedIn</span>
          <input
            value={form.linkedinUrl}
            onChange={(e) => update('linkedinUrl', e.target.value)}
            placeholder="https://linkedin.com/in/your-handle"
            maxLength={500}
          />
        </label>
        <label className="field">
          <span>Portfolio</span>
          <input
            value={form.portfolioUrl}
            onChange={(e) => update('portfolioUrl', e.target.value)}
            placeholder="https://your-domain.dev"
            maxLength={500}
          />
        </label>
        <div className="span-2 form-actions">
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? <InlineSpinner /> : 'Save profile'}
          </button>
        </div>
      </form>
    </div>
  );
}