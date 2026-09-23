package com.almoxarifado.api.metarepresentante;

import com.almoxarifado.api.meta.Meta;
import com.almoxarifado.api.representante.Representante;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * O valor de uma meta atribuído a um representante num mês: quanto ele precisa bater e quanto já bateu.
 * Um registro por representante + meta + mês, já que a meta pode mudar de um mês pro outro.
 *
 * <p>Mora na tabela metas_representante_mensal; a antiga metas_representante (sem mês) é migrada pra
 * cá uma vez só por {@link MigracaoMetasPorMes}.
 */
@Entity
@Table(name = "metas_representante_mensal", uniqueConstraints = @UniqueConstraint(
        name = "uk_meta_representante_mes", columnNames = { "representante_id", "meta_id", "mes" }))
public class MetaRepresentante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull(message = "Informe o representante")
    @ManyToOne(optional = false)
    @JoinColumn(name = "representante_id", nullable = false)
    private Representante representante;

    @NotNull(message = "Informe a meta")
    @ManyToOne(optional = false)
    @JoinColumn(name = "meta_id", nullable = false)
    private Meta meta;

    /** Mês de referência, no formato "2026-09" (ver {@link Mes}). */
    @NotBlank(message = "Informe o mês")
    @Column(nullable = false, length = 7)
    private String mes;

    @DecimalMin(value = "0", message = "A meta não pode ser negativa")
    @Column(name = "valor_meta", nullable = false)
    private double valorMeta;

    @DecimalMin(value = "0", message = "O realizado não pode ser negativo")
    @Column(name = "valor_realizado", nullable = false)
    private double valorRealizado;

    /**
     * Só em metas KG: quanto os mesmos itens do realizado deram em R$ (valor do produto), pra tela
     * mostrar embaixo do nome da meta. Null nas outras unidades e antes da primeira sincronização.
     */
    @Column(name = "realizado_em_reais")
    private Double realizadoEmReais;

    public MetaRepresentante() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Representante getRepresentante() {
        return representante;
    }

    public void setRepresentante(Representante representante) {
        this.representante = representante;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public double getValorMeta() {
        return valorMeta;
    }

    public void setValorMeta(double valorMeta) {
        this.valorMeta = valorMeta;
    }

    public double getValorRealizado() {
        return valorRealizado;
    }

    public void setValorRealizado(double valorRealizado) {
        this.valorRealizado = valorRealizado;
    }

    public Double getRealizadoEmReais() {
        return realizadoEmReais;
    }

    public void setRealizadoEmReais(Double realizadoEmReais) {
        this.realizadoEmReais = realizadoEmReais;
    }
}
