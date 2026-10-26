package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.DireccionOrden;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.RegistroRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public List<Registro> saveAll(List<Registro> registros) {
        return jpa.saveAll(registros.stream().map(mapper::toEntity).toList()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Registro> findByIdAndNegocioId(RegistroId id, NegocioId negocioId) {
        return jpa.findByIdAndNegocioId(id.valor(), negocioId.valor()).map(mapper::toDomain);
    }

    @Override
    public List<Registro> findAllByBloqueIdAndNegocioId(BloqueId bloqueId, NegocioId negocioId) {
        return jpa.findAllByBloqueIdAndNegocioIdOrderByCreadoEnAscIdAsc(bloqueId.valor(), negocioId.valor()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Registro> findAllByIdsAndBloque(Collection<RegistroId> ids, BloqueId bloqueId, NegocioId negocioId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpa.findAllByIdInAndBloqueIdAndNegocioIdOrderByCreadoEnDescIdDesc(Identificador.valores(ids),
                bloqueId.valor(), negocioId.valor()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(RegistroId id) {
        jpa.deleteById(id.valor());
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

    @Override
    public Map<CampoId, BigDecimal> sumarNumericos(ConsultaRegistros consulta, List<CampoId> campoIds) {
        if (campoIds.isEmpty()) {
            return Map.of();
        }
        return jpa.sumarNumericos(
                        consulta.negocioId().valor(),
                        consulta.bloqueId().valor(),
                        patronDe(consulta.texto()),
                        campoIds.stream().map(CampoId::toString).toList())
                .stream()
                .collect(Collectors.toMap(
                        fila -> CampoId.de(fila.getCampo()),
                        IRegistroJpaRepository.SumaPorCampo::getSuma));
    }

    @Override
    public List<Registro> buscarFiltrados(FiltroReporte filtro, int limite) {
        return jpa.buscarParaReporte(
                        filtro.negocioId().valor(),
                        filtro.bloqueId().valor(),
                        patronDe(filtro.texto()),
                        textoDe(filtro.desde()),
                        textoDe(filtro.hasta()),
                        textoDe(filtro.campoId()),
                        filtro.valor(),
                        limite)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long contarFiltrados(FiltroReporte filtro) {
        return jpa.contarParaReporte(
                filtro.negocioId().valor(),
                filtro.bloqueId().valor(),
                patronDe(filtro.texto()),
                textoDe(filtro.desde()),
                textoDe(filtro.hasta()),
                textoDe(filtro.campoId()),
                filtro.valor());
    }

    @Override
    public Map<CampoId, BigDecimal> sumarNumericos(FiltroReporte filtro, List<CampoId> campoIds) {
        if (campoIds.isEmpty()) {
            return Map.of();
        }
        return jpa.sumarNumericosParaReporte(
                        filtro.negocioId().valor(),
                        filtro.bloqueId().valor(),
                        patronDe(filtro.texto()),
                        textoDe(filtro.desde()),
                        textoDe(filtro.hasta()),
                        textoDe(filtro.campoId()),
                        filtro.valor(),
                        campoIds.stream().map(CampoId::toString).toList())
                .stream()
                .collect(Collectors.toMap(
                        fila -> CampoId.de(fila.getCampo()),
                        IRegistroJpaRepository.SumaPorCampo::getSuma));
    }

    private Page<RegistroEntity> consultar(ConsultaRegistros consulta) {
        PageRequest pageable = PageRequest.of(consulta.pagina(), consulta.tamanio());
        String patron = patronDe(consulta.texto());
        String campoOrden = consulta.campoOrdenId() == null ? null : consulta.campoOrdenId().toString();
        if (consulta.direccion() == DireccionOrden.ASC) {
            return jpa.buscarAscendente(consulta.negocioId().valor(), consulta.bloqueId().valor(), patron, campoOrden,
                    pageable);
        }
        return jpa.buscarDescendente(consulta.negocioId().valor(), consulta.bloqueId().valor(), patron, campoOrden,
                pageable);
    }

    private static String textoDe(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static String patronDe(String texto) {
        if (texto == null) {
            return null;
        }
        String escapado = texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
