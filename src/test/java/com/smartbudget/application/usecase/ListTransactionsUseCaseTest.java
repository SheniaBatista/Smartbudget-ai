package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.application.support.InMemoryTransactionRepository;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.smartbudget.application.support.TransactionFixtures.expense;
import static com.smartbudget.application.support.TransactionFixtures.income;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ListTransactionsUseCase")
class ListTransactionsUseCaseTest {
    private static final LocalDate TODAY = LocalDate.now();

    private InMemoryTransactionRepository repository;
    private ListTransactionsUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        useCase = new ListTransactionsUseCase(repository);

        repository.save(expense("Antiga", "10.00", TransactionCategory.OTHER, TODAY.minusDays(10)));
        repository.save(expense("Uber", "35.00", TransactionCategory.TRANSPORT, TODAY.minusDays(1)));
        repository.save(expense("Mercado", "120.00", TransactionCategory.FOOD, TODAY));
        repository.save(income("Salario", "5000.00", TransactionCategory.SALARY, TODAY));
    }

    @Test
    @DisplayName("lista da mais recente para a mais antiga")
    void ordersNewestFirst() {
        List<TransactionView> transactions = useCase.execute(ListTransactionsQuery.recent(10));

        assertThat(transactions).hasSize(4);
        assertThat(transactions.getLast().description()).isEqualTo("Antiga");
    }

    @Test
    @DisplayName("respeita o limite informado")
    void respectsLimit() {
        assertThat(useCase.execute(ListTransactionsQuery.recent(2))).hasSize(2);
    }

    @Test
    @DisplayName("limite invalido cai no padrao em vez de quebrar")
    void fallsBackToDefaultLimit() {
        ListTransactionsQuery query = new ListTransactionsQuery(null, null, null, 0);

        assertThat(query.limit()).isEqualTo(ListTransactionsQuery.DEFAULT_LIMIT);
    }

    @Test
    @DisplayName("limite acima do maximo e truncado")
    void capsLimit() {
        ListTransactionsQuery query = new ListTransactionsQuery(null, null, null, 5000);

        assertThat(query.limit()).isEqualTo(ListTransactionsQuery.MAX_LIMIT);
    }

    @Test
    @DisplayName("filtra por tipo")
    void filtersByType() {
        List<TransactionView> incomes =
                useCase.execute(new ListTransactionsQuery(null, TransactionType.INCOME, null, 10));

        assertThat(incomes).hasSize(1);
        assertThat(incomes.getFirst().description()).isEqualTo("Salario");
    }

    @Test
    @DisplayName("filtra por categoria")
    void filtersByCategory() {
        List<TransactionView> food =
                useCase.execute(new ListTransactionsQuery(null, null, TransactionCategory.FOOD, 10));

        assertThat(food).extracting(TransactionView::description).containsExactly("Mercado");
    }

    @Test
    @DisplayName("filtra por periodo")
    void filtersByPeriod() {
        List<TransactionView> recent = useCase.execute(
                new ListTransactionsQuery(DateRange.of(TODAY.minusDays(1), TODAY), null, null, 10));

        assertThat(recent).hasSize(3);
    }

    @Test
    @DisplayName("query nula usa o comportamento padrao")
    void nullQueryUsesDefaults() {
        assertThat(useCase.execute(null)).hasSize(4);
    }
}
