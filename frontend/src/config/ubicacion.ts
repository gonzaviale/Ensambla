// El proveedor de cartografía se puede cambiar sin tocar el mapa ni los componentes.
export const cartografia = {
  url: import.meta.env.VITE_MAPA_TILES_URL ?? 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  atribucion:
    import.meta.env.VITE_MAPA_ATRIBUCION ??
    '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  zoomInicial: 16,
  zoomMaximo: 19,
};
