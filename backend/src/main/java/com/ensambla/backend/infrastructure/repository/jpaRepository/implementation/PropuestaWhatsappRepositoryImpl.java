package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.whatsapp.EstadoPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.PropuestaWhatsappRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPropuestaWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PropuestaWhatsappRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PropuestaWhatsappRepositoryImpl implements PropuestaWhatsappRepository {

    private static final List<EstadoPropuestaWhatsapp> ABIERTAS =
            List.of(EstadoPropuestaWhatsapp.ELIGIENDO_TABLA, EstadoPropuestaWhatsapp.PENDIENTE);

    private final IPropuestaWhatsappJpaRepository jpa;
    private final PropuestaWhatsappRepositoryMapper mapper;

    @Override
    public PropuestaWhatsapp save(PropuestaWhatsapp propuesta) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(propuesta)));
    }

    @Override
    public Optional<PropuestaWhatsapp> findAbiertaByTelefono(String telefono) {
        return jpa.findByTelefonoAndEstadoIn(telefono, ABIERTAS).map(mapper::toDomain);
    }
}
