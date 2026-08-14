package com.smartbudget.infrastructure.ai.tools;

import com.smartbudget.application.dto.CreateTransactionCommand;
import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.application.usecase.CreateTransactionUseCase;
import com.smartbudget.application.usecase.GetBalanceUseCase;
import com.smartbudget.application.usecase.GetExpensesByCategoryUseCase;
import com.smartbudget.application.usecase.GetExpensesByPeriodUseCase;
import com.smartbudget.application.usecase.GetLargestExpenseUseCase;
import com.smartbudget.application.usecase.GetMonthlySummaryUseCase;
import com.smartbudget.application.usecase.ListTransactionsQuery;
import com.smartbudget.application.usecase.ListTransactionsUseCase;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionType;
import com.smartbudget.infrastructure.ai.support.ToolArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Component
public class FinanceTools {
    private static final Logger log = LoggerFactory.getLogger(FinanceTools.class);

    private final CreateTransactionUseCase createTransaction;
    private final ListTransactionsUseCase listTransactions;
    private final GetBalanceUseCase getBalance;
    private final GetExpensesByCategoryUseCase getExpensesByCategory;
    private final GetExpensesByPeriodUseCase getExpensesByPeriod;
    private final GetMonthlySummaryUseCase getMonthlySummary;
    private final GetLargestExpenseUseCase getLargestExpense;

    public FinanceTools(CreateTransactionUseCase createTransaction,
                        ListTransactionsUseCase listTransactions,
                        GetBalanceUseCase getBalance,
                        GetExpensesByCategoryUseCase getExpensesByCategory,
                        GetExpensesByPeriodUseCase getExpensesByPeriod,
                        GetMonthlySummaryUseCase getMonthlySummary,
                        GetLargestExpenseUseCase getLargestExpense) {
        this.createTransaction = createTransaction;
        this.listTransactions = listTransactions;
        this.getBalance = getBalance;
        this.getExpensesByCategory = getExpensesByCategory;
        this.getExpensesByPeriod = getExpensesByPeriod;
        this.getMonthlySummary = getMonthlySummary;
        this.getLargestExpense = getLargestExpense;
    }

    @Tool(name = "createTransaction", description = """
            Registra uma nova transacao financeira (receita ou despesa) no banco de dados do usuario \
            e retorna o registro efetivamente gravado. Use sempre que o usuario disser que gastou, \
            pagou, comprou, assinou, recebeu ou ganhou dinheiro. Nunca chame esta ferramenta sem \
            saber o valor: se o valor nao foi informado, pergunte antes.""")
    public ToolResults.Transaction createTransaction(

            @ToolParam(description = "Descricao curta do que foi pago ou recebido, por exemplo 'Uber', 'supermercado' ou 'salario'")
            String description,

            @ToolParam(description = "Valor em reais, sempre positivo, com ate duas casas decimais. Exemplo: 85.50")
            BigDecimal amount,

            @ToolParam(description = "INCOME quando o dinheiro entrou, EXPENSE quando o dinheiro saiu")
            String type,

            @ToolParam(description = "Uma destas categorias: FOOD, TRANSPORT, HOUSING, HEALTH, EDUCATION, ENTERTAINMENT, SHOPPING, SALARY, INVESTMENT, OTHER")
            String category,

            @ToolParam(required = false, description = "Data do gasto no formato AAAA-MM-DD. Omita para usar a data de hoje.")
            String occurredAt) {
        log.info("Tool selected: createTransaction");

        TransactionType parsedType = TransactionType.parse(type);
        TransactionCategory parsedCategory = TransactionCategory.parse(category);
        LocalDate parsedDate = ToolArguments.optionalDate(occurredAt, "a data da transacao");

        TransactionView created = createTransaction.execute(
                new CreateTransactionCommand(description, amount, parsedType, parsedCategory, parsedDate));

        return ToolResults.Transaction.from(created);
    }

    @Tool(name = "getBalance", description = """
            Consulta o saldo financeiro real do usuario: total de receitas, total de despesas e \
            saldo liquido do periodo. Sem datas, considera todo o historico. Use para perguntas \
            como qual e o meu saldo, quanto eu tenho ou quanto sobrou.""")
    public ToolResults.Balance getBalance(

            @ToolParam(required = false, description = "Data inicial do periodo no formato AAAA-MM-DD. Omita para considerar todo o historico.")
            String from,

            @ToolParam(required = false, description = "Data final do periodo no formato AAAA-MM-DD. Omita para considerar ate hoje.")
            String to) {
        log.info("Tool selected: getBalance");
        return ToolResults.Balance.from(getBalance.execute(ToolArguments.optionalRange(from, to)));
    }

