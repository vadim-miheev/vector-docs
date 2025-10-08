import React from 'react';
import {getTokenFromCookie} from "../services/authService";
import {ENDPOINTS} from "../config/endpoints";

export default function DownloadLink ({ fileId, fileName, children, open = false }) {
    const handleDownload = async () => {
        const token = getTokenFromCookie()
        const response = await fetch( ENDPOINTS.documents + `/${fileId}/download`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            console.error('Download error');
            return;
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);

        if (open) {
            window.open(url, '_blank');
            setTimeout(() => URL.revokeObjectURL(url), 30000);
        } else {
            const link = document.createElement('a');
            link.href = url;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        }
    };

    return (
        <button onClick={handleDownload} className="download-link">
            {children}
        </button>
    );
};