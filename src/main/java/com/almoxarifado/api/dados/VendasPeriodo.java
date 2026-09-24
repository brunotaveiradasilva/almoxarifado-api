package com.almoxarifado.api.dados;

import java.time.LocalDate;
import java.util.List;

/**
 * Quanto foi vendido num período (aba Dados do front-end), por representante e no total. O front
 * pede dois períodos e compara um com o outro.
 *
 * @param representantes um por representante do cadastro com código ADS (ou só o escolhido), mesmo
 *                       sem venda no período
 * @param total          soma de todos; os clientes do total são contados sem repetir, então não é
 *                       a soma dos clientes de cada representante (um cliente pode comprar de dois)
 */
public record VendasPeriodo(LocalDate inicio, LocalDate fim, List<VendasRepresentante> representantes, Valores total) {

    /**
     * @param codigoAds       código do representante na ADS (o {@code repr_id} usado na busca)
     * @param representanteId id no cadastro daqui — a chave pra casar os dois períodos
     */
    public record VendasRepresentante(String codigoAds, String representanteId, String nome, Valores valores) {
    }

    /**
     * Vendas menos devoluções, sem bonificação (mesma regra do realizado das metas).
     *
     * @param valor    R$ do produto
     * @param kg       peso bruto
     * @param clientes clientes diferentes com saldo positivo em R$
     */
    public record Valores(double valor, double kg, long clientes) {
    }
}
