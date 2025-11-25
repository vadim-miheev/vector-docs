import React from 'react';
import UploadForm from '../components/UploadForm';
import DocumentList from '../components/DocumentList';
import { useDocuments } from '../hooks/useDocuments';
import { useAuthContext } from '../store/AuthContext';

export default function DocumentsPage() {
  const { docs, loading, error, upload, remove, uploading } = useDocuments();
  const { user } = useAuthContext();
  const demoIdStr = process.env.REACT_APP_DEMO_USER_ID || '0';
  const demoId = parseInt(demoIdStr, 10);
  const isDemoUser = Number.isFinite(demoId) && demoId > 0 && String(user?.id || '') === String(demoId);
  const [showDemoWarning, setShowDemoWarning] = React.useState(true);

  return (
    <div style={{ padding: 16 }}>
      {isDemoUser && showDemoWarning && (
        <div className={"mb-3 py-2 px-3 border border-yellow-500 bg-yellow-50 text-yellow-800 rounded flex justify-between items-start gap-2"}>
          <span>
            A demo user is prohibited from uploading new or deleting existing documents. Please register to access all features.
          </span>
          <button
            type="button"
            onClick={() => setShowDemoWarning(false)}
            className="ml-2 text-yellow-800 hover:text-yellow-900 font-bold"
            aria-label="Close"
          >
            ×
          </button>
        </div>
      )}
      <h3 className={"text-1xl font-bold mb-4"}>Upload</h3>
      <UploadForm onUpload={upload} uploading={uploading} disabled={isDemoUser} />
      <h3 className={"text-2xl font-bold mb-4 mt-12"}>My Documents</h3>
      {loading && <div>Loading...</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
      <div style={{ marginTop: 16 }}>
        <DocumentList items={docs} onDelete={remove} canDelete={!isDemoUser} />
      </div>
    </div>
  );
}
