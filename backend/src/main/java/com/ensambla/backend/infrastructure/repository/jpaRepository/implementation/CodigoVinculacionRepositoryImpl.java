package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.repository.whatsapp.CodigoVinculacionRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICodigoVinculacionJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.WhatsappRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
@RequiredArgsConstructor
public class CodigoVinculacionRepositoryImpl implements CodigoVinculacionRepository {

    private final ICodigoVinculacionJpaRepository jpa;
    private final WhatsappRepositoryMapper mapper;

    @Override
    public CodigoVinculacion save(CodigoVinculacion codigo) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(codigo)));
    }

    @Override
    public Optional<CodigoVinculacion> findByCodigo(String codigo) {
        return jpa.findByCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return jpa.existsByCodigo(codigo);
    }

    @Override
    public void deleteSinUsarByUsuarioId(UsuarioId usuarioId) {
        jpa.deleteSinUsarByUsuarioId(usuarioId.valor());
    }
}
