export function Spinner({ size = 'regular' }: { size?: 'regular' | 'small' }) {
  return <span className={`spinner ${size === 'small' ? 'spinner--small' : ''}`} aria-hidden="true" />;
}

/** A centred spinner with a message, for when a whole area is waiting on a request. */
export function LoadingMessage({ label }: { label: string }) {
  return (
    <div className="state" role="status">
      <Spinner />
      <p className="state__text">{label}</p>
    </div>
  );
}
