package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.objetivo.CierreMini;
import com.ensambla.backend.domain.model.objetivo.ConteoDecisiones;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanalId;
import com.ensambla.backend.infrastructure.repository.entity.ReajusteMiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ResumenSemanalEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResumenSemanalRepositoryMapper {

    private static final String MINI_OBJETIVO_ID = "miniObjetivoId";

    private static final String VALOR = "valor";

    public ResumenSemanal toDomain(ResumenSemanalEntity source, List<ReajusteMiniObjetivoEntity> reajustes) {
        return new ResumenSemanal(
                ResumenSemanalId.de(source.getId()),
                ObjetivoId.de(source.getObjetivoId()),
                source.getSemanaInicio(),
                source.getResultado(),
                source.getAvance(),
                new ConteoDecisiones(
                        source.getDecisionesAplicadas(),
                        source.getDecisionesDescartadas(),
                        source.getDecisionesAceptadas(),
                        source.getDecisionesIgnoradas()),
                source.getMinis().stream().map(ResumenSemanalRepositoryMapper::cierreDe).toList(),
                source.getQueFunciono(),
                source.isTextoConIa(),
                source.getCerradoEn(),
                reajustes.stream().map(this::toDomain).toList());
    }

    public ReajusteMiniObjetivo toDomain(ReajusteMiniObjetivoEntity source) {
        return new ReajusteMiniObjetivo(
                ReajusteMiniObjetivoId.de(source.getId()),
                MiniObjetivoId.de(source.getMiniAnteriorId()),
                Identificador.opcional(source.getMiniNuevoId(), MiniObjetivoId::de),
                source.getTipo(),
                source.getMotivo(),
                source.getTituloAnterior(),
                source.getMetaAnterior(),
                source.getTituloNuevo(),
                source.getMetaNueva(),
                source.getCreadoEn());
    }

    public ResumenSemanalEntity toEntity(ResumenSemanal source) {
        return ResumenSemanalEntity.builder()
                .id(source.id().valor())
                .objetivoId(source.objetivoId().valor())
                .semanaInicio(source.semanaInicio())
                .resultado(source.resultado())
                .avance(source.avance())
                .decisionesAplicadas(source.decisiones().aplicadas())
                .decisionesDescartadas(source.decisiones().descartadas())
                .decisionesAceptadas(source.decisiones().aceptadas())
                .decisionesIgnoradas(source.decisiones().ignoradas())
                .minis(source.minis().stream().map(ResumenSemanalRepositoryMapper::jsonDe).toList())
                .queFunciono(source.queFunciono())
                .textoConIa(source.textoConIa())
                .cerradoEn(source.cerradoEn())
                .build();
    }

    public ReajusteMiniObjetivoEntity toEntity(ResumenSemanalId resumenId, ReajusteMiniObjetivo source) {
        return ReajusteMiniObjetivoEntity.builder()
                .id(source.id().valor())
                .resumenId(resumenId.valor())
                .miniAnteriorId(source.miniAnteriorId().valor())
                .miniNuevoId(Identificador.valorDe(source.miniNuevoId()))
                .tipo(source.tipo())
                .motivo(source.motivo())
                .tituloAnterior(source.tituloAnterior())
                .metaAnterior(source.metaAnterior())
                .tituloNuevo(source.tituloNuevo())
                .metaNueva(source.metaNueva())
                .creadoEn(source.creadoEn())
                .build();
    }

    /** El valor va como texto para no perder decimales al pasar por JSON. */
    private static Object jsonDe(CierreMini cierre) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put(MINI_OBJETIVO_ID, cierre.miniObjetivoId().toString());
        json.put(VALOR, cierre.valor().toPlainString());
        return json;
    }

    private static CierreMini cierreDe(Object json) {
        Map<?, ?> campos = (Map<?, ?>) json;
        return new CierreMini(
                MiniObjetivoId.de(String.valueOf(campos.get(MINI_OBJETIVO_ID))),
                new BigDecimal(String.valueOf(campos.get(VALOR))));
    }
}
