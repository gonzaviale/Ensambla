// Sprite de íconos del mockup (index.html), tal cual. Se monta una sola vez y los íconos lo usan con <use>.
export function IconSprite() {
  return (
    <svg className="icon-definitions" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <symbol id="i-arrow" viewBox="0 0 24 24">
          <path d="M5 12h14m-6-6 6 6-6 6" />
        </symbol>
        <symbol id="i-back" viewBox="0 0 24 24">
          <path d="M19 12H5m6-6-6 6 6 6" />
        </symbol>
        <symbol id="i-plus" viewBox="0 0 24 24">
          <path d="M12 5v14M5 12h14" />
        </symbol>
        <symbol id="i-close" viewBox="0 0 24 24">
          <path d="m6 6 12 12M6 18 18 6" />
        </symbol>
        <symbol id="i-check" viewBox="0 0 24 24">
          <path d="m5 12 4 4L19 6" />
        </symbol>
        <symbol id="i-box" viewBox="0 0 24 24">
          <path d="m12 3 9 5v8l-9 5-9-5V8l9-5Zm0 10 9-5M3 8l9 5v8M7.5 5.5l9 5" />
        </symbol>
        <symbol id="i-orders" viewBox="0 0 24 24">
          <rect x="5" y="4" width="14" height="17" rx="2" />
          <path d="M9 4V2h6v2M9 10h6M9 14h6M9 18h3" />
        </symbol>
        <symbol id="i-users" viewBox="0 0 24 24">
          <circle cx="9" cy="8" r="3" />
          <path d="M3 21v-3a6 6 0 0 1 12 0v3m2-16a3 3 0 0 1 0 6m4 10v-3a6 6 0 0 0-4-5" />
        </symbol>
        <symbol id="i-spark" viewBox="0 0 24 24">
          <path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3ZM20 2v4m-2-2h4" />
        </symbol>
        <symbol id="i-grid" viewBox="0 0 24 24">
          <rect x="3" y="3" width="7" height="7" rx="2" />
          <rect x="14" y="3" width="7" height="7" rx="2" />
          <rect x="3" y="14" width="7" height="7" rx="2" />
          <rect x="14" y="14" width="7" height="7" rx="2" />
        </symbol>
        <symbol id="i-home" viewBox="0 0 24 24">
          <path d="m3 10 9-7 9 7v9a2 2 0 0 1-2 2h-4v-8H9v8H5a2 2 0 0 1-2-2v-9Z" />
        </symbol>
        <symbol id="i-image" viewBox="0 0 24 24">
          <rect x="3" y="3" width="18" height="18" rx="3" />
          <circle cx="8" cy="8" r="1.5" />
          <path d="m21 16-6-6L3 21" />
        </symbol>
        <symbol id="i-camera" viewBox="0 0 24 24">
          <path d="M4 6h4l2-3h4l2 3h4a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1Z" />
          <circle cx="12" cy="13" r="4" />
        </symbol>
        <symbol id="i-search" viewBox="0 0 24 24">
          <circle cx="10.5" cy="10.5" r="6.5" />
          <path d="m16 16 5 5" />
        </symbol>
        <symbol id="i-help" viewBox="0 0 24 24">
          <circle cx="12" cy="12" r="9" />
          <path d="M9.5 8a2.6 2.6 0 0 1 5 1c0 2-2.5 2-2.5 4m0 3h.01" />
        </symbol>
        <symbol id="i-chevron" viewBox="0 0 24 24">
          <path d="m9 5 7 7-7 7" />
        </symbol>
        <symbol id="i-down" viewBox="0 0 24 24">
          <path d="m6 9 6 6 6-6" />
        </symbol>
        <symbol id="i-up" viewBox="0 0 24 24">
          <path d="m6 15 6-6 6 6" />
        </symbol>
        <symbol id="i-grip" viewBox="0 0 24 24">
          <path d="M9 5h.01M15 5h.01M9 12h.01M15 12h.01M9 19h.01M15 19h.01" strokeWidth="3.5" />
        </symbol>
        <symbol id="i-edit" viewBox="0 0 24 24">
          <path d="m14 5 5 5M4 20l5-1L21 7a2 2 0 0 0-5-5L4 14l-1 7 6-2" />
        </symbol>
        <symbol id="i-trash" viewBox="0 0 24 24">
          <path d="M3 6h18M9 6V3h6v3M6 6l1 15h10l1-15M10 10v7m4-7v7" />
        </symbol>
        <symbol id="i-copy" viewBox="0 0 24 24">
          <rect x="8" y="8" width="13" height="13" rx="2" />
          <path d="M16 8V3H3v13h5" />
        </symbol>
        <symbol id="i-settings" viewBox="0 0 24 24">
          <path d="M4 7h16M4 17h16M8 4v6m8 4v6" />
        </symbol>
        <symbol id="i-tools" viewBox="0 0 24 24">
          <path
            d="M14 5a6 6 0 0 0-7 7L2 17a3 3 0 0 0 5 5l5-6a6 6 0 0 0 7-7l-4 4-4-4 3-4Z"
            transform="translate(1 -2)"
          />
        </symbol>
        <symbol id="i-wallet" viewBox="0 0 24 24">
          <path d="M20 8V4H5a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h16V8H5a2 2 0 0 1 0-4m16 9h-6v4h6" />
        </symbol>
        <symbol id="i-chart" viewBox="0 0 24 24">
          <path d="M4 20V10m8 10V4m8 16v-7" strokeWidth="4" />
        </symbol>
        <symbol id="i-note" viewBox="0 0 24 24">
          <path d="M14 3H4v18h16V9l-6-6Zm0 0v6h6M8 13h8M8 17h5" />
        </symbol>
        <symbol id="i-form" viewBox="0 0 24 24">
          <rect x="4" y="3" width="16" height="18" rx="2" />
          <path d="M8 8h8m-8 4h8m-8 4h4" />
        </symbol>
        <symbol id="i-link" viewBox="0 0 24 24">
          <path
            d="m10 13 4-4M8 16l-2 2a4 4 0 0 1-6-6l5-5a4 4 0 0 1 6 0m2 1 2-2a4 4 0 0 1 6 6l-5 5a4 4 0 0 1-6 0"
            transform="translate(1 0)"
          />
        </symbol>
        <symbol id="i-upload" viewBox="0 0 24 24">
          <path d="M12 16V3m-5 5 5-5 5 5M3 15v6h18v-6" />
        </symbol>
        <symbol id="i-download" viewBox="0 0 24 24">
          <path d="M12 3v13m-5-5 5 5 5-5M3 15v6h18v-6" />
        </symbol>
        <symbol id="i-undo" viewBox="0 0 24 24">
          <path d="M4 5v7h7M4 12a8 8 0 1 1 2 7" />
        </symbol>
        <symbol id="i-clock" viewBox="0 0 24 24">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7v5l3 2" />
        </symbol>
        <symbol id="i-eye" viewBox="0 0 24 24">
          <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7S2 12 2 12Z" />
          <circle cx="12" cy="12" r="3" />
        </symbol>
        <symbol id="i-check-circle" viewBox="0 0 24 24">
          <circle cx="12" cy="12" r="9" />
          <path d="m7 12 3 3 7-7" />
        </symbol>
        <symbol id="i-folder" viewBox="0 0 24 24">
          <path d="M3 7V4h6l3 3h9v13H3V7Z" />
        </symbol>
        <symbol id="i-bell" viewBox="0 0 24 24">
          <path d="M6 16V11a6 6 0 0 1 12 0v5l2 2H4l2-2Zm4 4a2 2 0 0 0 4 0" />
        </symbol>
        <symbol id="i-menu" viewBox="0 0 24 24">
          <path d="M4 6h16M4 12h16M4 18h16" />
        </symbol>
        <symbol id="i-pin" viewBox="0 0 24 24">
          <path d="M9 3h6l-1 7 4 4H6l4-4-1-7Zm3 11v7" />
        </symbol>
        <symbol id="i-mic" viewBox="0 0 24 24">
          <rect x="9" y="3" width="6" height="11" rx="3" />
          <path d="M5 11a7 7 0 0 0 14 0m-7 7v3m-3 0h6" />
        </symbol>
      </defs>
    </svg>
  );
}
