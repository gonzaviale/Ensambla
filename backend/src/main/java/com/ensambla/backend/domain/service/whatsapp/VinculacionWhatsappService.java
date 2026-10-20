package com.ensambla.backend.domain.service.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.CodigoVinculacionRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VinculacionWhatsappService {

    /** Sin 0/O ni 1/I/L: el usuario lo copia a mano y no se confunden. */
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LARGO_CODIGO = 6;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final CodigoVinculacionRepository codigoRepository;
    private final VinculoWhatsappRepository vinculoRepository;
    private final NegocioService negocioService;

    /** Reemplaza el código pendiente del usuario por uno nuevo, vigente por {@link CodigoVinculacion#VIGENCIA}. */
    @Transactional
    public CodigoVinculacion generarCodigo(UUID usuarioId, OffsetDateTime ahora) {
        codigoRepository.deleteSinUsarByUsuarioId(usuarioId);
        String codigo;
        do {
            codigo = nuevoCodigo();
        } while (codigoRepository.existsByCodigo(codigo));
        return codigoRepository.save(CodigoVinculacion.nuevo(usuarioId, codigo, ahora));
    }

    /** Vincula el teléfono con el negocio del usuario que generó el código; si ya tenía un número, lo reemplaza. */
    @Transactional
    public VinculoWhatsapp canjear(String textoCodigo, TelefonoWhatsapp telefono, OffsetDateTime ahora) {
        CodigoVinculacion codigo = codigoRepository.findByCodigo(textoCodigo.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessRuleException("El código de vinculación no existe"));
        codigo.usar(ahora);
        exigirTelefonoLibre(telefono, codigo.getUsuarioId());
        UUID negocioId = negocioService.obtenerDelUsuario(codigo.getUsuarioId());
        codigoRepository.save(codigo);
        vinculoRepository.deleteByUsuarioId(codigo.getUsuarioId());
        return vinculoRepository.save(VinculoWhatsapp.nuevo(codigo.getUsuarioId(), negocioId, telefono, ahora));
    }

    public VinculoWhatsapp obtener(UUID usuarioId) {
        return vinculoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un número de WhatsApp vinculado"));
    }

    public Optional<VinculoWhatsapp> buscarPorTelefono(TelefonoWhatsapp telefono) {
        return vinculoRepository.findByTelefono(telefono.valor());
    }

    @Transactional
    public void desvincular(UUID usuarioId) {
        obtener(usuarioId);
        vinculoRepository.deleteByUsuarioId(usuarioId);
    }

    private void exigirTelefonoLibre(TelefonoWhatsapp telefono, UUID usuarioId) {
        boolean ocupadoPorOtro = vinculoRepository.findByTelefono(telefono.valor())
                .filter(vinculo -> !vinculo.getUsuarioId().equals(usuarioId))
                .isPresent();
        if (ocupadoPorOtro) {
            throw new BusinessRuleException("Ese número de WhatsApp ya está vinculado a otra cuenta");
        }
    }

    private static String nuevoCodigo() {
        StringBuilder codigo = new StringBuilder(LARGO_CODIGO);
        for (int i = 0; i < LARGO_CODIGO; i++) {
            codigo.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }
}
