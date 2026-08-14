package com.smartbudget.infrastructure.persistence.repository;

import com.smartbudget.domain.model.CategoryExpense;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionId;
import com.smartbudget.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TransactionRepositoryAdapter.class)
@DisplayName("TransactionRepositoryAdapter - agregacoes em banco")
class TransactionRepositoryAdapterTest {
    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private TransactionRepositoryAdapter repository;

    @BeforeEach
    void seed() {
        repository.save(income("Salario", "5000.00", TransactionCategory.SALARY, TODAY.minusDays(5)));
        repository.save(expense("Supermercado", "500.00", TransactionCategory.FOOD, TODAY.minusDays(4)));
        repository.save(expense("Restaurante", "230.00", TransactionCategory.FOOD, TODAY.minusDays(2)));
        repository.save(expense("Uber", "410.00", TransactionCategory.TRANSPORT, TODAY.minusDays(1)));
        repository.save(expense("Cinema", "160.00", TransactionCategory.ENTERTAINMENT, TODAY));
        repository.save(expense("Muito antigo", "999.00", TransactionCategory.OTHER, TODAY.minusDays(60)));
    }

    @Test
    @DisplayName("persiste e recupera preservando valor, tipo, categoria e datas")
    void savesAndReadsBack() {
        Transaction saved = repository.save(
                expense("Farmacia", "45.90", TransactionCategory.HEALTH, TODAY));

        Optional<Transaction> found = repository.findById(saved.id());

        assertThat(found).isPresent();
        assertThat(found.get().description()).isEqualTo("Farmacia");
        assertThat(found.get().amount()).isEqualByComparingTo("45.90");
        assertThat(found.get().type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(found.get().category()).isEqualTo(TransactionCategory.HEALTH);
        assertThat(found.get().occurredAt()).isEqualTo(TODAY);
        assertThat(found.get().createdAt()).isNotNull();
    }

    @Test
    @DisplayName("soma despesas do periodo em SQL")
    void sumsExpensesInPeriod() {
        DateRange lastWeek = DateRange.of(TODAY.minusDays(7), TODAY);

        BigDecimal total = repository.sumAmount(TransactionType.EXPENSE, lastWeek);

        assertThat(total).isEqualByComparingTo("1300.00");
    }

    @Test
    @DisplayName("soma receitas do periodo em SQL")
    void sumsIncomeInPeriod() {
        DateRange lastWeek = DateRange.of(TODAY.minusDays(7), TODAY);

        assertThat(repository.sumAmount(TransactionType.INCOME, lastWeek)).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("periodo sem registros soma zero em vez de retornar nulo")
    void sumsZeroWhenEmpty() {
        DateRange future = DateRange.of(TODAY.minusDays(59), TODAY.minusDays(58));

        assertThat(repository.sumAmount(TransactionType.EXPENSE, future)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("agrupa despesas por categoria ordenadas do maior total para o menor")
    void groupsExpensesByCategory() {
        DateRange lastWeek = DateRange.of(TODAY.minusDays(7), TODAY);

        List<CategoryExpense> grouped = repository.sumExpensesGroupedByCategory(lastWeek);

        assertThat(grouped).extracting(CategoryExpense::category)
                .containsExactly(TransactionCategory.FOOD,
                        TransactionCategory.TRANSPORT,
                        TransactionCategory.ENTERTAINMENT);

        assertThat(grouped.getFirst().total()).isEqualByComparingTo("730.00");
        assertThat(grouped.getFirst().transactionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("agrupamento ignora receitas")
    void groupingIgnoresIncome() {
        List<CategoryExpense> grouped = repository.sumExpensesGroupedByCategory(DateRange.allTime());

        assertThat(grouped).extracting(CategoryExpense::category)
                .doesNotContain(TransactionCategory.SALARY);
    }

    @Test
    @DisplayName("encontra a maior despesa do periodo")
    void findsLargestExpense() {
        DateRange lastWeek = DateRange.of(TODAY.minusDays(7), TODAY);

        Optional<Transaction> largest = repository.findLargestExpense(lastWeek);

        assertThat(largest).isPresent();
        assertThat(largest.get().description()).isEqualTo("Supermercado");
        assertThat(largest.get().amount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("conta transacoes com e sem filtro de tipo")
    void countsTransactions() {
        DateRange lastWeek = DateRange.of(TODAY.minusDays(7), TODAY);

        assertThat(repository.count(lastWeek, null)).isEqualTo(5);
        assertThat(repository.count(lastWeek, TransactionType.EXPENSE)).isEqualTo(4);
        assertThat(repository.count(lastWeek, TransactionType.INCOME)).isEqualTo(1);
    }

    @Test
    @DisplayName("lista as mais recentes primeiro respeitando o limite")
    void listsRecent() {
        List<Transaction> recent = repository.findRecent(3);

        assertThat(recent).hasSize(3);
        assertThat(recent.getFirst().description()).isEqualTo("Cinema");
    }

    @Test
    @DisplayName("busca combina filtros de periodo, tipo e categoria")
    void searchCombinesFilters() {
        DateRange lastWeek = DateRange.of(TODAY.minusDays(7), TODAY);

        List<Transaction> food = repository.search(
                lastWeek, TransactionType.EXPENSE, TransactionCategory.FOOD, 10);

        assertThat(food).extracting(Transaction::description)
                .containsExactly("Restaurante", "Supermercado");
    }

    @Test
    @DisplayName("busca sem filtros opcionais devolve tudo do periodo")
    void searchWithoutOptionalFilters() {
        List<Transaction> all = repository.search(DateRange.allTime(), null, null, 100);

        assertThat(all).hasSize(6);
    }

    @Test
    @DisplayName("remove por identificador e sinaliza quando nao existia")
    void deletes() {
        Transaction saved = repository.save(expense("Temporaria", "1.00", TransactionCategory.OTHER, TODAY));

        assertThat(repository.deleteById(saved.id())).isTrue();
        assertThat(repository.findById(saved.id())).isEmpty();
        assertThat(repository.deleteById(TransactionId.generate())).isFalse();
    }

    private static Transaction expense(String description, String amount,
                                       TransactionCategory category, LocalDate date) {
        return Transaction.create(description, new BigDecimal(amount), TransactionType.EXPENSE, category, date);
    }

    private static Transaction income(String description, String amount,
                                      TransactionCategory category, LocalDate date) {
        return Transaction.create(description, new BigDecimal(amount), TransactionType.INCOME, category, date);
    }
}
