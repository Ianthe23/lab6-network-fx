package org.example.lab6networkfx.domain.messages;

import org.example.lab6networkfx.domain.Entity;
import org.example.lab6networkfx.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Message extends Entity<Integer> {
    private User from;
    private User to;
    private String message;
    private LocalDateTime date;

    public Message(String message, LocalDateTime date, User to, User from) {
        this.message = message;
        this.date = date;
        this.to = to;
        this.from = from;
    }

    public User getFrom() {
        return from;
    }

    public void setFrom(User from) {
        this.from = from;
    }

    public User getTo() {
        return to;
    }

    public void setTo(User to) {
        this.to = to;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Message{" +
                "from=" + from +
                ", message='" + message + '\'' +
                ", date=" + date +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message1 = (Message) o;
        return Objects.equals(getFrom(), message1.getFrom()) && Objects.equals(getTo(), message1.getTo()) && Objects.equals(getMessage(), message1.getMessage()) && Objects.equals(getDate(), message1.getDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFrom(), getTo(), getMessage(), getDate());
    }
}