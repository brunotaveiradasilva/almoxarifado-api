package com.almoxarifado.api.material;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Um item que pode ser retirado do almoxarifado (mesmo formato do types.ts do front-end). */
@Entity
@Table(name = "materiais")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Informe o nome do material")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "Informe o código do material")
    @Column(nullable = false)
    private String codigo;

    /** Quantidade total que existe no almoxarifado. */
    @Min(value = 0, message = "O estoque não pode ser negativo")
    @Column(nullable = false)
    private int estoque;

    @Column(length = 2000)
    private String obs;

    public Material() {
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

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public int getEstoque() {
        return estoque;
    }

    public void setEstoque(int estoque) {
        this.estoque = estoque;
    }

    public String getObs() {
        return obs;
    }

    public void setObs(String obs) {
        this.obs = obs;
    }
}
