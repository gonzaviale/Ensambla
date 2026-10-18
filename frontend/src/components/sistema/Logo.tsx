import { BrandMark } from './BrandMark';

export function Logo({ onClick }: { onClick: () => void }) {
  return (
    <button type="button" className="logo" aria-label="Ensambla, ir al inicio" onClick={onClick}>
      <BrandMark />
      <span>Ensambla</span>
    </button>
  );
}
