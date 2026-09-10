package com.librio.util;

public final class IsbnUtils {

    private IsbnUtils() {}

    /**
     * Strips formatting characters, validates checksum, and converts ISBN-10 to ISBN-13.
     * Throws IllegalArgumentException if the ISBN is invalid.
     */
    public static String normalizeToIsbn13(String isbn) {
        if (isbn == null) {
            throw new IllegalArgumentException("ISBN cannot be null");
        }
        
        String clean = isbn.replaceAll("[\\s-]+", "");
        
        if (clean.length() == 10) {
            if (!isValidIsbn10(clean)) {
                throw new IllegalArgumentException("Invalid ISBN-10 checksum");
            }
            return convertToIsbn13(clean);
        } else if (clean.length() == 13) {
            if (!isValidIsbn13(clean)) {
                throw new IllegalArgumentException("Invalid ISBN-13 checksum");
            }
            return clean;
        } else {
            throw new IllegalArgumentException("ISBN must be 10 or 13 digits");
        }
    }

    private static boolean isValidIsbn10(String isbn) {
        if (!isbn.matches("^\\d{9}[\\dX]$")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (isbn.charAt(i) - '0') * (10 - i);
        }
        char last = isbn.charAt(9);
        sum += (last == 'X') ? 10 : (last - '0');
        return sum % 11 == 0;
    }

    private static boolean isValidIsbn13(String isbn) {
        if (!isbn.matches("^\\d{13}$")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = isbn.charAt(i) - '0';
            sum += digit * ((i % 2 == 0) ? 1 : 3);
        }
        int last = isbn.charAt(12) - '0';
        int check = (10 - (sum % 10)) % 10;
        return last == check;
    }

    private static String convertToIsbn13(String isbn10) {
        String core = "978" + isbn10.substring(0, 9);
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = core.charAt(i) - '0';
            sum += digit * ((i % 2 == 0) ? 1 : 3);
        }
        int check = (10 - (sum % 10)) % 10;
        return core + check;
    }
}
