package com.ghost.common.util;

import com.ghost.common.dto.PlayerData;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SerializationUtil {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationUtil.class);

    private SerializationUtil() {
    }

    public static String serialize(PlayerData data) {
        return GSON.toJson(data);
    }

    public static PlayerData deserialize(String js) {
        try {
            return GSON.fromJson(js, PlayerData.class);
        } catch (JsonSyntaxException ex) {
            LOGGER.error("Failed to deserialize Json.", ex);
            return null;
        }
    }
}
