package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.Comercio;
import com.ensambla.backend.domain.repository.pedido.ComercioRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ComercioRepositoryImpl implements ComercioRepository {

    private final INegocioJpaRepository negocios;

    @Override
    public List<Comercio> listarConWhatsapp() {
        return negocios.findConWhatsappVinculado().stream()
                .map(negocio -> new Comercio(NegocioId.de(negocio.getId()), negocio.getNombre()))
                .toList();
    }
}
