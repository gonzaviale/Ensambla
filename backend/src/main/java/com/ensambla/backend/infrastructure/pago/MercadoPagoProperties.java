package com.ensambla.backend.infrastructure.pago;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ensambla.pagos.mercadopago")
public class MercadoPagoProperties {

    private boolean habilitado;

    private String accessToken = "";

    private String collectorId = "";

    private String applicationId = "";

    private String webhookSecret = "";

    private String notificationUrl = "";

    private String returnUrl = "";

    private String apiUrl = "https://api.mercadopago.com";

    private boolean produccion;
}
