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

export default function DocumentItem({doc, onDelete, canDelete = true}) {
    const processingProgress = doc?.processingProgress;

    const badgeStyle = {
        display: "flex",
        gap: "5px",
        alignItems: "center",
        marginLeft: 8,
        fontSize: 12,
        color: '#666',
        border: '1px solid #ddd',
        borderRadius: 4,
        padding: '2px 6px'
    };

    return (
      <div className="flex flex-col md:flex-row justify-between py-2 border-b border-gray-200">
            <div>
                <div style={{fontWeight: 500, display: 'flex', alignItems: 'center'}}>
                    <span>{doc.name}</span>
                    {doc?.status !== 'processed' && (
                      <span style={badgeStyle}>
                        PROCESSING
                        {processingProgress !== undefined && (<>({processingProgress}%)</>)}
                        <svg className="animate-spin h-4 w-4 text-gray-500" xmlns="http://www.w3.org/2000/svg" fill="none"
                             viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                  strokeWidth="4"></circle>
                          <path className="opacity-75" fill="currentColor"
                                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                      </span>
                    )}
                    {doc?.status === 'processed' && (
                      <span style={badgeStyle} className={"bg-[#1fff0424]"}>READY</span>
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
                {doc?.status !== 'processed' ? (
                  <span className="opacity-60 cursor-not-allowed">
                    Search by
                  </span>
                ) : (
                  <Link to={`/search/${encodeURIComponent(doc.id)}`} className={"hover:underline"}>
                    Search by
                  </Link>
                )}
                <button
                    onClick={() => canDelete && onDelete?.(doc.id)}
                    className={"hover:underline disabled:opacity-60"}
                    style={{
                        color: '#b00',
                        cursor: canDelete ? 'pointer' : 'not-allowed'
                    }}
                    disabled={!canDelete}
                >
                    Delete
                </button>
            </div>

        </div>
    );
}
