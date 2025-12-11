package com.ghost.api.packet;

public enum MessageType {
    /**
     * A regular update of a player's state (position, rotation, etc.).
     * This is the most frequent message type.
     */
    UPDATE,

    /**
     * Sent to a newly connected client, containing the initial state of all
     * existing players.
     */
    INITIAL_SYNC,

    /**
     * Sent to all clients when a new player joins the server.
     * Often, its data is the same as an UPDATE message.
     */
    JOIN,

    /**
     * Sent to all clients when a player leaves the server.
     * This message will contain the UUID of the player who left.
     */
    LEAVE,
    AUTH_CHALLENGE,
    AUTH_RESPONSE,
    AUTH_SUCCESS,
    UNRECOGNIZED
}