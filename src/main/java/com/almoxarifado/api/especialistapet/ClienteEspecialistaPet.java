package com.almoxarifado.api.especialistapet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Um cliente participante da campanha Especialista Pet (PremieR) num mês: as duas metas em kg que
 * vêm da planilha da PremieR (produto foco e todos os SKUs) e o realizado de cada uma, que vem da
 * sincronização com a ADS (ver AdsEspecialistaPetService).
 *
 * <p>O representante é só o nome que veio na planilha (coluna VENDEDOR), sem ligar com o cadastro
 * de representantes — a planilha usa o nome completo e às vezes uma rede ("REDE DOG IN BOX").
 */
@Entity
@Table(name = "especialista_pet_clientes", uniqueConstraints = @UniqueConstraint(
        name = "uk_especialista_pet_cliente_mes", columnNames = { "mes", "codigo_cliente" }))
public class ClienteEspecialistaPet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Mês de referência, no formato "2026-09". */
    @Column(nullable = false, length = 7)
    private String mes;

    /** Id do cliente na ADS (coluna CÓDIGO da planilha). */
    @Column(name = "codigo_cliente", nullable = false, length = 20)
    private String codigoCliente;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String representante;

    /** "NUMÉRICA" ou "PONDERADA", como vem da planilha — define a faixa de desconto. */
    @Column(nullable = false, length = 20)
    private String classificacao;

    @Column(name = "meta_foco", nullable = false)
    private double metaFoco;

    @Column(name = "meta_total", nullable = false)
    private double metaTotal;

    @Column(name = "realizado_foco", nullable = false)
    private double realizadoFoco;

    @Column(name = "realizado_total", nullable = false)
    private double realizadoTotal;

    /** Todos os SKUs em R$, a preço de tabela — é a base do desconto conquistado. */
    @Column(name = "realizado_reais", nullable = false)
    private double realizadoReais;

    /**
     * Só o produto foco em R$, a preço de tabela (parte de realizadoReais). O desconto é separado: o
     * foco só ganha desconto se o cliente bateu a meta de foco; o resto, pela faixa da campanha.
     * O default 0 deixa a coluna ser criada em cima dos clientes já importados.
     */
    @Column(name = "realizado_foco_reais", nullable = false, columnDefinition = "double precision default 0")
    private double realizadoFocoReais;

    /** Só a linha NATTU WILD em kg (parte de realizadoFoco) — o resto do foco é "sem WILD". */
    @Column(name = "realizado_foco_wild", nullable = false, columnDefinition = "double precision default 0")
    private double realizadoFocoWild;

    public ClienteEspecialistaPet() {
    }

    public double getRealizadoFocoWild() {
        return realizadoFocoWild;
    }

    public void setRealizadoFocoWild(double realizadoFocoWild) {
        this.realizadoFocoWild = realizadoFocoWild;
    }

    public double getRealizadoFocoReais() {
        return realizadoFocoReais;
    }

    public void setRealizadoFocoReais(double realizadoFocoReais) {
        this.realizadoFocoReais = realizadoFocoReais;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public String getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(String codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getRepresentante() {
        return representante;
    }

    public void setRepresentante(String representante) {
        this.representante = representante;
    }

    public String getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(String classificacao) {
        this.classificacao = classificacao;
    }

    public double getMetaFoco() {
        return metaFoco;
    }

    public void setMetaFoco(double metaFoco) {
        this.metaFoco = metaFoco;
    }

    public double getMetaTotal() {
        return metaTotal;
    }

    public void setMetaTotal(double metaTotal) {
        this.metaTotal = metaTotal;
    }

    public double getRealizadoFoco() {
        return realizadoFoco;
    }

    public void setRealizadoFoco(double realizadoFoco) {
        this.realizadoFoco = realizadoFoco;
    }

    public double getRealizadoTotal() {
        return realizadoTotal;
    }

    public void setRealizadoTotal(double realizadoTotal) {
        this.realizadoTotal = realizadoTotal;
    }

    public double getRealizadoReais() {
        return realizadoReais;
    }

    public void setRealizadoReais(double realizadoReais) {
        this.realizadoReais = realizadoReais;
    }
}
