package com.tibudget.plugins.stubbed;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class FileGenerator {

    private static final String RESOURCE_PATH = "samples/"; // Path to the images folder in resources
    private static final Random RANDOM = new Random();
    private static final List<String> imageFiles; // Cached list of image files

    static {
        try {
            imageFiles = loadImageFiles();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to load image files from resources.", e);
        }
    }

    public static File getRandomImageFile() throws IOException {
        if (imageFiles.isEmpty()) {
            throw new RuntimeException("No image files found in resources.");
        }

        // Select a random image from the cached list
        String selectedImage = imageFiles.get(RANDOM.nextInt(imageFiles.size()));

        // Copy the selected image to a temporary file
        return copyToTempFile(selectedImage);
    }

    private static List<String> loadImageFiles() throws IOException, URISyntaxException {
        // Get the URL of the "images" directory
        URL resourceUrl = FileGenerator.class.getClassLoader().getResource(RESOURCE_PATH);
        if (resourceUrl == null) {
            throw new RuntimeException("Resource folder not found: " + RESOURCE_PATH);
        }

        Path resourcePath = Paths.get(resourceUrl.toURI());

        // List and filter valid image files
        return Files.list(resourcePath)
                .map(path -> path.getFileName().toString()) // Extract file names
                .filter(name -> name.matches(".*\\.(jpg|jpeg|png|avif|webp)$")) // Filter image formats
                .collect(Collectors.toList());
    }

    private static File copyToTempFile(String resourceName) throws IOException {
        // Load the image from classpath
        InputStream inputStream = FileGenerator.class.getClassLoader().getResourceAsStream(RESOURCE_PATH + resourceName);
        if (inputStream == null) {
            throw new RuntimeException("Failed to load resource: " + resourceName);
        }

        // Create a temporary file with the correct extension
        String extension = resourceName.substring(resourceName.lastIndexOf("."));
        File tempFile = File.createTempFile("product_image_", extension);

        // Copy content to the temporary file
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            inputStream.transferTo(outputStream);
        }

        return tempFile;
    }

    public static File getRandomInvoiceFile() throws IOException {
        return FileGenerator.copyToTempFile("invoice.pdf");
    }
}
