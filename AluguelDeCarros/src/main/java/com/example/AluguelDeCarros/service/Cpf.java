package com.example.AluguelDeCarros.service;

public final class Cpf {
    private Cpf() { }
    public static String normalizar(String valor) {
        if (valor == null || !valor.matches("[0-9.\\-]+")) throw new ApiException(400, "CPF inválido");
        String cpf = valor.replace(".", "").replace("-", "");
        if (!cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1)
            throw new ApiException(400, "CPF inválido");
        for (int n = 9; n <= 10; n++) {
            int soma = 0;
            for (int i = 0; i < n; i++) soma += (cpf.charAt(i) - '0') * (n + 1 - i);
            int digito = 11 - soma % 11;
            if (digito >= 10) digito = 0;
            if (cpf.charAt(n) - '0' != digito) throw new ApiException(400, "CPF inválido");
        }
        return cpf;
    }
}
