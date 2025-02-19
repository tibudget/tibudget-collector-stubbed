package com.tibudget.plugins.stubbed;

import java.util.Random;

public class OperationLabelGenerator {

    private static final String[] OPERATION_TYPES = {
            "Paiement CB",
            "Retrait DAB",
            "Virement reçu",
            "Virement émis",
            "Prélèvement",
            "Remboursement",
            "Achat en ligne",
            "Facture",
            "Crédit sur compte",
            "Abonnement"
    };

    private static final String[] MERCHANTS = {
            "Amazon",
            "Carrefour",
            "Fnac",
            "Uber",
            "Netflix",
            "Airbnb",
            "Apple Store",
            "Boulanger",
            "Auchan",
            "Cdiscount"
    };

    private static final String[] LOCATIONS = {
            "Paris",
            "Lyon",
            "Marseille",
            "Bordeaux",
            "Toulouse",
            "Lille",
            "Nice",
            "Strasbourg",
            "Nantes",
            "Montpellier"
    };

    private static final String[] WORDS = {
            "lorem", "ipsum", "dolor", "sit", "amet", "consectetur",
            "adipiscing", "elit", "sed", "do", "eiusmod", "tempor",
            "incididunt", "ut", "labore", "et", "dolore", "magna",
            "aliqua", "ut", "enim", "ad", "minim", "veniam", "quis",
            "nostrud", "exercitation", "ullamco", "laboris", "nisi",
            "ut", "aliquip", "ex", "ea", "commodo", "consequat",
            "duis", "aute", "irure", "dolor", "in", "reprehenderit",
            "in", "voluptate", "velit", "esse", "cillum", "dolore",
            "eu", "fugiat", "nulla", "pariatur", "excepteur",
            "sint", "occaecat", "cupidatat", "non", "proident",
            "sunt", "in", "culpa", "qui", "officia", "deserunt",
            "mollit", "anim", "id", "est", "laborum"
    };

    private static final Random RANDOM = new Random();

    public static String generateOperationLabel() {
        String operationType = OPERATION_TYPES[RANDOM.nextInt(OPERATION_TYPES.length)];
        String merchant = MERCHANTS[RANDOM.nextInt(MERCHANTS.length)];
        String location = LOCATIONS[RANDOM.nextInt(LOCATIONS.length)];
        String reference = generateRandomReference();
        return String.format("%s %s %s (%s)", operationType, merchant, location, reference);
    }

    public static String generateOperationDetails(int wordCount) {
        StringBuilder loremIpsum = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            String word = WORDS[RANDOM.nextInt(WORDS.length)];
            loremIpsum.append(word);
            if (i < wordCount - 1) {
                loremIpsum.append(" ");
            }
        }
        if (loremIpsum.length() > 0) {
            loremIpsum.setCharAt(0, Character.toUpperCase(loremIpsum.charAt(0)));
            loremIpsum.append(".");
        }
        return loremIpsum.toString();
    }

    private static String generateRandomReference() {
        StringBuilder reference = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            char nextChar = RANDOM.nextBoolean() ?
                    (char) ('A' + RANDOM.nextInt(26)) :
                    (char) ('0' + RANDOM.nextInt(10));
            reference.append(nextChar);
        }
        return reference.toString();
    }
}
