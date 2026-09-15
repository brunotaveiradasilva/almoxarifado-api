package com.almoxarifado.api.tipometa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** Um tipo de meta que pode ser atribuído a vendedores (ex.: "Vendas", medida em "R$"). */
@Entity
@Table(name = "tipos_meta")
public class TipoMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Informe o nome do tipo de meta")
    @Column(nullable = false, unique = true)
    private String nome;

    @NotBlank(message = "Informe a unidade de medida")
    @Column(nullable = false)
    private String unidade;

    public TipoMeta() {
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

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }
}
