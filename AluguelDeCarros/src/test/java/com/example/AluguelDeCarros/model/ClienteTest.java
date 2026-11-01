package com.example.AluguelDeCarros.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {
    private Cliente cliente() {
        return new Cliente("cliente", "hash-ficticio-para-teste", "123", "12345678901",
                "Nome", "Endereço", "Profissão");
    }

    private VinculoRenda vinculo() {
        return new VinculoRenda("Empresa", new BigDecimal("1000.00"));
    }

    @Test
    void aceitaAteTresVinculosESincronizaAssociacao() {
        Cliente cliente = cliente();
        assertTrue(cliente.getVinculosRenda().isEmpty());
        for (int i = 0; i < 3; i++) {
            VinculoRenda vinculo = vinculo();
            cliente.adicionarVinculoRenda(vinculo);
            assertSame(cliente, vinculo.getCliente());
        }
        assertEquals(3, cliente.getVinculosRenda().size());
    }

    @Test
    void rejeitaQuartoSemAlterarNenhumLado() {
        Cliente cliente = cliente();
        for (int i = 0; i < 3; i++) cliente.adicionarVinculoRenda(vinculo());
        var anteriores = java.util.List.copyOf(cliente.getVinculosRenda());
        VinculoRenda quarto = vinculo();
        assertThrows(IllegalStateException.class, () -> cliente.adicionarVinculoRenda(quarto));
        assertEquals(anteriores, cliente.getVinculosRenda());
        assertNull(quarto.getCliente());
    }

    @Test
    void removeDosDoisLadosELiberaVaga() {
        Cliente cliente = cliente();
        VinculoRenda vinculo = vinculo();
        cliente.adicionarVinculoRenda(vinculo);
        cliente.adicionarVinculoRenda(vinculo());
        cliente.adicionarVinculoRenda(vinculo());
        cliente.removerVinculoRenda(vinculo);
        assertNull(vinculo.getCliente());
        assertFalse(cliente.getVinculosRenda().contains(vinculo));
        cliente.adicionarVinculoRenda(vinculo());
        assertEquals(3, cliente.getVinculosRenda().size());
    }

    @Test
    void naoPermiteCompartilharVinculoComOutroCliente() {
        Cliente primeiro = cliente();
        Cliente segundo = cliente();
        VinculoRenda vinculo = vinculo();
        primeiro.adicionarVinculoRenda(vinculo);
        assertThrows(IllegalArgumentException.class, () -> segundo.adicionarVinculoRenda(vinculo));
        assertSame(primeiro, vinculo.getCliente());
        assertEquals(java.util.List.of(vinculo), primeiro.getVinculosRenda());
        assertTrue(segundo.getVinculosRenda().isEmpty());
        segundo.removerVinculoRenda(vinculo);
        assertSame(primeiro, vinculo.getCliente());
    }

    @Test
    void adicionarMesmoVinculoNaoDuplica() {
        Cliente cliente = cliente();
        VinculoRenda vinculo = vinculo();
        cliente.adicionarVinculoRenda(vinculo);
        cliente.adicionarVinculoRenda(vinculo);
        assertEquals(1, cliente.getVinculosRenda().size());
    }

    @Test
    void colecaoNaoPodeSerAlteradaExternamente() {
        Cliente cliente = cliente();
        cliente.adicionarVinculoRenda(vinculo());
        assertThrows(UnsupportedOperationException.class, () -> cliente.getVinculosRenda().clear());
        assertThrows(UnsupportedOperationException.class, () -> cliente.getVinculosRenda().add(vinculo()));
    }

    @Test
    void rejeitaVinculoNulo() {
        Cliente cliente = cliente();
        assertThrows(NullPointerException.class, () -> cliente.adicionarVinculoRenda(null));
        assertTrue(cliente.getVinculosRenda().isEmpty());
    }

    @Test
    void contaIniciaAtivaEPodeSerDesativada() {
        Cliente cliente = cliente();
        assertTrue(cliente.isAtivo());
        cliente.desativar();
        assertFalse(cliente.isAtivo());
        assertEquals("cliente", cliente.getUsuario());
    }
}
