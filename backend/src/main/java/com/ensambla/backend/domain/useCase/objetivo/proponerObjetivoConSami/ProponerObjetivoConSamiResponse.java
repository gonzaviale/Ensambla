package com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami;

import com.ensambla.backend.domain.model.objetivo.DatoUsado;
import com.ensambla.backend.domain.model.objetivo.FuenteDato;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoPropuesto;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.OrigenPropuesta;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que Sami contesta y propone antes de guardar nada.
 *
 * @param entendido         cómo entendió el objetivo; {@code null} si todavía necesita aclarar
 * @param tipo              el tipo que tendría el objetivo al guardarlo; {@code null} si no hay nada para guardar
 * @param datos             qué fuentes tiene el negocio y cuáles no
 * @param mensajesRestantes cuántos mensajes más admite la conversación
 */
public record ProponerObjetivoConSamiResponse(
        String respuesta,
        String entendido,
        TipoObjetivo tipo,
        boolean esMonetario,
        List<MiniPropuestoItem> minis,
        List<DatoItem> datos,
        OrigenPropuesta origen,
        int mensajesRestantes,
        boolean puedeGuardar) {

    /**
     * Un mini-objetivo propuesto. Uno manual no tiene métrica ni números: su tipo, producto, referencia, valores,
     * meta y aporte son {@code null}.
     */
    public record MiniPropuestoItem(
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

        public static MiniPropuestoItem de(MiniObjetivoPropuesto propuesto) {
            if (propuesto.esManual()) {
                return new MiniPropuestoItem(propuesto.modo(), null, null, null, propuesto.accion().titulo(),
                        propuesto.accion().justificacion(), null, null, null, propuesto.textoConIa());
            }
            return new MiniPropuestoItem(propuesto.modo(), propuesto.candidato().tipo(),
                    propuesto.candidato().registroId(), propuesto.candidato().referencia(),
                    propuesto.candidato().titulo(), propuesto.candidato().justificacion(),
                    propuesto.candidato().valorInicial(), propuesto.candidato().meta(),
                    propuesto.candidato().aporteEstimado(), propuesto.textoConIa());
        }
    }

    public record DatoItem(FuenteDato fuente, boolean hay) {

        public static DatoItem de(DatoUsado dato) {
            return new DatoItem(dato.fuente(), dato.hay());
        }
    }

    public static ProponerObjetivoConSamiResponse de(
            PropuestaObjetivo propuesta, List<DatoUsado> datos, int mensajesRestantes) {
        return new ProponerObjetivoConSamiResponse(
                propuesta.respuesta(),
                propuesta.entendido(),
                propuesta.tipo().orElse(null),
                propuesta.esMonetario(),
                propuesta.minis().stream().map(MiniPropuestoItem::de).toList(),
                datos.stream().map(DatoItem::de).toList(),
                propuesta.origen(),
                mensajesRestantes,
                propuesta.puedeGuardar());
    }
}
