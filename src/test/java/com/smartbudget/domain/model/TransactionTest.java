package com.smartbudget.domain.model;

import com.smartbudget.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Transaction - invariantes do dominio")
class TransactionTest {
    @Test
    @DisplayName("cria uma despesa valida com identificador e data de criacao")
    void createsValidExpense() {
        Transaction transaction = Transaction.create(
                "Uber para o trabalho",
                new BigDecimal("85.00"),
                TransactionType.EXPENSE,
                TransactionCategory.TRANSPORT,
                LocalDate.of(2026, 8, 10));

        assertThat(transaction.id()).isNotNull();
        assertThat(transaction.description()).isEqualTo("Uber para o trabalho");
        assertThat(transaction.amount()).isEqualByComparingTo("85.00");
        assertThat(transaction.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(transaction.category()).isEqualTo(TransactionCategory.TRANSPORT);
        assertThat(transaction.occurredAt()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(transaction.createdAt()).isNotNull();
        assertThat(transaction.isExpense()).isTrue();
        assertThat(transaction.isIncome()).isFalse();
    }

    @Test
    @DisplayName("normaliza o valor para duas casas decimais")
    void normalizesAmountScale() {
        Transaction transaction = newTransaction(new BigDecimal("10.999"));

        assertThat(transaction.amount()).isEqualByComparingTo("11.00");
        assertThat(transaction.amount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("remove espacos em volta da descricao")
    void trimsDescription() {
        Transaction transaction = Transaction.create(
                "   Mercado   ",
                BigDecimal.TEN,
                TransactionType.EXPENSE,
                TransactionCategory.FOOD,
                null);

        assertThat(transaction.description()).isEqualTo("Mercado");
    }

    @Test
    @DisplayName("assume a data de hoje quando a data nao e informada")
    void defaultsToToday() {
        Transaction transaction = Transaction.create(
                "Cafe",
                BigDecimal.ONE,
                TransactionType.EXPENSE,
                TransactionCategory.FOOD,
                null);

        assertThat(transaction.occurredAt()).isEqualTo(LocalDate.now());
    }

    @ParameterizedTest(name = "valor {0} e rejeitado")
    @ValueSource(strings = {"0", "-1", "-0.01"})
    @DisplayName("rejeita valores menores ou iguais a zero")
    void rejectsNonPositiveAmount(String amount) {
        assertThatThrownBy(() -> newTransaction(new BigDecimal(amount)))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    @DisplayName("rejeita valor nulo")
    void rejectsNullAmount() {
        assertThatThrownBy(() -> newTransaction(null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("obrigatorio");
    }

    @Test
    @DisplayName("rejeita valor acima do limite suportado")
    void rejectsAmountAboveLimit() {
        assertThatThrownBy(() -> newTransaction(new BigDecimal("1000000000.00")))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("limite");
    }

    @ParameterizedTest(name = "descricao \"{0}\" e rejeitada")
    @ValueSource(strings = {"", "   "})
    @DisplayName("rejeita descricao vazia ou em branco")
    void rejectsBlankDescription(String description) {
        assertThatThrownBy(() -> Transaction.create(
                description, BigDecimal.TEN, TransactionType.EXPENSE, TransactionCategory.FOOD, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("descricao");
    }

    @Test
    @DisplayName("rejeita descricao acima de 255 caracteres")
    void rejectsTooLongDescription() {
        String longDescription = "a".repeat(Transaction.MAX_DESCRIPTION_LENGTH + 1);

        assertThatThrownBy(() -> Transaction.create(
                longDescription, BigDecimal.TEN, TransactionType.EXPENSE, TransactionCategory.FOOD, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("255");
    }

    @Test
    @DisplayName("rejeita tipo nulo")
    void rejectsNullType() {
        assertThatThrownBy(() -> Transaction.create(
                "Mercado", BigDecimal.TEN, null, TransactionCategory.FOOD, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("tipo");
    }

    @Test
    @DisplayName("rejeita categoria nula")
    void rejectsNullCategory() {
        assertThatThrownBy(() -> Transaction.create(
                "Mercado", BigDecimal.TEN, TransactionType.EXPENSE, null, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("categoria");
    }

    @Test
    @DisplayName("rejeita data futura")
    void rejectsFutureDate() {
        assertThatThrownBy(() -> Transaction.create(
                "Mercado", BigDecimal.TEN, TransactionType.EXPENSE, TransactionCategory.FOOD,
                LocalDate.now().plusDays(1)))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    @DisplayName("restore preserva identidade e data de criacao originais")
    void restoreKeepsIdentity() {
        TransactionId id = TransactionId.generate();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 5, 10, 30);

        Transaction transaction = Transaction.restore(
                id, "Salario", new BigDecimal("5000.00"), TransactionType.INCOME,
                TransactionCategory.SALARY, LocalDate.of(2026, 1, 5), createdAt);

        assertThat(transaction.id()).isEqualTo(id);
        assertThat(transaction.createdAt()).isEqualTo(createdAt);
        assertThat(transaction.isIncome()).isTrue();
    }

    @Test
    @DisplayName("duas transacoes sao iguais quando compartilham o identificador")
    void equalityIsBasedOnId() {
        TransactionId id = TransactionId.generate();
        LocalDateTime createdAt = LocalDateTime.now();

        Transaction first = Transaction.restore(id, "A", BigDecimal.ONE, TransactionType.EXPENSE,
                TransactionCategory.OTHER, LocalDate.now(), createdAt);
        Transaction second = Transaction.restore(id, "B", BigDecimal.TEN, TransactionType.INCOME,
                TransactionCategory.SALARY, LocalDate.now(), createdAt);

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    private Transaction newTransaction(BigDecimal amount) {
        return Transaction.create("Mercado", amount, TransactionType.EXPENSE, TransactionCategory.FOOD, null);
    }
}
