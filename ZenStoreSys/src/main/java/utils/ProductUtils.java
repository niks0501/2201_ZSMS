package utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ProductUtils {

    private static final String IMAGE_DIR = "C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage";
    private static final String BARCODE_DIR = "C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes";

    // Save product image and return the path
    public static String saveProductImage(File sourceFile) throws IOException {
        if (sourceFile == null) return null;

        // Create directory if it doesn't exist
        Path targetDir = Paths.get(IMAGE_DIR);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // Generate unique filename
        String fileName = "product_" + System.currentTimeMillis() + getFileExtension(sourceFile.getName());
        Path targetPath = targetDir.resolve(fileName);

        // Copy file
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return targetPath.toString();
    }

    // Generate barcode and return the path
    public static String generateBarcode(int productId) throws Exception {
        // Create directory if it doesn't exist
        Path targetDir = Paths.get(BARCODE_DIR);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        String fileName = "barcode_" + productId + ".png";
        Path barcodePath = targetDir.resolve(fileName);

        // Generate barcode (using ZXing library)
        BitMatrix bitMatrix = new Code128Writer().encode(
                String.valueOf(productId), BarcodeFormat.CODE_128, 300, 100);

        BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ImageIO.write(barcodeImage, "PNG", barcodePath.toFile());

        return barcodePath.toString();
    }

    private static String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? ".jpg" : filename.substring(dotIndex);
    }
}