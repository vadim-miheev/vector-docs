import React from 'react';
import DownloadLink from "./DownloadLink";
import { Link } from 'react-router-dom';

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
    const processingProgress = doc?.processingProgress;

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
                    {doc?.status !== 'processed' && (
                      <span style={badgeStyle}>
                        PROCESSING
                        {processingProgress !== undefined && (<>({processingProgress}%)</>)}
                      </span>
                    )}
                </div>
                {doc.size != null && <div style={{fontSize: 12, color: '#666'}}>{formatFileSize(doc.size)}</div>}
            </div>
            <div className={"flex gap-4 items-center"} >
                <DownloadLink fileId={doc.id} fileName={doc.name} open={true} classes={"hover:underline"}>
                    Open
                </DownloadLink>
                <DownloadLink fileId={doc.id} fileName={doc.name} classes={"hover:underline"}>
                    Download
                </DownloadLink>
                <Link to={`/search/${encodeURIComponent(doc.id)}`} className={"hover:underline"}>
                    Search by
                </Link>
                <button
                    onClick={() => onDelete?.(doc.id)}
                    className={"hover:underline"}
                    style={{
                        color: '#b00',
                        cursor: 'pointer'
                    }}
                >
                    Delete
                </button>
            </div>

        </div>
    );
}
