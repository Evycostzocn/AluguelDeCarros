package com.example.AluguelDeCarros.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "clientes", schema = "public")
@PrimaryKeyJoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_clientes_usuarios"))
public class Cliente extends Usuario {

    @NotBlank
    @Size(max = 30)
    @Column(name = "rg", nullable = false, length = 30)
    private String rg;

    @NotBlank
    @Size(max = 11)
    @Pattern(regexp = "[0-9]{11}")
    @Column(name = "cpf", nullable = false, length = 11, unique = true)
    private String cpf;

    @NotBlank
    @Size(max = 200)
    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @NotBlank
    @Size(max = 500)
    @Column(name = "endereco", nullable = false, length = 500)
    private String endereco;

    @NotBlank
    @Size(max = 150)
    @Column(name = "profissao", nullable = false, length = 150)
    private String profissao;

    @Valid
    @Size(max = 3)
    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<VinculoRenda> vinculosRenda = new ArrayList<>();

    protected Cliente() { }

    public Cliente(String usuario, String senhaHash, String rg, String cpf,
                   String nome, String endereco, String profissao) {
        super(usuario, senhaHash);
        this.rg = rg;
        this.cpf = cpf;
        this.nome = nome;
        this.endereco = endereco;
        this.profissao = profissao;
    }

    public void atualizar(String rg, String cpf, String nome, String endereco, String profissao) {
        this.rg = rg; this.cpf = cpf; this.nome = nome;
        this.endereco = endereco; this.profissao = profissao;
    }

    public String getRg() { return rg; }
    public String getCpf() { return cpf; }
    public String getNome() { return nome; }
    public String getEndereco() { return endereco; }
    public String getProfissao() { return profissao; }

    public List<VinculoRenda> getVinculosRenda() {
        return Collections.unmodifiableList(vinculosRenda);
    }

    public void adicionarVinculoRenda(VinculoRenda vinculo) {
        Objects.requireNonNull(vinculo, "Vínculo obrigatório");
        if (vinculo.getCliente() != null && vinculo.getCliente() != this) {
            throw new IllegalArgumentException("Vínculo já pertence a outro cliente");
        }
        if (vinculosRenda.contains(vinculo)) {
            return;
        }
        if (vinculosRenda.size() >= 3) {
            throw new IllegalStateException("Cliente pode possuir no máximo três vínculos");
        }
        vinculo.associarA(this);
        vinculosRenda.add(vinculo);
    }

    public void removerVinculoRenda(VinculoRenda vinculo) {
        Objects.requireNonNull(vinculo, "Vínculo obrigatório");
        if (vinculosRenda.remove(vinculo)) {
            vinculo.desassociarDe(this);
        }
    }
}
