import React, { useState, useRef } from 'react';

export default function UploadForm({ onUpload }) {
  const [file, setFile] = useState(null);
  const [error, setError] = useState('');
  const inputRef = useRef(null);

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
      />
      <button className={"border border-black rounded-sm py-[2px] px-3 bg-gray-100"} type="submit">Upload</button>
      {error && <span className={"text-red-600"}>{error}</span>}
    </form>
  );
}
