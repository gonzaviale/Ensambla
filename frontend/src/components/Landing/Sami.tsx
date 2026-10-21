// Carita de Sami para avatares chicos (por ejemplo el chat). Mismos colores y rasgos que la mascota
// grande: cabeza naranja de plastilina, ojos grandes con brillo y sonrisa.
export function SamiCara({ className }: { className?: string }) {
  return (
    <svg aria-hidden viewBox="0 0 48 48" className={className} xmlns="http://www.w3.org/2000/svg">
      <defs>
        <radialGradient id="sami-cara-cuerpo" cx="36%" cy="28%" r="86%">
          <stop offset="0" stopColor="#ffa257" />
          <stop offset="0.5" stopColor="#f47621" />
          <stop offset="1" stopColor="#dc520f" />
        </radialGradient>
      </defs>
      <circle cx="12" cy="38" r="5.5" fill="#f0691a" />
      <circle cx="24" cy="41.5" r="5.5" fill="#f0691a" />
      <circle cx="36" cy="38" r="5.5" fill="#f0691a" />
      <ellipse cx="24" cy="22" rx="19" ry="18.5" fill="url(#sami-cara-cuerpo)" />
      <ellipse cx="17" cy="21" rx="5.4" ry="6" fill="#fff4e4" />
      <ellipse cx="31" cy="19" rx="5.4" ry="6" fill="#fff4e4" />
      <circle cx="18" cy="21.5" r="3.2" fill="#0f2742" />
      <circle cx="32" cy="19.5" r="3.2" fill="#0f2742" />
      <circle cx="17" cy="20.2" r="1" fill="#fff" />
      <circle cx="31" cy="18.2" r="1" fill="#fff" />
      <path
        d="M20 30Q24.5 34 29 29.5"
        fill="none"
        stroke="#0f2742"
        strokeWidth="2.2"
        strokeLinecap="round"
      />
    </svg>
  );
}
