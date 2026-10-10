package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.enums.EstadoUsuario;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Crea el grafo usuario → negocio → proyecto → bloque → campo → registro; cada test hace rollback. */
@Transactional
public abstract class PostgresDatosTestSupport extends PostgresRepositoryTestSupport {

    protected static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    @Autowired
    protected IUsuarioJpaRepository usuarios;

    @Autowired
    protected INegocioJpaRepository negocios;

    @Autowired
    protected IProyectoJpaRepository proyectos;

    @Autowired
    protected IBloqueJpaRepository bloques;

    @Autowired
    protected ICampoJpaRepository campos;

    @Autowired
    protected IRegistroJpaRepository registros;

    protected UUID givenUsuario(String email) {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(email)
                .hashContrasenia("{bcrypt}x")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    protected NegocioEntity givenNegocio(String emailDuenio) {
        UUID duenioId = givenUsuario(emailDuenio);
        return negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio de " + emailDuenio)
                .duenioId(duenioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build());
    }

    protected UUID givenBloque(UUID negocioId, String nombre) {
        UUID proyectoId = proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocioId)
                .nombre("Proyecto " + nombre)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        return bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocioId)
                .proyectoId(proyectoId)
                .nombre(nombre)
                .posicion((short) 0)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    protected UUID givenCampo(UUID negocioId, UUID bloqueId, String nombre, TipoCampo tipo, int posicion) {
        return campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocioId)
                .bloqueId(bloqueId)
                .nombre(nombre)
                .tipo(tipo)
                .obligatorio(false)
                .opciones(List.of())
                .posicion((short) posicion)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    protected UUID givenRegistro(UUID negocioId, UUID bloqueId, Map<String, Object> valores, OffsetDateTime creadoEn) {
        return registros.saveAndFlush(RegistroEntity.builder()
                .negocioId(negocioId)
                .bloqueId(bloqueId)
                .valores(valores)
                .creadoEn(creadoEn)
                .actualizadoEn(creadoEn)
                .build()).getId();
    }
}
