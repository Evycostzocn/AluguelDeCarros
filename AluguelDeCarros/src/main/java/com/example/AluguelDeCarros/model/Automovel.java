package com.example.AluguelDeCarros.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "automoveis", schema = "public")
public class Automovel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "matricula", nullable = false, unique = true, length = 50)
    private String matricula;

    @Column(name = "ano", nullable = false)
    private int ano;

    @Column(name = "marca", nullable = false, length = 100)
    private String marca;

    @Column(name = "modelo", nullable = false, length = 100)
    private String modelo;

    @Column(name = "placa", nullable = false, unique = true, length = 10)
    private String placa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietario_cliente_id")
    private Cliente proprietarioCliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietario_agente_id")
    private Agente proprietarioAgente;

    protected Automovel() { }

    public Automovel(String matricula, int ano, String marca, String modelo, String placa,
                     Cliente cliente, Agente agente) {
        if ((cliente == null) == (agente == null)) throw new IllegalArgumentException("Informe exatamente um proprietário");
        this.matricula = matricula; this.ano = ano; this.marca = marca;
        this.modelo = modelo; this.placa = placa;
        this.proprietarioCliente = cliente; this.proprietarioAgente = agente;
    }

    public Long getId() { return id; }
    public String getMatricula() { return matricula; }
    public int getAno() { return ano; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getPlaca() { return placa; }
    public Cliente getProprietarioCliente() { return proprietarioCliente; }
    public Agente getProprietarioAgente() { return proprietarioAgente; }
}
