package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import org.springframework.stereotype.Component;

@Component
public class RegistroRepositoryMapper {

    public Registro toDomain(RegistroEntity entity) {
        return Registro.builder()
                .id(Identificador.opcional(entity.getId(), RegistroId::de))
                .negocioId(Identificador.opcional(entity.getNegocioId(), NegocioId::de))
                .bloqueId(Identificador.opcional(entity.getBloqueId(), BloqueId::de))
                .valores(entity.getValores())
                .fotoOrigenId(Identificador.opcional(entity.getFotoOrigenId(), FotoId::de))
                .esEjemplo(entity.getEsEjemplo())
                .origen(entity.getOrigen())
                .orden(entity.getOrden())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public RegistroEntity toEntity(Registro registro) {
        return RegistroEntity.builder()
                .id(Identificador.valorDe(registro.getId()))
                .negocioId(Identificador.valorDe(registro.getNegocioId()))
                .bloqueId(Identificador.valorDe(registro.getBloqueId()))
                .valores(registro.getValores())
                .fotoOrigenId(Identificador.valorDe(registro.getFotoOrigenId()))
                .esEjemplo(registro.getEsEjemplo())
                .origen(registro.getOrigen())
                .orden(registro.getOrden())
                .creadoEn(registro.getCreadoEn())
                .actualizadoEn(registro.getActualizadoEn())
                .build();
    }
}
