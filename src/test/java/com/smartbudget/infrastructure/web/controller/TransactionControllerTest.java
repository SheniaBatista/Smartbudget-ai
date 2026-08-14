package com.smartbudget.infrastructure.web.controller;

import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.application.usecase.CreateTransactionUseCase;
import com.smartbudget.application.usecase.DeleteTransactionUseCase;
import com.smartbudget.application.usecase.GetTransactionByIdUseCase;
import com.smartbudget.application.usecase.ListTransactionsQuery;
import com.smartbudget.application.usecase.ListTransactionsUseCase;
import com.smartbudget.domain.exception.TransactionNotFoundException;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionId;
import com.smartbudget.domain.model.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@DisplayName("TransactionController")
class TransactionControllerTest {
    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateTransactionUseCase createTransaction;

    @MockitoBean
    private ListTransactionsUseCase listTransactions;

    @MockitoBean
    private GetTransactionByIdUseCase getTransactionById;

    @MockitoBean
    private DeleteTransactionUseCase deleteTransaction;

    @Test
    @DisplayName("POST cria a transacao e devolve 201 com o cabecalho Location")
    void createsTransaction() throws Exception {
        given(createTransaction.execute(any())).willReturn(sampleView());

        String body = """
                {"description":"Supermercado","amount":120.00,"type":"EXPENSE","category":"FOOD","occurredAt":"%s"}
                """.formatted(LocalDate.now());

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/transactions/" + ID)))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.amount").value(120.00))
                .andExpect(jsonPath("$.categoryLabel").value("Alimentação"));
    }

    @Test
    @DisplayName("POST com valor zero devolve 400 e detalha o campo invalido")
    void rejectsZeroAmount() throws Exception {
        String body = """
                {"description":"Uber","amount":0,"type":"EXPENSE","category":"TRANSPORT"}
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0].field").value("amount"));
    }

    @Test
    @DisplayName("POST sem descricao devolve 400")
    void rejectsBlankDescription() throws Exception {
        String body = """
                {"description":"  ","amount":50,"type":"EXPENSE","category":"TRANSPORT"}
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("description"));
    }

    @Test
    @DisplayName("POST com corpo mal formatado devolve 400 sem stack trace")
    void rejectsMalformedBody() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isso nao e json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("GET lista as transacoes")
    void listsTransactions() throws Exception {
        given(listTransactions.execute(any(ListTransactionsQuery.class))).willReturn(List.of(sampleView()));

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Supermercado"));
    }

    @Test
    @DisplayName("GET por id inexistente devolve 404")
    void notFound() throws Exception {
        given(getTransactionById.execute(TransactionId.of(ID)))
                .willThrow(new TransactionNotFoundException(TransactionId.of(ID)));

        mockMvc.perform(get("/api/v1/transactions/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TRANSACTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE remove e devolve 204")
    void deletesTransaction() throws Exception {
        mockMvc.perform(delete("/api/v1/transactions/{id}", ID))
                .andExpect(status().isNoContent());

        verify(deleteTransaction).execute(TransactionId.of(ID));
    }

    @Test
    @DisplayName("DELETE de transacao inexistente devolve 404")
    void deleteNotFound() throws Exception {
        willThrow(new TransactionNotFoundException(TransactionId.of(ID)))
                .given(deleteTransaction).execute(TransactionId.of(ID));

        mockMvc.perform(delete("/api/v1/transactions/{id}", ID))
                .andExpect(status().isNotFound());
    }

    private TransactionView sampleView() {
        return new TransactionView(
                ID,
                "Supermercado",
                new BigDecimal("120.00"),
                TransactionType.EXPENSE,
                "Despesa",
                TransactionCategory.FOOD,
                "Alimentação",
                LocalDate.now(),
                LocalDateTime.now());
    }
}
