package com.example.dormmate.ui;

import com.example.dormmate.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.TextPart;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.concurrent.Executors;

public class ChatbotManager {

    private ChatFutures chatFutures;

    public ChatbotManager(String context) {
        String apiKey = BuildConfig.GEMINI_API_KEY;

        // Construct the strict system instruction [cite: 185-186, 460]
        String systemInstructionText = "You are the DormMate Hostel Assistant. " +
                "Answer questions using ONLY the provided live hostel context. " +
                "If information is missing, say 'I don't have that information.' " +
                "Current Data Context:\n" + context;

        // Correct way to build Content for systemInstruction in Java
        Content systemInstruction = new Content.Builder()
                .addText(systemInstructionText)
                .build();
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash", // 2026 Flagship: High reasoning & context
                apiKey, // Your BuildConfig.apiKey
                null, // generationConfig (optional)
                null, // safetySettings (optional)
                new com.google.ai.client.generativeai.type.RequestOptions(), // requestOptions MUST NOT be null
                null, // tools (optional)
                null, // toolConfig (optional)
                systemInstruction // The 'Omni-Context' string as a Content object
        );

        GenerativeModelFutures modelFutures = GenerativeModelFutures.from(gm);
        chatFutures = modelFutures.startChat();
    }

    public void sendMessage(String userMessage, FutureCallback<GenerateContentResponse> callback) {
        Content userContent = new Content("user", Collections.singletonList(new TextPart(userMessage)));
        ListenableFuture<GenerateContentResponse> future = chatFutures.sendMessage(userContent);
        Futures.addCallback(future, callback, Executors.newSingleThreadExecutor());
    }
}
