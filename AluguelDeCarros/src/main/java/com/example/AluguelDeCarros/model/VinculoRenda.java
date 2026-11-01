package com.example.AluguelDeCarros.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "vinculos_renda", schema = "public")
public class VinculoRenda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "entidade_empregadora", nullable = false, length = 200)
    private String entidadeEmpregadora;

    @NotNull
    @Positive
    @Digits(integer = 13, fraction = 2)
    @Column(name = "rendimento", nullable = false, precision = 15, scale = 2)
    private BigDecimal rendimento;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vinculos_renda_clientes"))
    private Cliente cliente;

    protected VinculoRenda() { }

    public VinculoRenda(String entidadeEmpregadora, BigDecimal rendimento) {
        this.entidadeEmpregadora = entidadeEmpregadora;
        this.rendimento = rendimento;
    }

    public void atualizar(String entidadeEmpregadora, BigDecimal rendimento) {
        this.entidadeEmpregadora = entidadeEmpregadora;
        this.rendimento = rendimento;
    }

    public Long getId() { return id; }
    public String getEntidadeEmpregadora() { return entidadeEmpregadora; }
    public BigDecimal getRendimento() { return rendimento; }
    public Cliente getCliente() { return cliente; }

    // Uso interno: a API pública da associação fica em Cliente.
    void associarA(Cliente cliente) {
        Objects.requireNonNull(cliente, "Cliente obrigatório");
        if (this.cliente != null && this.cliente != cliente) {
            throw new IllegalArgumentException("Vínculo já pertence a outro cliente");
        }
        this.cliente = cliente;
    }

    void desassociarDe(Cliente cliente) {
        if (this.cliente != cliente) {
            throw new IllegalArgumentException("Vínculo não pertence a este cliente");
        }
        this.cliente = null;
    }
}
