package com.almoxarifado.api.metarepresentante;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Mês de referência das metas, no formato "2026-09". O "mês atual" segue o horário de Brasília —
 * o servidor roda em UTC, e às 21h do último dia do mês ele já estaria no mês seguinte.
 */
public final class Mes {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private static final DateTimeFormatter POR_EXTENSO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.of("pt", "BR"));

    private Mes() {
    }

    public static YearMonth atual() {
        return YearMonth.now(FUSO);
    }

    /**
     * Mês que já acabou não aceita mais mudança de meta (em outubro, setembro fica só pra consulta).
     * O realizado continua vindo da ADS nos primeiros dias do mês seguinte — isso não passa por aqui.
     */
    public static void garantirAberto(YearMonth mes) {
        if (mes.isBefore(atual())) {
            throw new MesFechadoException("As metas de " + mes.format(POR_EXTENSO)
                    + " não podem mais ser alteradas: o mês já fechou");
        }
    }

    /** Lê "2026-09"; vazio/nulo vira o mês atual. */
    public static YearMonth ler(String texto) {
        if (texto == null || texto.isBlank()) return atual();
        try {
            return YearMonth.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new MesInvalidoException("Mês inválido: \"" + texto + "\" (use o formato 2026-09)");
        }
    }
}
