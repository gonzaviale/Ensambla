package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.reporte.ResumenNegocio;

/** Escribe el resumen del negocio como un documento PDF para descargar. */
public interface GeneradorResumenPdfPort {

    byte[] generar(ResumenNegocio resumen);
}
