package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.marca.ColorHex;
import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.marca.KitDeMarcaId;
import com.ensambla.backend.domain.model.marca.LogoDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.KitDeMarcaEntity;
import org.springframework.stereotype.Component;

@Component
public class KitDeMarcaRepositoryMapper {

    public KitDeMarca toDomain(KitDeMarcaEntity entity) {
        return KitDeMarca.builder()
                .id(Identificador.opcional(entity.getId(), KitDeMarcaId::de))
                .negocioId(Identificador.opcional(entity.getNegocioId(), NegocioId::de))
                .colorPrincipal(ColorHex.de(entity.getColorPrincipal()))
                .colorSecundario(ColorHex.de(entity.getColorSecundario()))
                .tipografia(entity.getTipografia())
                .tono(entity.getTono())
                .logo(logoDe(entity))
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public KitDeMarcaEntity toEntity(KitDeMarca kit) {
        return KitDeMarcaEntity.builder()
                .id(Identificador.valorDe(kit.getId()))
                .negocioId(Identificador.valorDe(kit.getNegocioId()))
                .colorPrincipal(kit.getColorPrincipal().valor())
                .colorSecundario(kit.getColorSecundario().valor())
                .tipografia(kit.getTipografia())
                .tono(kit.getTono())
                .logo(kit.tieneLogo() ? kit.getLogo().contenido() : null)
                .logoTipoMime(kit.tieneLogo() ? kit.getLogo().tipoMime() : null)
                .creadoEn(kit.getCreadoEn())
                .actualizadoEn(kit.getActualizadoEn())
                .build();
    }

    private LogoDeMarca logoDe(KitDeMarcaEntity entity) {
        return entity.getLogo() == null ? null : new LogoDeMarca(entity.getLogo(), entity.getLogoTipoMime());
    }
}
