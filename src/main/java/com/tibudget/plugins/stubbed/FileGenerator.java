package com.tibudget.plugins.stubbed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class FileGenerator {

    private static final String RESOURCE_PATH = "samples/"; // Path to the images folder in resources
    private static final Random RANDOM = new Random();

    public static final String[] IMAGE_FILES = {
            "image_produit_01.avif",
            "image_produit_02.jpg",
            "image_produit_03.jpg",
            "image_produit_04.png",
            "image_produit_05.webp",
            "image_produit_06.jpg"
    };

    public static File getRandomImageFile() throws IOException {
        // Select a random image from the pre-defined list
        String selectedImage = IMAGE_FILES[RANDOM.nextInt(IMAGE_FILES.length)];

        // Copy the selected image to a temporary file
        return copyResourceToTempFile(selectedImage);
    }

    private static File copyResourceToTempFile(String resourceName) throws IOException {
        // Load the image as a stream from the classpath
        InputStream inputStream = FileGenerator.class.getClassLoader().getResourceAsStream(RESOURCE_PATH + resourceName);
        if (inputStream == null) {
            throw new RuntimeException("Failed to load resource: " + resourceName);
        }

        // Create a temporary file with the correct extension
        String extension = resourceName.substring(resourceName.lastIndexOf("."));
        File tempFile = File.createTempFile("tibu_stubbed_", extension);

        // Copy the resource content into the temp file
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }

        return tempFile;
    }

    public static File getRandomInvoiceFile() throws IOException {
        return FileGenerator.copyResourceToTempFile("invoice.pdf");
    }
}
