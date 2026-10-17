import React, { useState } from 'react';

interface ChipsInputProps {
  label?: string;
  chips: string[];
  onChange: (chips: string[]) => void;
  placeholder?: string;
  disabled?: boolean;
}

export const ChipsInput: React.FC<ChipsInputProps> = ({
  label,
  chips,
  onChange,
  placeholder = 'Type skill and press Enter or comma',
  disabled = false,
}) => {
  const [inputValue, setInputValue] = useState('');

  const addChip = (rawVal: string) => {
    const trimmed = rawVal.trim().toLowerCase();
    if (trimmed && !chips.includes(trimmed)) {
      onChange([...chips, trimmed]);
    }
    setInputValue('');
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      addChip(inputValue);
    } else if (e.key === 'Backspace' && !inputValue && chips.length > 0) {
      e.preventDefault();
      onChange(chips.slice(0, -1));
    }
  };

  const handleRemove = (chipToRemove: string) => {
    onChange(chips.filter((chip) => chip !== chipToRemove));
  };

  return (
    <div className="chips-input-container">
      {label && <label className="form-label">{label}</label>}
      <div className="chips-wrapper">
        {chips.map((chip) => (
          <span key={chip} className="chip">
            {chip}
            {!disabled && (
              <button
                type="button"
                className="chip-remove"
                onClick={() => handleRemove(chip)}
                aria-label={`Remove ${chip}`}
              >
                &times;
              </button>
            )}
          </span>
        ))}
        <input
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          onBlur={() => {
            if (inputValue.trim()) {
              addChip(inputValue);
            }
          }}
          placeholder={chips.length === 0 ? placeholder : 'Add more...'}
          disabled={disabled}
          className="chips-text-input"
        />
      </div>
    </div>
  );
};

