package com.ensambla.backend.domain.useCase.sistema.listarSistemas;

import com.ensambla.backend.domain.model.bloque.EstructuraFija;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Los negocios de la cuenta. Al abrirlos completa lo que les falte de la estructura fija (una sección de cada tipo
 * obligatorio y sus columnas) y lo deja guardado, sin tocar los datos que ya tienen.
 */
@RequiredArgsConstructor
public class ListarSistemas {

    private final NegocioRepository negocioRepository;

    private final SistemaRepository sistemaRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public List<SistemaItem> execute(ListarSistemasRequest request) {
        OffsetDateTime leidoEn = OffsetDateTime.now(clock);
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return obtenerSistemas(negocioId).stream()
                .map(sistema -> completarSiHaceFalta(sistema, leidoEn))
                .map(SistemaItem::de)
                .toList();
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private List<Sistema> obtenerSistemas(NegocioId negocioId) {
        return sistemaRepository.findAllByNegocioId(negocioId);
    }

    private Sistema completarSiHaceFalta(Sistema sistema, OffsetDateTime leidoEn) {
        if (EstructuraFija.estaCompleta(sistema)) {
            return sistema;
        }
        return transaccionPort.ejecutar(() -> completarYGuardar(sistema, leidoEn));
    }

    private Sistema completarYGuardar(Sistema sistema, OffsetDateTime leidoEn) {
        Sistema completo = EstructuraFija.completar(sistema, leidoEn);
        guardarSistema(completo, leidoEn);
        return completo;
    }

    private void guardarSistema(Sistema sistema, OffsetDateTime leidoEn) {
        sistemaRepository.guardar(sistema, leidoEn);
    }
}
