package com.ogm.market.ai;

public class GeminiEmbeddingRequest {

    private Content content;

    public GeminiEmbeddingRequest(String text) {
        this.content = new Content(text);
    }

    public Content getContent() {
        return content;
    }

    static class Content {

        private Part[] parts;

        public Content(String text) {
            this.parts = new Part[]{new Part(text)};
        }

        public Part[] getParts() {
            return parts;
        }
    }

    static class Part {

        private String text;

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
}