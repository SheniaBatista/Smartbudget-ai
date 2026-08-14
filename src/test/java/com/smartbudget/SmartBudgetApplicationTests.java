package com.smartbudget;

import com.smartbudget.infrastructure.ai.service.AssistantService;
import com.smartbudget.infrastructure.ai.service.VoiceAssistantService;
import com.smartbudget.infrastructure.ai.tools.FinanceTools;
import com.smartbudget.infrastructure.web.controller.AssistantController;
import com.smartbudget.infrastructure.web.controller.FinanceController;
import com.smartbudget.infrastructure.web.controller.TransactionController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Contexto da aplicacao")
class SmartBudgetApplicationTests {
    @Autowired
    private ChatClient financeChatClient;

    @Autowired
    private FinanceTools financeTools;

    @Autowired
    private AssistantService assistantService;

    @Autowired
    private VoiceAssistantService voiceAssistantService;

    @Autowired
    private TransactionController transactionController;

    @Autowired
    private FinanceController financeController;

    @Autowired
    private AssistantController assistantController;

    @Test
    @DisplayName("todos os componentes principais sao criados e injetados")
    void contextLoads() {
        assertThat(financeChatClient).isNotNull();
        assertThat(financeTools).isNotNull();
        assertThat(assistantService).isNotNull();
        assertThat(voiceAssistantService).isNotNull();
        assertThat(transactionController).isNotNull();
        assertThat(financeController).isNotNull();
        assertThat(assistantController).isNotNull();
    }
}
