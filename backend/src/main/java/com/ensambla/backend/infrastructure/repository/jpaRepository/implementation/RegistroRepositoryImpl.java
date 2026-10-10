package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.enums.DireccionOrden;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.RegistroRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RegistroRepositoryImpl implements RegistroRepository {

    private final IRegistroJpaRepository jpa;
    private final RegistroRepositoryMapper mapper;

    @Override
    public Registro save(Registro registro) {
        return mapper.toDomain(jpa.save(mapper.toEntity(registro)));
    }

    @Override
    public Optional<Registro> findByIdAndNegocioId(UUID id, UUID negocioId) {
        return jpa.findByIdAndNegocioId(id, negocioId).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public Pagina<Registro> buscar(ConsultaRegistros consulta) {
        Page<RegistroEntity> pagina = consultar(consulta);
        return new Pagina<>(
                pagina.getContent().stream().map(mapper::toDomain).toList(),
                consulta.pagina(),
                consulta.tamanio(),
                pagina.getTotalElements());
    }

    private Page<RegistroEntity> consultar(ConsultaRegistros consulta) {
        PageRequest pageable = PageRequest.of(consulta.pagina(), consulta.tamanio());
        String patron = patronDe(consulta.texto());
        String campoOrden = consulta.campoOrdenId() == null ? null : consulta.campoOrdenId().toString();
        if (consulta.direccion() == DireccionOrden.ASC) {
            return jpa.buscarAscendente(consulta.negocioId(), consulta.bloqueId(), patron, campoOrden, pageable);
        }
        return jpa.buscarDescendente(consulta.negocioId(), consulta.bloqueId(), patron, campoOrden, pageable);
    }

    private static String patronDe(String texto) {
        if (texto == null) {
            return null;
        }
        String escapado = texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
