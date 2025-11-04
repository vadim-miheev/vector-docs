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
  const [uploading, setUploading] = useState(false);
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

  // Listen for the global update event (emitted by WebSocket handlers)
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const statusUpdateHandler = (e) => {
        setDocs((prev) => prev.map(d => {
            if (d?.id === e?.detail?.id && e?.detail?.status) {
                d.status = e?.detail?.status;
            }
            return d;
        }));
    };

    const processingHandler = (e) => {
        setDocs((prev) => prev.map(d => {
            if (d?.id === e?.detail?.id) {
                d.processingProgress = e?.detail?.progressPercentage;
            }
            return d;
        }));
    };

    const processingErrorHandler = (e) => {
      setDocs((prev) => prev.filter(d => d.id !== e?.detail?.id));
    };

    window.addEventListener('documents:uploaded', statusUpdateHandler);
    window.addEventListener('documents:processed', statusUpdateHandler);
    window.addEventListener('documents:processing', processingHandler);
    window.addEventListener('documents:processing:error', processingErrorHandler);
    return () => {
      window.removeEventListener('documents:uploaded', statusUpdateHandler);
      window.removeEventListener('documents:processed', statusUpdateHandler);
      window.removeEventListener('documents:processing', processingHandler);
      window.removeEventListener('documents:processing:error', processingErrorHandler);
    };
  }, [setDocs]);

  const upload = useCallback(async (file) => {
    setUploading(true);
    try {
      const created = await documentsService.upload(file);
      setDocs((prev) => [created, ...prev]);
      return created;
    } finally {
      setUploading(false);
    }
  }, [userId]);

  const remove = useCallback(async (id) => {
    await documentsService.remove(id);
    setDocs((prev) => prev.filter((d) => d.id !== id));
  }, [userId]);

  return { docs, loading, error, uploading, refresh, upload, remove };
}
