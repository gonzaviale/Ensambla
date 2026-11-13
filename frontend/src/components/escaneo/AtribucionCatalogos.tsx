export function AtribucionCatalogos() {
  return (
    <details className="escaneo-creditos">
      <summary>Catálogos de referencia y atribución</summary>
      <p>
        <a
          href="https://datos.produccion.gob.ar/dataset/sepa-precios"
          target="_blank"
          rel="noreferrer"
        >
          SEPA / Secretaría de Comercio
        </a>{' '}
        · CC BY 4.0.
      </p>
      <p>
        <a href="https://world.openfoodfacts.org/terms-of-use" target="_blank" rel="noreferrer">
          Open Food Facts y proyectos asociados
        </a>{' '}
        · ODbL / DbCL; fotos CC BY-SA.
      </p>
      <p>
        Son referencias: corroborá el envase y la presentación. El precio y el stock son los de tu
        negocio.
      </p>
    </details>
  );
}
