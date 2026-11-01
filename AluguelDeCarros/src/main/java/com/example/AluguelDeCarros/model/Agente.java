package com.example.AluguelDeCarros.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "agentes", schema = "public")
@PrimaryKeyJoinColumn(name = "id")
public class Agente extends Usuario {
    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "cnpj", nullable = false, length = 14)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoAgente tipo;

    protected Agente() { }

    public Agente(String usuario, String senhaHash, String nome, String cnpj, TipoAgente tipo) {
        super(usuario, senhaHash);
        this.nome = nome; this.cnpj = cnpj; this.tipo = tipo;
    }

    public String getNome() { return nome; }
    public String getCnpj() { return cnpj; }
    public TipoAgente getTipo() { return tipo; }
}
