package com.tyler.sentinel.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.tyler.sentinel.dto.SecurityExplanation;
import com.tyler.sentinel.dto.SecurityFinding;
import com.tyler.sentinel.model.User;
import com.tyler.sentinel.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AIService {

    private final UserRepository userRepository;
    private final OpenAIClient client = OpenAIOkHttpClient.fromEnv();

    public AIService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public SecurityExplanation analyzeCode(SecurityFinding finding, String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String message = """
                Severity: %s
                Category: %s
                File Path: %s
                Line Number: %d
                Code Snippet:
                %s

                Scanner Description:
                %s
                """.formatted(
                finding.getSeverity(),
                finding.getCategory(),
                finding.getFilePath(),
                finding.getLineNumber(),
                finding.getCodeSnippet(),
                finding.getDescription()
        );

        StructuredChatCompletionCreateParams<SecurityExplanation> params =
                StructuredChatCompletionCreateParams.<SecurityExplanation>builder()
                        .addSystemMessage("""
                                You are a senior application security engineer.

                                Explain the supplied security finding clearly and accurately.

                                Rules:
                                1. Explain the vulnerability in simple language.
                                2. Explain why it is dangerous.
                                3. Explain how it could realistically be exploited.
                                4. Provide a practical recommended fix.
                                5. Never invent vulnerabilities that are not present in the finding.
                                6. Base your explanation only on the supplied security finding and code snippet.
                                7. If the finding lacks enough information, say additional context is required.
                                8. Risk rating must be from 0 to 100, where 100 is extremely unsafe and 0 is completely secure.
                                9. Do not use markdown code fences.
                                10. Do not mention that you are an AI model.
                                11. Keep the response concise.
                                """)
                        .addUserMessage(message)
                        .model(ChatModel.GPT_5_2)
                        .responseFormat(SecurityExplanation.class)
                        .build();

        return client.chat()
                .completions()
                .create(params)
                .choices()
                .get(0)
                .message()
                .content()
                .get();
    }

    
}
