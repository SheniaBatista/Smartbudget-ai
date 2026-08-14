package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.CategoryExpenseView;
import com.smartbudget.domain.model.CategoryExpense;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GetExpensesByCategoryUseCase {
    private final TransactionRepository repository;

    public GetExpensesByCategoryUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoryExpenseView> execute(DateRange range) {
        DateRange effective = range != null ? range : DateRange.allTime();
        List<CategoryExpense> expenses = repository.sumExpensesGroupedByCategory(effective);
        BigDecimal total = totalOf(expenses);

        return expenses.stream().map(expense -> CategoryExpenseView.from(expense, total)).toList();
    }

    @Transactional(readOnly = true)
    public CategoryExpenseView execute(TransactionCategory category, DateRange range) {
        DateRange effective = range != null ? range : DateRange.allTime();
        List<CategoryExpense> expenses = repository.sumExpensesGroupedByCategory(effective);
        BigDecimal total = totalOf(expenses);

        return expenses.stream()
                .filter(expense -> expense.category() == category)
                .findFirst()
                .map(expense -> CategoryExpenseView.from(expense, total))
                .orElseGet(() -> CategoryExpenseView.from(new CategoryExpense(category, BigDecimal.ZERO, 0L), total));
    }

    private BigDecimal totalOf(List<CategoryExpense> expenses) {
        return expenses.stream()
                .map(CategoryExpense::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
