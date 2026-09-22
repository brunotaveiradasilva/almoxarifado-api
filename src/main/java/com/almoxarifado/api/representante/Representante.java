package com.almoxarifado.api.representante;

import java.util.LinkedHashSet;
import java.util.Set;

import com.almoxarifado.api.fornecedor.Fornecedor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** Um representante, que pode trabalhar para mais de um fornecedor. */
@Entity
@Table(name = "representantes")
public class Representante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Informe o nome do representante")
    @Column(nullable = false)
    private String nome;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "representante_fornecedor",
            joinColumns = @JoinColumn(name = "representante_id"),
            inverseJoinColumns = @JoinColumn(name = "fornecedor_id"))
    private Set<Fornecedor> fornecedores = new LinkedHashSet<>();

    private String email;

    private String celular;

    /** Código desse representante na API da ADS (histórico de vendas) — vazio se ele não é sincronizado automaticamente. */
    private String codigoAds;

    /**
     * Total vendido em R$ por esse representante no mês corrente, somando TODOS os fornecedores
     * e divisões (não só o que está mapeado em alguma meta) — atualizado junto com o
     * valorRealizado das metas, em AdsSincronizacaoService. Null se ele nunca foi sincronizado.
     */
    private Double totalVendidoAds;

    public Representante() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Set<Fornecedor> getFornecedores() {
        return fornecedores;
    }

    public void setFornecedores(Set<Fornecedor> fornecedores) {
        this.fornecedores = fornecedores;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getCodigoAds() {
        return codigoAds;
    }

    public void setCodigoAds(String codigoAds) {
        this.codigoAds = codigoAds;
    }

    public Double getTotalVendidoAds() {
        return totalVendidoAds;
    }

    public void setTotalVendidoAds(Double totalVendidoAds) {
        this.totalVendidoAds = totalVendidoAds;
    }
}
