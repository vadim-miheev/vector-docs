import React from 'react';
import DownloadLink from "./DownloadLink";

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
    const isProcessing = doc?.processed === false;
    const deleteDisabled = isProcessing;
    const badgeStyle = {
        marginLeft: 8,
        fontSize: 12,
        color: '#666',
        border: '1px solid #ddd',
        borderRadius: 4,
        padding: '2px 6px',
        backgroundColor: '#f8f8f8'
    };

    return (
        <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            padding: '8px 0',
            borderBottom: '1px solid #eee'
        }}>
            <div>
                <div style={{fontWeight: 500, display: 'flex', alignItems: 'center'}}>
                    <span>{doc.name}</span>
                    {isProcessing && <span style={badgeStyle}>in progress</span>}
                </div>
                {doc.size != null && <div style={{fontSize: 12, color: '#666'}}>{formatFileSize(doc.size)}</div>}
            </div>
            <div style={{display: 'flex', gap: 8}}>
                <DownloadLink fileId={doc.id} fileName={doc.name} open={true}>
                    Open
                </DownloadLink>
                <DownloadLink fileId={doc.id} fileName={doc.name}>
                    Download
                </DownloadLink>
                <button
                    onClick={() => onDelete?.(doc.id)}
                    disabled={deleteDisabled}
                    style={{
                        color: deleteDisabled ? '#aaa' : '#b00',
                        cursor: deleteDisabled ? 'not-allowed' : 'pointer'
                    }}
                >
                    Delete
                </button>
            </div>

        </div>
    );
}
