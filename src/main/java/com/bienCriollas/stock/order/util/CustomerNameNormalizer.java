package com.bienCriollas.stock.order.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Normaliza nombres de clientes para persistirlos con un formato consistente. */
public final class CustomerNameNormalizer {

    private CustomerNameNormalizer() {
    }

    public static String normalize(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }

        String cleanName = name
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);

        return Arrays.stream(cleanName.split(" "))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
