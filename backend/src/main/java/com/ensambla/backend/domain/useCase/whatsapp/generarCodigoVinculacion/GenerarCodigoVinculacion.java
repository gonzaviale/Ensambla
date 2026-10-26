package com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.whatsapp.CodigoVinculacionRepository;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class GenerarCodigoVinculacion {

    /** Sin 0/O ni 1/I/L: el usuario lo copia a mano y no se confunden. */
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LARGO_CODIGO = 6;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final NegocioRepository negocioRepository;
    private final CodigoVinculacionRepository codigoRepository;
    private final MensajeriaWhatsappPort mensajeria;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    /** Reemplaza el código pendiente del usuario por uno nuevo, vigente por {@link CodigoVinculacion#VIGENCIA}. */
    public GenerarCodigoVinculacionResponse execute(GenerarCodigoVinculacionRequest request) {
        negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        CodigoVinculacion codigo = transaccionPort.ejecutar(
                () -> generar(request.usuarioId(), OffsetDateTime.now(clock)));
        return GenerarCodigoVinculacionResponse.de(codigo, mensajeria.numeroBot());
    }

    private CodigoVinculacion generar(UsuarioId usuarioId, OffsetDateTime ahora) {
        codigoRepository.deleteSinUsarByUsuarioId(usuarioId);
        String codigo;
        do {
            codigo = nuevoCodigo();
        } while (codigoRepository.existsByCodigo(codigo));
        return codigoRepository.save(CodigoVinculacion.nuevo(usuarioId, codigo, ahora));
    }

    private static String nuevoCodigo() {
        StringBuilder codigo = new StringBuilder(LARGO_CODIGO);
        for (int i = 0; i < LARGO_CODIGO; i++) {
            codigo.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }
}
