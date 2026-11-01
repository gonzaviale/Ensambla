package com.ensambla.backend.infrastructure.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Código que se mandó por WhatsApp para verificar el número que la persona cargó en la app. */
@Entity
@Table(name = "whatsapp_verificacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionWhatsappEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(nullable = false, columnDefinition = "text")
    private String telefono;

    @Column(name = "codigo_hash", nullable = false, columnDefinition = "text")
    private String codigoHash;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "enviado_en", nullable = false)
    private OffsetDateTime enviadoEn;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(name = "envios_ventana", nullable = false)
    private int enviosVentana;

    @Column(name = "ventana_desde", nullable = false)
    private OffsetDateTime ventanaDesde;
}
