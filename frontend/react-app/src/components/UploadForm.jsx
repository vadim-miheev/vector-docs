import React, { useState } from 'react';

export default function UploadForm({ onUpload }) {
  const [file, setFile] = useState(null);
  const [error, setError] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (!file) {
      setError('Select a file');
      return;
    }
    try {
      await onUpload?.(file);
      setFile(null);
    } catch (e) {
      setError(e.message || 'Upload failed');
    }
  };

  return (
    <form onSubmit={submit} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
      <input type="file" onChange={(e) => setFile(e.target.files?.[0] || null)} />
      <button type="submit">Upload</button>
      {error && <span style={{ color: 'red' }}>{error}</span>}
    </form>
  );
}
