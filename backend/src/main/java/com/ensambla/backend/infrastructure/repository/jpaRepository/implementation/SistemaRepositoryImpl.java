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

import java.time.OffsetDateTime;
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
        Map<ProyectoId, List<Seccion>> secciones = seccionesPorProyecto(negocioId);
        Map<BloqueId, List<Campo>> campos = camposPorBloque(negocioId);
        Map<ProyectoId, List<Bloque>> bloques = bloquesPorProyecto(negocioId, campos);
        Map<BloqueId, List<Registro>> registros = registrosPorBloque(negocioId);
        return proyectoJpaRepository.findAllByNegocioIdOrderByCreadoEnDescIdAsc(negocioId.valor()).stream()
                .map(proyectoRepositoryMapper::toDomain)
                .map(proyecto -> sistema(proyecto, secciones, bloques, registros))
                .toList();
    }

    private Map<ProyectoId, List<Seccion>> seccionesPorProyecto(NegocioId negocioId) {
        return seccionJpaRepository.findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId.valor()).stream()
                .map(seccionRepositoryMapper::toDomain)
                .collect(Collectors.groupingBy(Seccion::getProyectoId));
    }

    private Map<BloqueId, List<Campo>> camposPorBloque(NegocioId negocioId) {
        return campoJpaRepository.findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId.valor()).stream()
                .map(proyectoRepositoryMapper::toDomain)
                .collect(Collectors.groupingBy(Campo::getBloqueId));
    }

    private Map<ProyectoId, List<Bloque>> bloquesPorProyecto(NegocioId negocioId,
                                                             Map<BloqueId, List<Campo>> campos) {
        return bloqueJpaRepository.findAllByNegocioIdOrderByPosicionAscIdAsc(negocioId.valor()).stream()
                .map(proyectoRepositoryMapper::toDomain)
                .peek(bloque -> bloque.setCampos(campos.getOrDefault(bloque.getId(), List.of())))
                .collect(Collectors.groupingBy(Bloque::getProyectoId));
    }

    private Map<BloqueId, List<Registro>> registrosPorBloque(NegocioId negocioId) {
        return registroJpaRepository.findAllByNegocioIdOrderByOrdenAscCreadoEnAscIdAsc(negocioId.valor()).stream()
                .map(registroRepositoryMapper::toDomain)
                .collect(Collectors.groupingBy(Registro::getBloqueId, LinkedHashMap::new, Collectors.toList()));
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
    public void guardar(Sistema sistema, OffsetDateTime leidoEn) {
        Actuales actuales = leerLoGuardado(sistema.proyecto());
        Nuevos nuevos = aEntidades(sistema);
        quitarLoQueYaNoEsta(actuales, nuevos, leidoEn);
        guardarLoQueEsta(sistema.proyecto(), actuales, nuevos);
        quitarLaEstructuraQueYaNoEsta(actuales, nuevos);
    }

    /** Lo que hoy hay guardado del proyecto, por id, para decidir qué se crea, qué se actualiza y qué se quita. */
    private Actuales leerLoGuardado(Proyecto proyecto) {
        UUID negocioId = proyecto.getNegocioId().valor();
        UUID proyectoId = proyecto.getId().valor();
        Map<UUID, SeccionEntity> secciones = porId(
                seccionJpaRepository.findAllByProyectoIdAndNegocioId(proyectoId, negocioId), SeccionEntity::getId);
        Map<UUID, BloqueEntity> bloques = porId(
                bloqueJpaRepository.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(proyectoId, negocioId),
                BloqueEntity::getId);
        Set<UUID> bloqueIds = bloques.keySet();
        Map<UUID, CampoEntity> campos = porId(
                campoJpaRepository.findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(
                        List.copyOf(bloqueIds), negocioId), CampoEntity::getId);
        Map<UUID, RegistroEntity> registros = porId(
                registroJpaRepository.findAllByBloqueIdInAndNegocioId(bloqueIds, negocioId), RegistroEntity::getId);
        return new Actuales(secciones, bloques, campos, registros);
    }

    private Nuevos aEntidades(Sistema sistema) {
        List<SeccionEntity> secciones = sistema.secciones().stream().map(seccionRepositoryMapper::toEntity).toList();
        List<BloqueEntity> bloques = sistema.proyecto().getBloques().stream()
                .map(proyectoRepositoryMapper::toEntity).toList();
        List<CampoEntity> campos = sistema.proyecto().getBloques().stream()
                .flatMap(bloque -> bloque.getCampos().stream())
                .map(proyectoRepositoryMapper::toEntity).toList();
        List<RegistroEntity> registros = sistema.registrosPorBloque().values().stream()
                .flatMap(Collection::stream)
                .map(registroRepositoryMapper::toEntity).toList();
        return new Nuevos(secciones, bloques, campos, registros);
    }

    /** Primero salen las filas y columnas que ya no están: una columna nueva puede reusar la clave de una vieja. */
    private void quitarLoQueYaNoEsta(Actuales actuales, Nuevos nuevos, OffsetDateTime leidoEn) {
        quitarRegistros(actuales.registros(), nuevos.registros(), leidoEn);
        quitar(actuales.campos(), nuevos.campos(), CampoEntity::getId);
        entityManager.flush();
    }

    private void guardarLoQueEsta(Proyecto proyecto, Actuales actuales, Nuevos nuevos) {
        guardarProyecto(proyecto);
        guardarTodos(nuevos.secciones(), actuales.secciones(), SeccionEntity::getId,
                (nueva, actual) -> { });
        guardarTodos(nuevos.bloques(), actuales.bloques(), BloqueEntity::getId,
                (nueva, actual) -> { });
        guardarTodos(nuevos.campos(), actuales.campos(), CampoEntity::getId,
                (nueva, actual) -> nueva.setValorInicial(actual.getValorInicial()));
        guardarTodos(nuevos.registros(), actuales.registros(), RegistroEntity::getId,
                (nueva, actual) -> nueva.setFotoOrigenId(actual.getFotoOrigenId()));
        entityManager.flush();
    }

    /** Los bloques y secciones se eliminan al final: las columnas que apuntaban a ellos ya se corrigieron. */
    private void quitarLaEstructuraQueYaNoEsta(Actuales actuales, Nuevos nuevos) {
        quitar(actuales.bloques(), nuevos.bloques(), BloqueEntity::getId);
        quitar(actuales.secciones(), nuevos.secciones(), SeccionEntity::getId);
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

    /**
     * Las filas que el cliente no mandó se eliminan, salvo las que cargó Sami por WhatsApp después de que leyó el sistema
     * (mientras tenía la pestaña abierta): no las conocía, así que no las borró. Las filas que creó el propio cliente
     * se eliminan siempre que las quite, aunque sean más nuevas que su lectura.
     */
    private void quitarRegistros(Map<UUID, RegistroEntity> actuales, List<RegistroEntity> nuevos,
                                 OffsetDateTime leidoEn) {
        Set<UUID> quedan = nuevos.stream().map(RegistroEntity::getId).collect(Collectors.toSet());
        actuales.forEach((actualId, actual) -> {
            boolean lasCargoWhatsapp = leidoEn != null && Registro.ORIGEN_WHATSAPP.equals(actual.getOrigen())
                    && actual.getCreadoEn().isAfter(leidoEn);
            if (!quedan.contains(actualId) && !lasCargoWhatsapp) {
                entityManager.remove(actual);
            }
        });
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

    /** Lo que ya está guardado del proyecto, por id. */
    private record Actuales(Map<UUID, SeccionEntity> secciones, Map<UUID, BloqueEntity> bloques,
                            Map<UUID, CampoEntity> campos, Map<UUID, RegistroEntity> registros) {
    }

    /** Lo que se quiere dejar guardado, en el orden en que llegó. */
    private record Nuevos(List<SeccionEntity> secciones, List<BloqueEntity> bloques, List<CampoEntity> campos,
                          List<RegistroEntity> registros) {
    }
}
