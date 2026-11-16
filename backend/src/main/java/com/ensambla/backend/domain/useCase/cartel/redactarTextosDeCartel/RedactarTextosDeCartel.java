package com.ensambla.backend.domain.useCase.cartel.redactarTextosDeCartel;

import com.ensambla.backend.domain.model.cartel.DatosDePromocion;
import com.ensambla.backend.domain.model.cartel.PedidoDeTextosDeCartel;
import com.ensambla.backend.domain.model.cartel.TextosDeCartel;
import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.RedaccionDeCartelIaPort;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.cartel.TextosDeCartelItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@RequiredArgsConstructor
public class RedactarTextosDeCartel {

    private final NegocioRepository negocioRepository;

    private final KitDeMarcaRepository kitDeMarcaRepository;

    private final ControlPlanService controlPlanService;

    private final RedaccionDeCartelIaPort redaccionDeCartelIaPort;

    private final Clock clock;

    public TextosDeCartelItem execute(RedactarTextosDeCartelRequest request) {
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        exigirCupoDeCartelesIa(negocio);
        DatosDePromocion datos = request.datos();
        Optional<TextosDeCartel> redactados = redactarConIa(pedidoDe(negocio, datos), datos);
        redactados.ifPresent(textos -> registrarCartelIa(negocio));
        return textosParaElCartel(redactados, datos, negocio);
    }

    private TextosDeCartelItem textosParaElCartel(Optional<TextosDeCartel> redactados, DatosDePromocion datos,
                                                  Negocio negocio) {
        TextosDeCartel textos = redactados.orElseGet(() -> TextosDeCartel.deReserva(datos, negocio.getNombre()));
        return TextosDeCartelItem.de(textos, redactados.isPresent());
    }

    private Optional<TextosDeCartel> redactarConIa(PedidoDeTextosDeCartel pedido, DatosDePromocion datos) {
        try {
            TextosDeCartel textos = redactar(pedido);
            textos.exigirCoherentesCon(datos);
            return Optional.of(textos);
        } catch (RuntimeException textoQueNoSirve) {
            return Optional.empty();
        }
    }

    private PedidoDeTextosDeCartel pedidoDe(Negocio negocio, DatosDePromocion datos) {
        return new PedidoDeTextosDeCartel(negocio.getNombre(), negocio.getRubro(), obtenerKitDelNegocio(negocio).getTono(),
                datos);
    }

    private Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerNegocioDelUsuario(usuarioId);
    }

    private KitDeMarca obtenerKitDelNegocio(Negocio negocio) {
        return kitDeMarcaRepository.obtenerOPredeterminado(negocio.getId(), OffsetDateTime.now(clock));
    }

    private void exigirCupoDeCartelesIa(Negocio negocio) {
        controlPlanService.exigirCupoDeCartelesIa(negocio.getId(), OffsetDateTime.now(clock));
    }

    private TextosDeCartel redactar(PedidoDeTextosDeCartel pedido) {
        return redaccionDeCartelIaPort.redactar(pedido).textos();
    }

    private void registrarCartelIa(Negocio negocio) {
        controlPlanService.registrarCartelIa(negocio.getId(), OffsetDateTime.now(clock));
    }
}
