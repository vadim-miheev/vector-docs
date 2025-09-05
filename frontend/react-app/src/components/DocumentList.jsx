import React from 'react';
import DocumentItem from './DocumentItem';

export default function DocumentList({ items = [], onDelete }) {
  if (!items.length) return <div>No documents</div>;
  return (
    <div>
      {items.map((doc) => (
        <DocumentItem key={doc.id} doc={doc} onDelete={onDelete} />
      ))}
    </div>
  );
}
