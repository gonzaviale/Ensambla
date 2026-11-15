package com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca;

import com.ensambla.backend.domain.model.marca.ColoresSugeridos;
import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.marca.LogoDeMarca;
import com.ensambla.backend.domain.model.marca.SugeridorDeColores;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.LectorDePixelesPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class SubirLogoDeMarca {

    private final NegocioRepository negocioRepository;

    private final KitDeMarcaRepository kitDeMarcaRepository;

    private final LectorDePixelesPort lectorDePixelesPort;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public SubirLogoDeMarcaResponse execute(SubirLogoDeMarcaRequest request) {
        LogoDeMarca logo = request.logo();
        ColoresSugeridos sugeridos = sugerirColoresDe(logo);
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return SubirLogoDeMarcaResponse.de(guardarLogo(negocioId, logo), sugeridos);
    }

    private ColoresSugeridos sugerirColoresDe(LogoDeMarca logo) {
        List<Integer> pixeles = leerPixeles(logo);
        return SugeridorDeColores.sugerir(pixeles, KitDeMarca.COLORES_DE_ENSAMBLA);
    }

    private List<Integer> leerPixeles(LogoDeMarca logo) {
        return lectorDePixelesPort.leer(logo);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private KitDeMarca guardarLogo(NegocioId negocioId, LogoDeMarca logo) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return transaccionPort.ejecutar(() -> ponerLogoYGuardar(negocioId, logo, ahora));
    }

    private KitDeMarca ponerLogoYGuardar(NegocioId negocioId, LogoDeMarca logo, OffsetDateTime ahora) {
        KitDeMarca kit = obtenerKitDelNegocio(negocioId, ahora);
        kit.ponerLogo(logo, ahora);
        return guardar(kit);
    }

    private KitDeMarca obtenerKitDelNegocio(NegocioId negocioId, OffsetDateTime ahora) {
        return kitDeMarcaRepository.obtenerOPredeterminado(negocioId, ahora);
    }

    private KitDeMarca guardar(KitDeMarca kit) {
        return kitDeMarcaRepository.save(kit);
    }
}
