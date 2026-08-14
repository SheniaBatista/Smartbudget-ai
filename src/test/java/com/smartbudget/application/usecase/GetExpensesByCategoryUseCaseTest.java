package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.CategoryExpenseView;
import com.smartbudget.application.support.InMemoryTransactionRepository;
import com.smartbudget.domain.model.TransactionCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.smartbudget.application.support.TransactionFixtures.expense;
import static com.smartbudget.application.support.TransactionFixtures.income;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetExpensesByCategoryUseCase")
class GetExpensesByCategoryUseCaseTest {
    private static final LocalDate TODAY = LocalDate.now();

    private InMemoryTransactionRepository repository;
    private GetExpensesByCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        useCase = new GetExpensesByCategoryUseCase(repository);

        repository.save(expense("Supermercado", "500.00", TransactionCategory.FOOD, TODAY));
        repository.save(expense("Restaurante", "230.00", TransactionCategory.FOOD, TODAY));
        repository.save(expense("Uber", "270.00", TransactionCategory.TRANSPORT, TODAY));
        repository.save(income("Salario", "5000.00", TransactionCategory.SALARY, TODAY));
    }

    @Test
    @DisplayName("distribuicao completa vem ordenada do maior gasto para o menor")
    void returnsOrderedBreakdown() {
        List<CategoryExpenseView> breakdown = useCase.execute(null);

        assertThat(breakdown).extracting(CategoryExpenseView::category)
                .containsExactly(TransactionCategory.FOOD, TransactionCategory.TRANSPORT);
        assertThat(breakdown.getFirst().total()).isEqualByComparingTo("730.00");
    }

    @Test
    @DisplayName("receitas nao entram na distribuicao de despesas")
    void ignoresIncome() {
        List<CategoryExpenseView> breakdown = useCase.execute(null);

        assertThat(breakdown).extracting(CategoryExpenseView::category)
                .doesNotContain(TransactionCategory.SALARY);
    }

    @Test
    @DisplayName("consulta de uma categoria devolve total, contagem e percentual")
    void returnsSingleCategory() {
        CategoryExpenseView food = useCase.execute(TransactionCategory.FOOD, null);

        assertThat(food.total()).isEqualByComparingTo("730.00");
        assertThat(food.transactionCount()).isEqualTo(2);
        assertThat(food.categoryLabel()).isEqualTo("Alimentação");
        assertThat(food.percentageOfExpenses()).isEqualByComparingTo("73.00");
    }

    @Test
    @DisplayName("categoria sem gastos devolve zero em vez de erro")
    void unusedCategoryReturnsZero() {
        CategoryExpenseView health = useCase.execute(TransactionCategory.HEALTH, null);

        assertThat(health.total()).isEqualByComparingTo("0.00");
        assertThat(health.transactionCount()).isZero();
    }
}
