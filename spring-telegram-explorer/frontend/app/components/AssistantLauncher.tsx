'use client';

import { useCallback, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { AiAssistantChat } from './AiAssistantChat';

export function AssistantLauncher() {
  const [open, setOpen] = useState(false);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const handleOpen = useCallback(() => {
    setOpen(true);
  }, []);

  const handleClose = useCallback(() => {
    setOpen(false);
  }, []);

  useEffect(() => {
    if (!open) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        setOpen(false);
      }
    };

    const { body } = document;
    const previousOverflow = body.style.overflow;
    body.style.overflow = 'hidden';
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [open]);

  return (
    <>
      <button
        type="button"
        className="btn btn-outline-primary app-contact-button"
        onClick={handleOpen}
      >
        Связаться
      </button>
      {mounted && open
        ? createPortal(
            <div
              className="assistant-modal-backdrop"
              role="dialog"
              aria-modal="true"
              aria-label="Окно ассистента"
              onClick={handleClose}
            >
              <div
                className="assistant-modal"
                onClick={(event) => event.stopPropagation()}
              >
                <button
                  type="button"
                  className="btn-close assistant-modal__close"
                  aria-label="Закрыть"
                  onClick={handleClose}
                />
                <AiAssistantChat active={open} />
              </div>
            </div>,
            document.body,
          )
        : null}
    </>
  );
}
