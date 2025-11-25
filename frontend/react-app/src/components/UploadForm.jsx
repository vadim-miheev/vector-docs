import React, { useState, useRef } from 'react';

export default function UploadForm({ onUpload, uploading, disabled = false }) {
  const [file, setFile] = useState(null);
  const [error, setError] = useState('');
  const inputRef = useRef(null);

  const submit = async (e) => {
    e.preventDefault();
    if (uploading || disabled) return;
    setError('');
    if (!file) {
      setError('Select a file');
      return;
    }
    try {
      await onUpload?.(file);
      setFile(null);
      if (inputRef.current) inputRef.current.value = '';
    } catch (e) {
      setError(e.message || 'Upload failed');
    }
  };

  return (
    <form onSubmit={submit} className={"flex gap-4 items-center"} >
      <input
        ref={inputRef}
        type="file"
        onChange={(e) => setFile(e.target.files?.[0] || null)}
        disabled={disabled}
      />
      <div className={"flex items-center"}>
        <button
          className={"border border-black rounded-sm py-[2px] px-3 bg-gray-100 disabled:opacity-60"}
          type="submit"
          disabled={uploading || disabled}
        >
          Upload
        </button>
        {uploading && (
          <span
            className={"ml-2 inline-block h-4 w-4 border-2 border-gray-300 border-t-black rounded-full animate-spin"}
            aria-label="Uploading"
            role="status"
          />
        )}
      </div>
      {error && <span className={"text-red-600"}>{error}</span>}
    </form>
  );
}
