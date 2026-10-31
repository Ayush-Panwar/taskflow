import { useEffect, useId, useRef, type ReactNode } from 'react';

interface ModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
}

/**
 * Built on the native dialog element, which already traps keyboard focus inside the modal and
 * closes on Escape. It deliberately does not close on a backdrop click, so a stray click cannot
 * throw away a half-filled form.
 */
export function Modal({ title, onClose, children }: ModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) {
      return undefined;
    }
    dialog.showModal();
    dialog.querySelector<HTMLElement>('[data-autofocus]')?.focus();
    return () => dialog.close();
  }, []);

  return (
    <dialog
      ref={dialogRef}
      className="modal"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault(); // Let the parent decide, so React state stays the source of truth.
        onClose();
      }}
    >
      <div className="modal__header">
        <h2 className="modal__title" id={titleId}>
          {title}
        </h2>
        <button type="button" className="modal__close" aria-label="Close" onClick={onClose}>
          ×
        </button>
      </div>
      <div className="modal__body">{children}</div>
    </dialog>
  );
}
