package com.almoxarifado.api.meta;

import com.almoxarifado.api.fornecedor.Fornecedor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Uma meta: pertence a um único fornecedor e é medida numa unidade fixa (kg, unidade ou R$). */
@Entity
@Table(name = "metas")
public class Meta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Informe o nome da meta")
    @Column(nullable = false)
    private String nome;

    @NotNull(message = "Informe o fornecedor")
    @ManyToOne(optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @NotNull(message = "Informe a unidade de medida")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnidadeMeta unidade;

    public Meta() {
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

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Fornecedor fornecedor) {
        this.fornecedor = fornecedor;
    }

    public UnidadeMeta getUnidade() {
        return unidade;
    }

    public void setUnidade(UnidadeMeta unidade) {
        this.unidade = unidade;
    }
}
