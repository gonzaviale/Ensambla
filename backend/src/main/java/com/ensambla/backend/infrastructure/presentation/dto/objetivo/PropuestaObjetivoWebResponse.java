package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.FuenteDato;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.OrigenPropuesta;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.List;

/** Lo que Sami contesta y propone antes de guardar: ver {@code docs/objetivos-con-sami.md}. */
public record PropuestaObjetivoWebResponse(
        String respuesta,
        String entendido,
        TipoObjetivo tipo,
        boolean esMonetario,
        List<MiniPropuestoWebResponse> minis,
        List<DatoWebResponse> datos,
        OrigenPropuesta origen,
        int mensajesRestantes,
        boolean puedeGuardar,
        CuotaWebResponse cuota) {

    /** Los objetivos con IA del mes; {@code limite} y {@code restantes} vacíos si el plan no tiene límite. */
    public record CuotaWebResponse(long usadas, Integer limite, Integer restantes) {
    }

    public record MiniPropuestoWebResponse(
            ModoMiniObjetivo modo,
            TipoMiniObjetivo tipo,
            RegistroId registroId,
            String referencia,
            String titulo,
            String justificacion,
            BigDecimal valorInicial,
            BigDecimal meta,
            BigDecimal aporteEstimado,
            boolean textoConIa) {
    }

    public record DatoWebResponse(FuenteDato fuente, boolean hay) {
    }
}
