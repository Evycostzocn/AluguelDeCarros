package com.example.AluguelDeCarros.service;

public class ApiException extends RuntimeException {
    private final int status;
    public ApiException(int status, String mensagem) { super(mensagem); this.status = status; }
    public int getStatus() { return status; }
    public static ApiException naoEncontrado() { return new ApiException(404, "Recurso não encontrado"); }
}
