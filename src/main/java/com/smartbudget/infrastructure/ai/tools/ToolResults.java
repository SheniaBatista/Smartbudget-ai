package com.smartbudget.infrastructure.ai.tools;

import com.smartbudget.application.dto.BalanceView;
import com.smartbudget.application.dto.CategoryExpenseView;
import com.smartbudget.application.dto.MonthlySummaryView;
import com.smartbudget.application.dto.PeriodExpenseView;
import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.infrastructure.ai.support.BrazilianFormat;

import java.util.List;

public final class ToolResults {
    private ToolResults() {
    }

    public record Transaction(String id,
                              String description,
                              String amount,
                              String type,
                              String category,
                              String date) {
        public static Transaction from(TransactionView view) {
            return new Transaction(
                    view.id().toString(),
                    view.description(),
                    BrazilianFormat.money(view.amount()),
                    view.typeLabel(),
                    view.categoryLabel(),
                    BrazilianFormat.date(view.occurredAt()));
        }
    }

    public record TransactionList(int count, List<Transaction> transactions) {
        public static TransactionList from(List<TransactionView> views) {
            List<Transaction> items = views.stream().map(Transaction::from).toList();
            return new TransactionList(items.size(), items);
        }
    }

    public record Balance(String periodStart,
                          String periodEnd,
                          String totalIncome,
                          String totalExpense,
                          String netBalance) {
        public static Balance from(BalanceView view) {
            return new Balance(
                    BrazilianFormat.date(view.from()),
                    BrazilianFormat.date(view.to()),
                    BrazilianFormat.money(view.totalIncome()),
                    BrazilianFormat.money(view.totalExpense()),
                    BrazilianFormat.money(view.netBalance()));
        }
    }

    public record CategoryExpense(String category,
                                  String total,
                                  String shareOfExpenses,
                                  long transactionCount) {
        public static CategoryExpense from(CategoryExpenseView view) {
            return new CategoryExpense(
                    view.categoryLabel(),
                    BrazilianFormat.money(view.total()),
                    BrazilianFormat.percentage(view.percentageOfExpenses()),
                    view.transactionCount());
        }
    }

    public record PeriodExpense(String periodStart,
                                String periodEnd,
                                String totalExpense,
                                long expenseCount) {
        public static PeriodExpense from(PeriodExpenseView view) {
            return new PeriodExpense(
                    BrazilianFormat.date(view.from()),
                    BrazilianFormat.date(view.to()),
                    BrazilianFormat.money(view.totalExpense()),
                    view.transactionCount());
        }
    }

    public record MonthlySummary(String month,
                                 String totalIncome,
                                 String totalExpense,
                                 String netBalance,
                                 long transactionCount,
                                 Transaction largestExpense,
                                 List<CategoryExpense> expensesByCategory) {
        public static MonthlySummary from(MonthlySummaryView view) {
            return new MonthlySummary(
                    view.period(),
                    BrazilianFormat.money(view.totalIncome()),
                    BrazilianFormat.money(view.totalExpense()),
                    BrazilianFormat.money(view.netBalance()),
                    view.transactionCount(),
                    view.largestExpense() != null ? Transaction.from(view.largestExpense()) : null,
                    view.expensesByCategory().stream().map(CategoryExpense::from).toList());
        }
    }
}
