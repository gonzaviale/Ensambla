package com.ensambla.backend.domain.useCase.cartel.exportarCartel;

import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.OpcionesDeCartel;
import com.ensambla.backend.domain.model.cartel.SalidaDeCartel;
import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.ExportadorDeCartelPort;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/** Baja el cartel que la persona eligió: PDF para imprimir (A4, A5) o PNG para redes (cuadrado, vertical). */
@RequiredArgsConstructor
public class ExportarCartel {

    private final NegocioRepository negocioRepository;

    private final KitDeMarcaRepository kitDeMarcaRepository;

    private final List<ExportadorDeCartelPort> exportadores;

    private final Clock clock;

    public ExportarCartelResponse execute(ExportarCartelRequest request) {
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        ComposicionDeCartel composicion = componer(request, obtenerKitDelNegocio(negocio), negocio);
        return ExportarCartelResponse.de(exportar(composicion), request.tipo(), composicion);
    }

    private ComposicionDeCartel componer(ExportarCartelRequest request, KitDeMarca kit, Negocio negocio) {
        return OpcionesDeCartel.componer(request.variante(), kit, request.tipo(), request.formato(),
                request.contenidoDe(negocio.getNombre()));
    }

    private Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerNegocioDelUsuario(usuarioId);
    }

    private KitDeMarca obtenerKitDelNegocio(Negocio negocio) {
        return kitDeMarcaRepository.obtenerOPredeterminado(negocio.getId(), OffsetDateTime.now(clock));
    }

    private byte[] exportar(ComposicionDeCartel composicion) {
        return exportadorPara(composicion.formato().salida()).exportar(composicion);
    }

    private ExportadorDeCartelPort exportadorPara(SalidaDeCartel salida) {
        return exportadores.stream().filter(exportador -> exportador.salida() == salida).findFirst().orElseThrow();
    }
}
