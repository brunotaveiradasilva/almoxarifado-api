package com.almoxarifado.api.dados;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.almoxarifado.api.ads.AdsVendasPeriodoService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aba Dados: vendas de qualquer período direto da ADS, pra comparar um período com outro (mês contra
 * mês anterior, contra o mesmo mês do ano passado...). Só admins acessam (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/dados")
public class DadosController {

    /** Um ano (bissexto incluso) por consulta: mais que isso a varredura na ADS fica lenta demais. */
    private static final long MAX_DIAS = 366;

    private final AdsVendasPeriodoService vendas;

    public DadosController(AdsVendasPeriodoService vendas) {
        this.vendas = vendas;
    }

    /**
     * ?inicio=2025-09-01&fim=2025-09-30, opcionalmente &representanteId=...&fornecedorId=... — os dois
     * aceitam vários (repetindo o parâmetro ou separados por vírgula); sem nenhum, vale todos.
     */
    @GetMapping("/vendas")
    public VendasPeriodo vendas(
            @RequestParam String inicio,
            @RequestParam String fim,
            @RequestParam(name = "representanteId", required = false) List<String> representanteIds,
            @RequestParam(name = "fornecedorId", required = false) List<String> fornecedorIds) {
        LocalDate de = data(inicio);
        LocalDate ate = data(fim);
        if (ate.isBefore(de)) throw new ConsultaInvalidaException("O fim do período vem antes do início");
        if (ChronoUnit.DAYS.between(de, ate) >= MAX_DIAS) {
            throw new ConsultaInvalidaException("Escolha um período de até um ano");
        }
        return vendas.buscar(de, ate, semVazios(representanteIds), semVazios(fornecedorIds));
    }

    private static Set<String> semVazios(List<String> ids) {
        if (ids == null) return Set.of();
        return ids.stream().map(String::trim).filter(id -> !id.isEmpty()).collect(Collectors.toSet());
    }

    private static LocalDate data(String texto) {
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new ConsultaInvalidaException("Data inválida: \"" + texto + "\" (use o formato 2026-09-24)");
        }
    }
}
