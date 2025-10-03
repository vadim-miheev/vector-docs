import { useCallback, useEffect, useState } from 'react';
import { documentsService } from '../services/documentsService';
import { useAuthContext } from '../store/AuthContext';
import { useAuth } from './useAuth';

export function useDocuments() {
  const { user } = useAuthContext();
  const userId = user?.id || 'anonymous';
  const [docs, setDocs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { logout } = useAuth();

  const refresh = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    setError('');
    try {
      const list = await documentsService.list();
      setDocs(list);
    } catch (e) {
      if (e.message === 'Unauthorized') {
          logout()
      }
      setError(e.message || 'Failed to load list');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const upload = useCallback(async (file) => {
    const created = await documentsService.upload(file);
    setDocs((prev) => [created, ...prev]);
    return created;
  }, [userId]);

  const remove = useCallback(async (id) => {
    await documentsService.remove(id);
    setDocs((prev) => prev.filter((d) => d.id !== id));
  }, [userId]);

  return { docs, loading, error, refresh, upload, remove };
}
