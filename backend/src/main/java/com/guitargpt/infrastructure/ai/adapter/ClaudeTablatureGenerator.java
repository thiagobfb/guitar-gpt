package com.guitargpt.infrastructure.ai.adapter;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.guitargpt.domain.port.out.TablatureGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClaudeTablatureGenerator implements TablatureGenerator {

    private static final String SYSTEM_PROMPT = """
            Você é o GuitarGPT, um tutor de violão e guitarra que gera tablaturas em ASCII.

            Formato obrigatório da resposta:
            1. Cabeçalho com: título da peça, afinação (default: E A D G B E), BPM e tom.
            2. Tablatura em ASCII com 6 cordas no padrão (de baixo para cima, e B G D A E):
               e|---0---1---0---|
               B|---1---1---1---|
               G|---0---2---0---|
               D|---2---3---2---|
               A|---3---3---3---|
               E|---x---1---x---|
            3. Legenda das técnicas usadas: h (hammer-on), p (pull-off), b (bend), ~ (vibrato),
               / (slide up), \\ (slide down), x (muted), () (ghost note).
            4. Breve explicação didática em português ao final (2-4 frases) com dica de execução.

            Restrições:
            - Respeite a escala/tom requisitados pelo usuário.
            - Não invente afinações não-padrão sem alertar explicitamente.
            - Se o pedido for ambíguo, escolha a interpretação mais simples e indique a escolha.
            - Responda em português.
            """;

    private final AnthropicClient client;
    private final String model;
    private final long maxTokens;

    public ClaudeTablatureGenerator(
            AnthropicClient client,
            @Value("${anthropic.model}") String model,
            @Value("${anthropic.max-tokens}") long maxTokens) {
        this.client = client;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    public String generate(String userPrompt) {
        log.info("Calling Claude API: model={}, maxTokens={}, promptLength={}",
                model, maxTokens, userPrompt.length());

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(SYSTEM_PROMPT)
                .addUserMessage(userPrompt)
                .build();

        Message response = client.messages().create(params);

        String result = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .reduce("", (a, b) -> a + b);

        log.info("Claude response received: inputTokens={}, outputTokens={}, resultLength={}",
                response.usage().inputTokens(), response.usage().outputTokens(), result.length());

        return result;
    }
}
