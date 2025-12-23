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
        return copyResourceToTempFile(RESOURCE_PATH + selectedImage);
    }

    /**
     * Copies a resource from the classpath to a temporary file.
     *
     * @param resourceName the name/path of the resource relative to the classpath
     * @return a File object pointing to the newly created temporary file
     * @throws IOException if the resource cannot be read or the file cannot be written
     */
    public static File copyResourceToTempFile(String resourceName) throws IOException {
        // Attempt to load the resource as an InputStream from the classpath
        try (InputStream inputStream = FileGenerator.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Resource not found on classpath: " + resourceName);
            }

            // Extract the file extension (e.g., ".png", ".jpg") from the resource name
            String extension = "";
            int lastDotIndex = resourceName.lastIndexOf('.');
            if (lastDotIndex >= 0 && lastDotIndex < resourceName.length() - 1) {
                extension = resourceName.substring(lastDotIndex);
            }

            // Create a temporary file with a prefix and extracted extension
            File tempFile = File.createTempFile("tibu_", extension);
            tempFile.deleteOnExit(); // Ensure the file is removed when the JVM exits

            // Write the content of the resource to the temp file
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096]; // Larger buffer for better performance
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return tempFile;
        }
    }

    public static File getRandomInvoiceFile() throws IOException {
        return FileGenerator.copyResourceToTempFile(RESOURCE_PATH + "invoice.pdf");
    }
}
