package com.smartbudget.domain.model;

import com.smartbudget.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransactionId")
class TransactionIdTest {
    @Test
    @DisplayName("gera identificadores distintos")
    void generatesDistinctIds() {
        assertThat(TransactionId.generate()).isNotEqualTo(TransactionId.generate());
    }

    @Test
    @DisplayName("igualdade e baseada no valor, nao na instancia")
    void equalityIsByValue() {
        UUID uuid = UUID.randomUUID();

        assertThat(TransactionId.of(uuid))
                .isEqualTo(TransactionId.of(uuid))
                .hasSameHashCodeAs(TransactionId.of(uuid));
    }

    @Test
    @DisplayName("converte texto valido, tolerando espacos")
    void parsesText() {
        UUID uuid = UUID.randomUUID();

        assertThat(TransactionId.of(uuid.toString())).isEqualTo(TransactionId.of(uuid));
        assertThat(TransactionId.of("  " + uuid + "  ")).isEqualTo(TransactionId.of(uuid));
    }

    @Test
    @DisplayName("toString devolve o UUID puro, sem envelope do record")
    void printsRawUuid() {
        UUID uuid = UUID.randomUUID();

        assertThat(TransactionId.of(uuid)).hasToString(uuid.toString());
    }

    @Test
    @DisplayName("texto invalido e recusado com mensagem que orienta a correcao")
    void rejectsInvalidText() {
        assertThatThrownBy(() -> TransactionId.of("nao-e-um-uuid"))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("UUID");
    }

    @Test
    @DisplayName("texto ausente e recusado")
    void rejectsBlankText() {
        assertThatThrownBy(() -> TransactionId.of((String) null))
                .isInstanceOf(InvalidTransactionException.class);
        assertThatThrownBy(() -> TransactionId.of("   "))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    @DisplayName("UUID nulo e recusado")
    void rejectsNullUuid() {
        assertThatThrownBy(() -> TransactionId.of((UUID) null))
                .isInstanceOf(NullPointerException.class);
    }
}
