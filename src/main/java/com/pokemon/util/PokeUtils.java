package com.pokemon.util;

import java.util.Map;

public class PokeUtils {
    public static Integer getIdFromUrl(String urlDataPoke) {
        String[] parts = urlDataPoke.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    /**
     * Obtiene un String de un Map anidado usando una clave separada por puntos.
     */
    public static String getStringFromNestedMap(Map<String, Object> map, String key) {
        Map<String, Object> mapValue = map;
        String[] parts = key.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            mapValue = (Map<String, Object>) mapValue.get(parts[i]);
        }
        return (String) mapValue.get(parts[parts.length - 1]);
    }

    /**
     * Obtiene un Map de un Map anidado usando una clave separada por puntos.
     */
    public static Map<String, Object> getMapFromNestedMap(Map<String, Object> map, String key) {
        Map<String, Object> mapValue = map;
        String[] parts = key.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            Object value = mapValue.get(parts[i]);
            if (i == parts.length - 1) {
                if (value instanceof Map) {
                    return (Map<String, Object>) value;
                } else {
                    return null;
                }
            }
            if (!(value instanceof Map)) {
                return null;
            }
            mapValue = (Map<String, Object>) value;
        }
        return null;
    }
} 