    @Tool(name = "listTransactions", description = """
            Lista as transacoes ja registradas, da mais recente para a mais antiga, com filtros \
            opcionais de periodo, tipo e categoria. Use para pedidos de extrato, historico, \
            ultimas transacoes ou ultimos gastos.""")
    public ToolResults.TransactionList listTransactions(

            @ToolParam(required = false, description = "Quantidade maxima de transacoes a retornar. Padrao 20, maximo 200.")
            Integer limit,

            @ToolParam(required = false, description = "Filtra por INCOME ou EXPENSE. Omita para trazer os dois tipos.")
            String type,

            @ToolParam(required = false, description = "Filtra por categoria: FOOD, TRANSPORT, HOUSING, HEALTH, EDUCATION, ENTERTAINMENT, SHOPPING, SALARY, INVESTMENT, OTHER")
            String category,

            @ToolParam(required = false, description = "Data inicial no formato AAAA-MM-DD")
            String from,

            @ToolParam(required = false, description = "Data final no formato AAAA-MM-DD")
            String to) {
        log.info("Tool selected: listTransactions");

        ListTransactionsQuery query = new ListTransactionsQuery(
                ToolArguments.optionalRange(from, to),
                type != null && !type.isBlank() ? TransactionType.parse(type) : null,
                category != null && !category.isBlank() ? TransactionCategory.parse(category) : null,
                limit != null ? limit : ListTransactionsQuery.DEFAULT_LIMIT);

        return ToolResults.TransactionList.from(listTransactions.execute(query));
    }

    @Tool(name = "getExpensesByCategory", description = """
            Consulta despesas agrupadas por categoria em um periodo. Informe a categoria para saber \
            quanto foi gasto especificamente nela; omita a categoria para receber a distribuicao \
            completa e descobrir onde o usuario mais gastou.""")
    public List<ToolResults.CategoryExpense> getExpensesByCategory(

            @ToolParam(required = false, description = "Categoria consultada: FOOD, TRANSPORT, HOUSING, HEALTH, EDUCATION, ENTERTAINMENT, SHOPPING, SALARY, INVESTMENT, OTHER. Omita para receber todas.")
            String category,

            @ToolParam(required = false, description = "Data inicial no formato AAAA-MM-DD. Omita para considerar todo o historico.")
            String from,

            @ToolParam(required = false, description = "Data final no formato AAAA-MM-DD")
            String to) {
        log.info("Tool selected: getExpensesByCategory");

        DateRange range = ToolArguments.optionalRange(from, to);

        if (category == null || category.isBlank()) {
            return getExpensesByCategory.execute(range).stream()
                    .map(ToolResults.CategoryExpense::from)
                    .toList();
        }

        TransactionCategory parsedCategory = TransactionCategory.parse(category);
        return List.of(ToolResults.CategoryExpense.from(getExpensesByCategory.execute(parsedCategory, range)));
    }

    @Tool(name = "getExpensesByPeriod", description = """
            Consulta o total gasto em um intervalo de datas, junto com a quantidade de despesas. \
            Use para perguntas como quanto gastei hoje, quanto gastei ontem ou quanto gastei nesta semana. \
            Para um unico dia, informe a mesma data em ambos os parametros.""")
    public ToolResults.PeriodExpense getExpensesByPeriod(

            @ToolParam(description = "Data inicial do intervalo no formato AAAA-MM-DD")
            String from,

            @ToolParam(description = "Data final do intervalo no formato AAAA-MM-DD")
            String to) {
        log.info("Tool selected: getExpensesByPeriod");
        return ToolResults.PeriodExpense.from(getExpensesByPeriod.execute(ToolArguments.optionalRange(from, to)));
    }

    @Tool(name = "getMonthlySummary", description = """
            Gera o resumo financeiro completo de um mes: total de receitas, total de despesas, saldo, \
            quantidade de transacoes, maior despesa e distribuicao dos gastos por categoria. Use para \
            pedidos de resumo, panorama, balanco ou analise do mes. Todos os valores ja vem calculados.""")
    public ToolResults.MonthlySummary getMonthlySummary(

            @ToolParam(required = false, description = "Mes de referencia no formato AAAA-MM, por exemplo 2026-08. Omita para usar o mes atual.")
            String month) {
        log.info("Tool selected: getMonthlySummary");

        YearMonth reference = ToolArguments.monthOrCurrent(month);
        return ToolResults.MonthlySummary.from(getMonthlySummary.execute(reference));
    }

    @Tool(name = "getLargestExpense", description = """
            Consulta a maior despesa individual registrada em um periodo. Use para perguntas como \
            qual foi o meu maior gasto ou qual foi a compra mais cara. Retorna vazio quando nao ha \
            despesas no periodo.""")
    public List<ToolResults.Transaction> getLargestExpense(

            @ToolParam(required = false, description = "Data inicial no formato AAAA-MM-DD. Omita para considerar todo o historico.")
            String from,

            @ToolParam(required = false, description = "Data final no formato AAAA-MM-DD")
            String to) {
        log.info("Tool selected: getLargestExpense");

        return getLargestExpense.execute(ToolArguments.optionalRange(from, to))
                .map(ToolResults.Transaction::from)
                .map(List::of)
                .orElseGet(List::of);
    }
}
