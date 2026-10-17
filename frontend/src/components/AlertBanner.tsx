import React from 'react';

interface AlertBannerProps {
  type: 'error' | 'success' | 'info';
  message: string | null;
  onClose?: () => void;
}

export const AlertBanner: React.FC<AlertBannerProps> = ({ type, message, onClose }) => {
  if (!message) return null;

  return (
    <div className={`alert-banner alert-${type}`}>
      <div className="alert-content">
        <span className="alert-icon">
          {type === 'error' && '⚠️ '}
          {type === 'success' && '✅ '}
          {type === 'info' && 'ℹ️ '}
        </span>
        <span className="alert-message">{message}</span>
      </div>
      {onClose && (
        <button type="button" className="alert-close" onClick={onClose} aria-label="Close alert">
          &times;
        </button>
      )}
    </div>
  );
};

