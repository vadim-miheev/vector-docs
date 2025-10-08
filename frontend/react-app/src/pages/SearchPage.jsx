import React, { useState } from 'react';
import SearchBar from '../components/SearchBar';
import { searchService } from '../services/searchService';
import { useAuth } from '../hooks/useAuth';

export default function SearchPage() {
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { logout } = useAuth();

  async function onSearch(query) {
    setLoading(true);
    setError('');
    try {
      const res = await searchService.search(query);
      setResults(res);
    } catch (e) {
      if (e.message === 'Unauthorized') {
        logout()
      }
      setError(e.message || 'Search failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ padding: 16 }}>
      <h2>Search</h2>
      <SearchBar onSearch={onSearch} />
      {loading && <div>Searching...</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
      <div style={{ marginTop: 16 }}>
        {!results.length ? (
          <div>No results</div>
        ) : (
          <ul>
            {results.map((r, idx) => (
              <li key={r.id ?? idx}>{r.name || JSON.stringify(r)}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
