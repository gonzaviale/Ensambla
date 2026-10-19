package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.infrastructure.repository.entity.MensajeWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.VinculoWhatsappEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.MENSAJE_ID;
import static com.ensambla.backend.support.WhatsappTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.TELEFONO;
import static com.ensambla.backend.support.WhatsappTestData.audio;
import static com.ensambla.backend.support.WhatsappTestData.vinculado;
import static com.ensambla.backend.support.WhatsappTestData.vinculoConCodigo;
import static org.assertj.core.api.Assertions.assertThat;

class WhatsappRepositoryMappersTest {

    private final VinculoWhatsappRepositoryMapper vinculoWhatsappRepositoryMapper =
            new VinculoWhatsappRepositoryMapper();
    private final MensajeWhatsappRepositoryMapper mensajeWhatsappRepositoryMapper =
            new MensajeWhatsappRepositoryMapper();

    @Test
    @DisplayName("Mapea todos los campos del vínculo con código a la entidad")
    void vinculoToEntityShouldMapEveryField() {
        VinculoWhatsapp vinculo = vinculoConCodigo(NEGOCIO_ID);

        VinculoWhatsappEntity result = vinculoWhatsappRepositoryMapper.toEntity(vinculo);

        assertThat(result).usingRecursiveComparison().isEqualTo(vinculo);
    }

    @Test
    @DisplayName("Mapea todos los campos de la entidad del vínculo al dominio")
    void vinculoToDomainShouldMapEveryField() {
        VinculoWhatsappEntity entity = vinculoWhatsappRepositoryMapper.toEntity(vinculado(NEGOCIO_ID, TELEFONO));

        VinculoWhatsapp result = vinculoWhatsappRepositoryMapper.toDomain(entity);

        assertThat(result).usingRecursiveComparison().isEqualTo(entity);
    }

    @Test
    @DisplayName("Mapea todos los campos del mensaje a la entidad")
    void mensajeToEntityShouldMapEveryField() {
        MensajeWhatsapp mensaje = MensajeWhatsapp.nuevo(NEGOCIO_ID, audio(), AHORA);
        mensaje.setId(MENSAJE_ID);

        MensajeWhatsappEntity result = mensajeWhatsappRepositoryMapper.toEntity(mensaje);

        assertThat(result).usingRecursiveComparison().isEqualTo(mensaje);
    }

    @Test
    @DisplayName("Mapea todos los campos de la entidad del mensaje al dominio")
    void mensajeToDomainShouldMapEveryField() {
        MensajeWhatsappEntity entity = mensajeWhatsappRepositoryMapper.toEntity(
                MensajeWhatsapp.nuevo(NEGOCIO_ID, audio(), AHORA));

        MensajeWhatsapp result = mensajeWhatsappRepositoryMapper.toDomain(entity);

        assertThat(result).usingRecursiveComparison().isEqualTo(entity);
    }
}
