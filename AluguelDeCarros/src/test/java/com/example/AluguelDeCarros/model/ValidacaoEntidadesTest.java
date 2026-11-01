package com.example.AluguelDeCarros.model;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ValidacaoEntidadesTest {
    @Test
    void validaFormatoCpfSemValidarDigitosVerificadores() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            Cliente formatoValido = new Cliente("usuario", "hash-ficticio", "RG", "00000000000", "Nome", "Rua", "Profissão");
            assertTrue(validator.validate(formatoValido).isEmpty());
            Cliente pontuado = new Cliente("usuario", "hash-ficticio", "RG", "123.456.789-01", "Nome", "Rua", "Profissão");
            assertTrue(validator.validate(pontuado).stream().anyMatch(v -> v.getPropertyPath().toString().equals("cpf")));
        }
    }

    @Test
    void rendimentoDeveSerPositivoECaberEmNumeric15_2() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String valor : new String[]{"0", "-1", "1.001", "10000000000000.00"}) {
                var vinculo = new VinculoRenda("Empresa", new BigDecimal(valor));
                assertTrue(validator.validateProperty(vinculo, "rendimento").size() > 0);
            }
            assertTrue(validator.validateProperty(new VinculoRenda("Empresa", new BigDecimal("0.01")), "rendimento").isEmpty());
            assertFalse(validator.validate(new VinculoRenda("Empresa", BigDecimal.ONE)).isEmpty());
        }
    }

    @Test
    void hashNaoApareceEmJsonOuToString() throws Exception {
        Cliente cliente = new Cliente("usuario", "hash-ficticio-secreto", "RG", "12345678901", "Nome", "Rua", "Profissão");
        cliente.adicionarVinculoRenda(new VinculoRenda("Empresa", BigDecimal.ONE));
        String json = JsonMapper.builder().build().writeValueAsString(cliente);
        assertFalse(json.contains("senhaHash"));
        assertFalse(json.contains("hash-ficticio-secreto"));
        assertFalse(cliente.toString().contains("hash-ficticio-secreto"));
    }
}
