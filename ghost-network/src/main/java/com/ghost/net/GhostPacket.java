package com.ghost.net;

import org.jetbrains.annotations.Nullable;

/**
 * A generic wrapper for all communications between the client and server.
 *
 * @param <T> The type of the payload data.
 */
public final class GhostPacket<T> {

    // Non-final fields for Gson deserialization
    private MessageType type;
    @Nullable
    private T data; // The payload (e.g., PlayerData, a UUID string, or a list of PlayerData)

    // Default constructor for Gson
    public GhostPacket() {
    }

    // Main constructor
    public GhostPacket(MessageType type, @Nullable T data) {
        this.type = type;
        this.data = data;
    }

    // Getters
    public MessageType getType() {
        return type;
    }

    @Nullable
    public T getData() {
        return data;
    }

    // No setters to maintain immutability after creation
}