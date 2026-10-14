import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  ReactNode,
} from 'react';
import { api, TOKEN_KEY } from '../api/client';
import { AppConfig, AuthResponse, UserResponse } from '../types';

interface AuthContextValue {
  user: UserResponse | null;
  token: string | null;
  loading: boolean;
  aiConfigured: boolean;
  login: (email: string, password: string) => Promise<UserResponse>;
  register: (fullName: string, email: string, password: string) => Promise<UserResponse>;
  logout: () => void;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem(TOKEN_KEY)
  );
  const [loading, setLoading] = useState<boolean>(!!localStorage.getItem(TOKEN_KEY));
  const [aiConfigured, setAiConfigured] = useState(true);

  useEffect(() => {
    let active = true;
    api
      .get<AppConfig>('/config')
      .then((response) => {
        if (active) setAiConfigured(response.data.aiConfigured);
      })
      .catch(() => {
        if (active) setAiConfigured(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const persistAuth = useCallback((auth: AuthResponse) => {
    localStorage.setItem(TOKEN_KEY, auth.token);
    setToken(auth.token);
  }, []);

  useEffect(() => {
    let active = true;
    async function loadMe() {
      const stored = localStorage.getItem(TOKEN_KEY);
      if (!stored) {
        setLoading(false);
        return;
      }
      try {
        const response = await api.get<UserResponse>('/auth/me');
        if (active) {
          setUser(response.data);
        }
      } catch {
        if (active) {
          localStorage.removeItem(TOKEN_KEY);
          setToken(null);
          setUser(null);
        }
      } finally {
        if (active) setLoading(false);
      }
    }
    loadMe();
    return () => {
      active = false;
    };
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const response = await api.post<AuthResponse>('/auth/login', { email, password });
      persistAuth(response.data);
      const me = await api.get<UserResponse>('/auth/me');
      setUser(me.data);
      return me.data;
    },
    [persistAuth]
  );

  const register = useCallback(
    async (fullName: string, email: string, password: string) => {
      const response = await api.post<AuthResponse>('/auth/register', {
        fullName,
        email,
        password,
      });
      persistAuth(response.data);
      const me = await api.get<UserResponse>('/auth/me');
      setUser(me.data);
      return me.data;
    },
    [persistAuth]
  );

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        aiConfigured,
        login,
        register,
        logout,
        isAdmin: user?.role === 'ADMIN',
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}