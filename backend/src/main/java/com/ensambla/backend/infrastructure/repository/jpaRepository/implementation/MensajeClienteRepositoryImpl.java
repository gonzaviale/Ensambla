package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.pedido.MensajeCliente;
import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.repository.pedido.MensajeClienteRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMensajeClienteJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PedidoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MensajeClienteRepositoryImpl implements MensajeClienteRepository {

    private final IMensajeClienteJpaRepository jpa;
    private final PedidoRepositoryMapper mapper;

    @Override
    public MensajeCliente save(MensajeCliente mensaje) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(mensaje)));
    }

    @Override
    public boolean existsByIdExterno(String idExterno) {
        return jpa.existsByIdExterno(idExterno);
    }

    @Override
    @Transactional
    public Optional<MensajeCliente> reclamarSiguiente(OffsetDateTime ahora, Duration vigencia) {
        return jpa.findSiguienteRecibidoBloqueado()
                .map(mapper::toDomain)
                .map(mensaje -> {
                    mensaje.reclamar(ahora, vigencia);
                    return save(mensaje);
                });
    }

    @Override
    public List<MensajeCliente> vencidos(OffsetDateTime ahora) {
        return jpa.findByEstadoAndReclamadoHastaBefore(EstadoMensajeWhatsapp.ATENDIENDO, ahora).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
