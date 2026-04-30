package hk.hkucs.comp7506_project.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatSession {
    private final String id;
    private String title;
    private final List<ChatMessage> messages;
    private final long createdAt;

    public ChatSession(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.messages = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public ChatSession(String id, String title, long createdAt) {
        this.id = id;
        this.title = title;
        this.messages = new ArrayList<>();
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
    }

    public String getPreview() {
        if (messages.isEmpty()) return "No messages yet";
        return messages.get(messages.size() - 1).getContent();
    }
}
