import React from 'react';
import UploadForm from '../components/UploadForm';
import DocumentList from '../components/DocumentList';
import { useDocuments } from '../hooks/useDocuments';

export default function DocumentsPage() {
  const { docs, loading, error, upload, remove, uploading } = useDocuments();

  return (
    <div style={{ padding: 16 }}>
      <h3 className={"text-1xl font-bold mb-4"}>Upload</h3>
      <UploadForm onUpload={upload} uploading={uploading} />
      <h3 className={"text-2xl font-bold mb-4 mt-12"}>My documents</h3>
      {loading && <div>Loading...</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
      <div style={{ marginTop: 16 }}>
        <DocumentList items={docs} onDelete={remove} />
      </div>
    </div>
  );
}
