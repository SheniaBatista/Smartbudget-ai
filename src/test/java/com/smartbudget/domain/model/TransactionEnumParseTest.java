package com.smartbudget.domain.model;

import com.smartbudget.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Conversao textual de tipo e categoria")
class TransactionEnumParseTest {
    @ParameterizedTest(name = "\"{0}\" vira EXPENSE")
    @ValueSource(strings = {"EXPENSE", "expense", " Expense "})
    @DisplayName("aceita o tipo em qualquer caixa e com espacos")
    void parsesTypeLeniently(String value) {
        assertThat(TransactionType.parse(value)).isEqualTo(TransactionType.EXPENSE);
    }

    @ParameterizedTest(name = "\"{0}\" vira FOOD")
    @ValueSource(strings = {"FOOD", "food", " Food "})
    @DisplayName("aceita a categoria em qualquer caixa e com espacos")
    void parsesCategoryLeniently(String value) {
        assertThat(TransactionCategory.parse(value)).isEqualTo(TransactionCategory.FOOD);
    }

    @Test
    @DisplayName("tipo desconhecido lista os valores aceitos")
    void unknownTypeListsAccepted() {
        assertThatThrownBy(() -> TransactionType.parse("DESPESA"))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("INCOME")
                .hasMessageContaining("EXPENSE");
    }

    @Test
    @DisplayName("categoria desconhecida lista os valores aceitos")
    void unknownCategoryListsAccepted() {
        assertThatThrownBy(() -> TransactionCategory.parse("ALIMENTACAO"))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("FOOD")
                .hasMessageContaining("OTHER");
    }

    @Test
    @DisplayName("valores nulos ou vazios sao rejeitados com mensagem clara")
    void rejectsBlank() {
        assertThatThrownBy(() -> TransactionType.parse(null))
                .isInstanceOf(InvalidTransactionException.class);
        assertThatThrownBy(() -> TransactionCategory.parse("  "))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    @DisplayName("todas as categorias expoem rotulo em portugues")
    void everyCategoryHasLabel() {
        for (TransactionCategory category : TransactionCategory.values()) {
            assertThat(category.label()).isNotBlank();
        }
        assertThat(TransactionCategory.FOOD.label()).isEqualTo("Alimentação");
        assertThat(TransactionType.INCOME.label()).isEqualTo("Receita");
    }
}
