package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.seccion.Seccion;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.entity.SeccionEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ISeccionJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ProyectoRepositoryMapper;
import com.ensambla.backend.infrastructure.repository.mapper.RegistroRepositoryMapper;
import com.ensambla.backend.infrastructure.repository.mapper.SeccionRepositoryMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Guarda el sistema entero respetando los ids que trae. Los ids los define la pantalla, así que no alcanza con
 * {@code save}: lo que ya existe se actualiza, lo nuevo se inserta y lo que ya no está se elimina.
 */
@Component
@RequiredArgsConstructor
public class SistemaRepositoryImpl implements SistemaRepository {

    private final IProyectoJpaRepository proyectoJpaRepository;

    private final ISeccionJpaRepository seccionJpaRepository;

    private final IBloqueJpaRepository bloqueJpaRepository;

    private final ICampoJpaRepository campoJpaRepository;

    private final IRegistroJpaRepository registroJpaRepository;

    private final ProyectoRepositoryMapper proyectoRepositoryMapper;

    private final SeccionRepositoryMapper seccionRepositoryMapper;

    private final RegistroRepositoryMapper registroRepositoryMapper;

    private final EntityManager entityManager;

    @Override
    public List<Sistema> findAllByNegocioId(NegocioId negocioId) {
        Map<ProyectoId, List<Seccion>> secciones = seccionJpaRepository
                .findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId.valor()).stream()
                .map(seccionRepositoryMapper::toDomain)
                .collect(Collectors.groupingBy(Seccion::getProyectoId));
        Map<BloqueId, List<Campo>> campos = campoJpaRepository
                .findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId.valor()).stream()
                .map(proyectoRepositoryMapper::toDomain)
                .collect(Collectors.groupingBy(Campo::getBloqueId));
        Map<ProyectoId, List<Bloque>> bloques = bloqueJpaRepository
                .findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId.valor()).stream()
                .map(proyectoRepositoryMapper::toDomain)
                .peek(bloque -> bloque.setCampos(campos.getOrDefault(bloque.getId(), List.of())))
                .collect(Collectors.groupingBy(Bloque::getProyectoId));
        Map<BloqueId, List<Registro>> registros = registroJpaRepository
                .findAllByNegocioIdOrderByOrdenAscCreadoEnAscIdAsc(negocioId.valor()).stream()
                .map(registroRepositoryMapper::toDomain)
                .collect(Collectors.groupingBy(Registro::getBloqueId, LinkedHashMap::new,
                        Collectors.toList()));
        return proyectoJpaRepository.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId.valor()).stream()
                .map(proyectoRepositoryMapper::toDomain)
                .map(proyecto -> sistema(proyecto, secciones, bloques, registros))
                .toList();
    }

    private Sistema sistema(Proyecto proyecto, Map<ProyectoId, List<Seccion>> secciones,
                            Map<ProyectoId, List<Bloque>> bloques, Map<BloqueId, List<Registro>> registros) {
        List<Bloque> suyos = bloques.getOrDefault(proyecto.getId(), List.of());
        proyecto.setBloques(suyos);
        Map<BloqueId, List<Registro>> filas = suyos.stream()
                .filter(bloque -> registros.containsKey(bloque.getId()))
                .collect(Collectors.toMap(Bloque::getId, bloque -> registros.get(bloque.getId())));
        return new Sistema(proyecto, secciones.getOrDefault(proyecto.getId(), List.of()), filas);
    }

    @Override
    public Optional<NegocioId> findNegocioIdByProyectoId(ProyectoId proyectoId) {
        return proyectoJpaRepository.findById(proyectoId.valor()).map(ProyectoEntity::getNegocioId).map(NegocioId::de);
    }

    @Override
    public void guardar(Sistema sistema) {
        NegocioId negocioId = sistema.proyecto().getNegocioId();
        ProyectoId proyectoId = sistema.id();
        Map<UUID, SeccionEntity> seccionesActuales = porId(
                seccionJpaRepository.findAllByProyectoIdAndNegocioId(proyectoId.valor(), negocioId.valor()),
                        SeccionEntity::getId);
        Map<UUID, BloqueEntity> bloquesActuales = porId(
                bloqueJpaRepository.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(proyectoId.valor(),
                        negocioId.valor()),
                BloqueEntity::getId);
        Set<UUID> bloqueIds = bloquesActuales.keySet();
        Map<UUID, CampoEntity> camposActuales = porId(
                campoJpaRepository.findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(
                        List.copyOf(bloqueIds), negocioId.valor()), CampoEntity::getId);
        Map<UUID, RegistroEntity> registrosActuales = porId(
                registroJpaRepository.findAllByBloqueIdInAndNegocioId(bloqueIds, negocioId.valor()),
                        RegistroEntity::getId);

        List<SeccionEntity> secciones = sistema.secciones().stream().map(seccionRepositoryMapper::toEntity).toList();
        List<BloqueEntity> bloques = sistema.proyecto().getBloques().stream()
                .map(proyectoRepositoryMapper::toEntity).toList();
        List<CampoEntity> campos = sistema.proyecto().getBloques().stream()
                .flatMap(bloque -> bloque.getCampos().stream())
                .map(proyectoRepositoryMapper::toEntity).toList();
        List<RegistroEntity> registros = sistema.registrosPorBloque().values().stream()
                .flatMap(Collection::stream)
                .map(registroRepositoryMapper::toEntity).toList();

        // Primero salen las filas y columnas que ya no están: una columna nueva puede reusar la clave de una vieja.
        quitar(registrosActuales, registros, RegistroEntity::getId);
        quitar(camposActuales, campos, CampoEntity::getId);
        entityManager.flush();

        guardarProyecto(sistema.proyecto());
        guardarTodos(secciones, seccionesActuales, SeccionEntity::getId, (nueva, actual) -> { });
        guardarTodos(bloques, bloquesActuales, BloqueEntity::getId, (nueva, actual) -> { });
        guardarTodos(campos, camposActuales, CampoEntity::getId,
                (nueva, actual) -> nueva.setValorInicial(actual.getValorInicial()));
        guardarTodos(registros, registrosActuales, RegistroEntity::getId,
                (nueva, actual) -> nueva.setFotoOrigenId(actual.getFotoOrigenId()));
        entityManager.flush();

        // Los bloques y secciones se eliminan al final: las columnas que apuntaban a ellos ya se corrigieron.
        quitar(bloquesActuales, bloques, BloqueEntity::getId);
        quitar(seccionesActuales, secciones, SeccionEntity::getId);
        entityManager.flush();
    }

    private void guardarProyecto(Proyecto proyecto) {
        ProyectoEntity nuevo = proyectoRepositoryMapper.toEntity(proyecto);
        Optional<ProyectoEntity> actual = proyectoJpaRepository.findById(nuevo.getId());
        if (actual.isPresent()) {
            nuevo.setDescripcion(actual.get().getDescripcion());
            nuevo.setPlantillaId(actual.get().getPlantillaId());
            entityManager.merge(nuevo);
        } else {
            entityManager.persist(nuevo);
        }
    }

    private <T> void guardarTodos(List<T> nuevos, Map<UUID, T> actuales, Function<T, UUID> id,
                                  BiConsumer<T, T> conservar) {
        for (T nuevo : nuevos) {
            T actual = actuales.get(id.apply(nuevo));
            if (actual == null) {
                entityManager.persist(nuevo);
            } else {
                conservar.accept(nuevo, actual);
                entityManager.merge(nuevo);
            }
        }
    }

    private <T> void quitar(Map<UUID, T> actuales, List<T> nuevos, Function<T, UUID> id) {
        Set<UUID> quedan = nuevos.stream().map(id).collect(Collectors.toSet());
        actuales.forEach((actualId, actual) -> {
            if (!quedan.contains(actualId)) {
                entityManager.remove(actual);
            }
        });
    }

    private static <T> Map<UUID, T> porId(List<T> entidades, Function<T, UUID> id) {
        return entidades.stream().collect(Collectors.toMap(id, entidad -> entidad));
    }
}
