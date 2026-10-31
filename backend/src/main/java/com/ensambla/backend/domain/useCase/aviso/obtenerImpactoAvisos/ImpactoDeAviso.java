package com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;

import java.time.OffsetDateTime;
import java.util.Optional;

/** El impacto ya medido de un aviso aplicado. */
public record ImpactoDeAviso(
        AvisoId avisoId, TipoAviso tipo, String titulo, OffsetDateTime aplicadoEn, ImpactoAviso impacto) {

    /** El impacto del aviso, o vacío si todavía no se midió. */
    public static Optional<ImpactoDeAviso> de(Aviso aviso) {
        return aviso.impacto().map(impacto ->
                new ImpactoDeAviso(aviso.getId(), aviso.getTipo(), aviso.getTitulo(), aviso.getResueltoEn(), impacto));
    }
}
