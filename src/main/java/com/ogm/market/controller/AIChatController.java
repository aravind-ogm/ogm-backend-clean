package com.ogm.market.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ogm.market.dto.ChatMessageDTO;
import com.ogm.market.dto.ChatRequestDTO;
import com.ogm.market.dto.ChatResponseDTO;
import com.ogm.market.model.Property;
import com.ogm.market.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:3000")
public class AIChatController {

    private final PropertyRepository propertyRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // ⛔ NEVER hardcode this – use env / application.yml
//    @Value("${openai.api.key}")
    private String openAiApiKey;

    public AIChatController(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {

        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ChatResponseDTO.builder().reply("No messages provided").build());
        }

        // Last user message
        ChatMessageDTO lastUser = request.getMessages()
                .stream()
                .filter(m -> "user".equalsIgnoreCase(m.getFrom()))
                .reduce((first, second) -> second)
                .orElse(null);

        if (lastUser == null) {
            return ResponseEntity.badRequest()
                    .body(ChatResponseDTO.builder().reply("No user message found").build());
        }

        // 1️⃣ Get some properties from DB (top 20)
        List<Property> properties = propertyRepository
                .findAll(PageRequest.of(0, 20))
                .getContent();

        String propertyContext = properties.stream()
                .map(p -> String.format(
                        "ID: %d | Title: %s | Location: %s | Price: %s | Type: %s | BHK: %s | Sqft: %s",
                        p.getId(),
                        safe(p.getTitle()),
                        safe(p.getLocation()),
                        safe(p.getPrice()),
                        safe(p.getType()),
                        p.getBedrooms() == null ? "-" : p.getBedrooms().toString(),
                        safe(p.getSqft())
                ))
                .collect(Collectors.joining("\n"));

        String systemPrompt =
                "You are an AI property assistant for OGM Market. " +
                        "You MUST answer ONLY based on the properties provided below.\n\n" +
                        "Each property has an ID, title, location, price, type, bedrooms (BHK) and sqft.\n" +
                        "When user asks something, pick 3–6 best matching options and:\n" +
                        "- Explain why they match\n" +
                        "- Include property IDs in your answer\n" +
                        "- Be concise but helpful.\n\n" +
                        "PROPERTIES:\n" + propertyContext;

        try {
            String aiReply = callOpenAI(systemPrompt, lastUser.getText());
            return ResponseEntity.ok(ChatResponseDTO.builder().reply(aiReply).build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(ChatResponseDTO.builder()
                            .reply("Sorry, I had an issue talking to the AI service.")
                            .build());
        }
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    /**
     * Calls OpenAI Chat Completions API
     */
    private String callOpenAI(String systemPrompt, String userMessage) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions"; // :contentReference[oaicite:0]{index=0}

        ObjectNode body = mapper.createObjectNode();
        body.put("model", "gpt-4.1-mini"); // or any available chat model

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode sys = mapper.createObjectNode();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        messages.add(sys);

        ObjectNode user = mapper.createObjectNode();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(user);

        body.set("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey); // from application.yml/env :contentReference[oaicite:1]{index=1}

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("OpenAI error: " + response.getStatusCode());
        }

        JsonNode root = mapper.readTree(response.getBody());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText();
        }

        return "Sorry, I couldn't generate a response.";
    }
}
