package com.example.AluguelDeCarros.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "usuarios", schema = "public")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "usuario", nullable = false, unique = true, length = 100)
    private String usuario;

    @JsonIgnore
    @NotBlank
    @Size(max = 255)
    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    protected Usuario() { }

    // Recebe somente o hash produzido pela futura camada de autenticação.
    protected Usuario(String usuario, String senhaHash) {
        this.usuario = usuario;
        this.senhaHash = senhaHash;
    }

    @JsonIgnore
    public String getSenhaHash() { return senhaHash; }

    public Long getId() { return id; }
    public String getUsuario() { return usuario; }
    public boolean isAtivo() { return ativo; }
    public void desativar() { ativo = false; }
}
