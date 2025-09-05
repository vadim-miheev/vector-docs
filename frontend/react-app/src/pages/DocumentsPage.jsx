import React from 'react';
import UploadForm from '../components/UploadForm';
import DocumentList from '../components/DocumentList';
import { useDocuments } from '../hooks/useDocuments';

export default function DocumentsPage() {
  const { docs, loading, error, upload, remove } = useDocuments();

  return (
    <div style={{ padding: 16 }}>
      <h2>My documents</h2>
      <UploadForm onUpload={upload} />
      {loading && <div>Loading...</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
      <div style={{ marginTop: 16 }}>
        <DocumentList items={docs} onDelete={remove} />
      </div>
    </div>
  );
}
