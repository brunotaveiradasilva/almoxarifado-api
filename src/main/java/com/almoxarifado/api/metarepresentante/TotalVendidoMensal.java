package com.almoxarifado.api.metarepresentante;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Tudo que um representante vendeu num mês (todos os fornecedores), calculado na sincronização com a
 * ADS — é o card "Total vendido". Guarda só o id do representante, sem chave estrangeira, pra não
 * impedir a exclusão de um representante que já teve vendas.
 */
@Entity
@Table(name = "totais_vendidos_mensais",
        uniqueConstraints = @UniqueConstraint(name = "uk_total_vendido_mes", columnNames = { "representante_id", "mes" }))
public class TotalVendidoMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "representante_id", nullable = false)
    private String representanteId;

    @Column(nullable = false, length = 7)
    private String mes;

    @Column(nullable = false)
    private double total;

    public TotalVendidoMensal() {
    }

    public TotalVendidoMensal(String representanteId, String mes) {
        this.representanteId = representanteId;
        this.mes = mes;
    }

    public String getId() {
        return id;
    }

    public String getRepresentanteId() {
        return representanteId;
    }

    public String getMes() {
        return mes;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
