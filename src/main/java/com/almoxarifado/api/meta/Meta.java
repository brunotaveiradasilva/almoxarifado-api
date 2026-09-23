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
    // varchar em vez do enum(...) que o Hibernate cria sozinho no MySQL: senão cada unidade nova
    // exigiria alterar a coluna (bancos antigos são convertidos por MigracaoUnidadeMetaVarchar).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private UnidadeMeta unidade;

    /**
     * Código (ou códigos, separados por vírgula) da divisão correspondente na API da ADS —
     * soma só os itens vendidos nessa(s) divisão(ões). Vazio se essa meta usa
     * {@link #cnpjAdsFornecedor} ou não é sincronizada automaticamente.
     */
    private String codigoAdsDivisao;

    /**
     * CNPJ do fornecedor na API da ADS — alternativa a {@link #codigoAdsDivisao} pra metas
     * "catch-all" que somam tudo vendido desse fornecedor, não só uma divisão específica
     * (ex: uma meta "Geral"). Se preenchido, tem prioridade sobre codigoAdsDivisao.
     */
    private String cnpjAdsFornecedor;

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

    public String getCodigoAdsDivisao() {
        return codigoAdsDivisao;
    }

    public void setCodigoAdsDivisao(String codigoAdsDivisao) {
        this.codigoAdsDivisao = codigoAdsDivisao;
    }

    public String getCnpjAdsFornecedor() {
        return cnpjAdsFornecedor;
    }

    public void setCnpjAdsFornecedor(String cnpjAdsFornecedor) {
        this.cnpjAdsFornecedor = cnpjAdsFornecedor;
    }
}
