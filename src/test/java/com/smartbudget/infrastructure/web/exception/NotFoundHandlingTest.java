package com.smartbudget.infrastructure.web.exception;

import com.smartbudget.application.usecase.CreateTransactionUseCase;
import com.smartbudget.application.usecase.DeleteTransactionUseCase;
import com.smartbudget.application.usecase.GetTransactionByIdUseCase;
import com.smartbudget.application.usecase.ListTransactionsUseCase;
import com.smartbudget.infrastructure.web.controller.TransactionController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@DisplayName("Tratamento de rota inexistente")
class NotFoundHandlingTest {
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
    @DisplayName("rota de API inexistente devolve 404, nao 500")
    void unknownApiRouteReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("recurso estatico inexistente devolve 404, nao 500")
    void unknownStaticResourceReturns404() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("a resposta de erro nunca expoe stack trace")
    void errorResponseNeverLeaksStackTrace() throws Exception {
        mockMvc.perform(get("/api/v1/rota-que-nao-existe"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
