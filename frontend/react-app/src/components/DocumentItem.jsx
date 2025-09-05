import React from 'react';

function formatFileSize(bytes) {
    const n = Number(bytes);
    if (!isFinite(n) || n < 0) return '';

    const kb = 1024;
    const mb = kb * 1024;
    const gb = mb * 1024;

    if (n < kb) {
        return `${n} B`;
    } else if (n < mb) {
        return `${(n / kb).toFixed(2)} KB`;
    } else if (n < gb) {
        return `${(n / mb).toFixed(2)} MB`;
    } else {
        return `${(n / gb).toFixed(2)} GB`;
    }
}

export default function DocumentItem({doc, onDelete}) {
    return (
        <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            padding: '8px 0',
            borderBottom: '1px solid #eee'
        }}>
            <div>
                <div style={{fontWeight: 500}}>{doc.name}</div>
                {doc.size != null && <div style={{fontSize: 12, color: '#666'}}>{formatFileSize(doc.size)}</div>}
            </div>
            <button onClick={() => onDelete?.(doc.id)} style={{color: '#b00'}}>Delete</button>
        </div>
    );
}
