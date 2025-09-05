import React, { useState } from 'react';

export default function SearchBar({ onSearch }) {
  const [q, setQ] = useState('');

  const submit = (e) => {
    e.preventDefault();
    onSearch?.(q);
  };

  return (
    <form onSubmit={submit} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Enter query"
        style={{ minWidth: 240 }}
      />
      <button type="submit">Search</button>
    </form>
  );
}
