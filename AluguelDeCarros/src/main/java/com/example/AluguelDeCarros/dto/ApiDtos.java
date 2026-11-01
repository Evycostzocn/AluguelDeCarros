package com.example.AluguelDeCarros.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.example.AluguelDeCarros.model.*;

public final class ApiDtos {
    private ApiDtos() { }
    public record Login(@NotBlank @Size(max=100) String usuario, @NotBlank @Size(max=72) String senha) {
        @Override public String toString() { return "Login[credenciais omitidas]"; }
    }
    public record Cadastro(@NotBlank @Pattern(regexp="[a-zA-Z0-9_.-]{3,100}") String usuario,
        @NotBlank @Size(min=8,max=72) String senha,
        @NotBlank @Size(max=30) String rg, @NotBlank @Size(max=14) String cpf,
        @NotBlank @Size(max=200) String nome, @NotBlank @Size(max=500) String endereco,
        @NotBlank @Size(max=150) String profissao) {
        @Override public String toString() { return "Cadastro[dados omitidos]"; }
    }
    public record DadosCliente(@NotBlank @Size(max=30) String rg, @NotBlank @Size(max=14) String cpf,
        @NotBlank @Size(max=200) String nome, @NotBlank @Size(max=500) String endereco,
        @NotBlank @Size(max=150) String profissao) { }
    public record RendaEntrada(@NotBlank @Size(max=200) String entidadeEmpregadora,
        @NotNull @Positive @Digits(integer=13,fraction=2) BigDecimal rendimento) { }
    public record PedidoEntrada(@NotNull @Positive Long automovelId, @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim) { }
    public record AvaliacaoEntrada(@NotBlank @Size(max=2000) String parecer, @NotNull Boolean aprovada) { }
    public record ContratoEntrada(@NotNull LocalDate dataCelebracao,
        @NotNull @Positive @Digits(integer=13,fraction=2) BigDecimal valor) { }
    public record CreditoEntrada(@NotNull @Positive @Digits(integer=13,fraction=2) BigDecimal valor,
        @NotNull @Positive Integer quantidadeParcelas) { }
    public record UsuarioSaida(Long id, String usuario, String perfil, boolean ativo) { }
    public record RendaSaida(Long id, String entidadeEmpregadora, BigDecimal rendimento) { }
    public record ClienteSaida(Long id, String usuario, String rg, String cpf, String nome,
        String endereco, String profissao, boolean ativo, List<RendaSaida> vinculosRenda) { }
    public record AutomovelSaida(Long id, String matricula, int ano, String marca, String modelo,
        String placa, String tipoProprietario, Long proprietarioId) { }
    public record AvaliacaoSaida(Long id, Long agenteId, LocalDateTime dataAvaliacao, String parecer, boolean aprovada) { }
    public record PedidoSaida(Long id, Long clienteId, AutomovelSaida automovel, LocalDateTime dataCriacao,
        LocalDate dataInicio, LocalDate dataFim, StatusPedido status, AvaliacaoSaida avaliacao, Long contratoId) { }
    public record CreditoSaida(Long id, Long bancoId, BigDecimal valor, Integer quantidadeParcelas) { }
    public record ContratoSaida(Long id, Long pedidoId, Long agenteId, LocalDate dataCelebracao,
        BigDecimal valor, CreditoSaida credito) { }
    public record Erro(int status, String mensagem) { }
    public record CsrfSaida(String headerName, String token) { }
}
