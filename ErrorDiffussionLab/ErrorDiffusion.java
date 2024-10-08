import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public class ErrorDiffusion {

    private final int width;
    private final int height;
    private final int[][] outputImage;
    private final int numThreads;
    private final CountDownLatch[][] readyToProcess;

    public ErrorDiffusion(int width, int height, int numThreads) {
        this.width = width;
        this.height = height;
        this.outputImage = new int[height][width];
        this.numThreads = numThreads; // Number of threads is now configurable
        this.readyToProcess = new CountDownLatch[height][width];

        // Initialize latches for each pixel
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                readyToProcess[i][j] = new CountDownLatch(1);
            }
        }
    }

    private static int[][] loadImage(String path) throws IOException {
        BufferedImage img = ImageIO.read(new File(path));
        int width = img.getWidth();
        int height = img.getHeight();
        int[][] grayscale = new int[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int rgb = img.getRGB(j, i);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb & 0xFF);
                int grayLevel = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                grayscale[i][j] = grayLevel;
            }
        }
        return grayscale;
    }

    private void saveImage(String path) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int value = outputImage[i][j] == 0 ? 0 : 255;
                img.setRGB(j, i, (value << 16) | (value << 8) | value);
            }
        }
        ImageIO.write(img, "png", new File(path));
    }

    @SuppressWarnings("deprecation")
    public void processImage(int[][] inputImage) {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < height; i++) {
            final int row = i;
            executor.submit(() -> {
                long rowStartTime = System.nanoTime();
                try {
                    for (int j = 0; j < width; j++) {
                        final int col = j;

                        // Wait for the previous row to process the necessary pixels
                        if (row > 0) {
                            if (col > 0) readyToProcess[row - 1][col - 1].await();
                            readyToProcess[row - 1][col].await();
                            if (col + 1 < width) readyToProcess[row - 1][col + 1].await();
                        }

                        int oldPixel = inputImage[row][col];
                        int newPixel = oldPixel < 128 ? 0 : 255;
                        outputImage[row][col] = newPixel;
                        int err = oldPixel - newPixel;

                        // Diffusion logic with error spreading
                        if (col + 1 < width) inputImage[row][col + 1] += err * 7 / 16;
                        if (row + 1 < height) {
                            if (col > 0) inputImage[row + 1][col - 1] += err * 3 / 16;
                            inputImage[row + 1][col] += err * 5 / 16;
                            if (col + 1 < width) inputImage[row + 1][col + 1] += err * 1 / 16;
                        }

                        // Signal that this pixel is processed
                        readyToProcess[row][col].countDown();
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
                long rowEndTime = System.nanoTime();
                System.out.println("Thread ID: " + Thread.currentThread().getId() + " processed row " + row + " in " + (rowEndTime - rowStartTime) + " ns");
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Total processing time: " + (endTime - startTime) + " ms");
    }

    public static void main(String[] args) {
        try {
            String inputPath = "ErrorDiffussionLab/miraidon.png";
            String outputPath = "ErrorDiffussionLab/miraidon_output.png";
            int numThreads = 4; // Configurable number of threads
            int[][] inputImage = loadImage(inputPath);

            // Save grayscale image
            BufferedImage img = new BufferedImage(inputImage[0].length, inputImage.length, BufferedImage.TYPE_BYTE_GRAY);
            for (int i = 0; i < inputImage.length; i++) {
                for (int j = 0; j < inputImage[0].length; j++) {
                    img.setRGB(j, i, (inputImage[i][j] << 16) | (inputImage[i][j] << 8) | inputImage[i][j]);
                }
            }
            ImageIO.write(img, "png", new File("ErrorDiffussionLab/miraidon_grayscale.png"));

            // Process image using error diffusion
            ErrorDiffusion processor = new ErrorDiffusion(inputImage[0].length, inputImage.length, numThreads);
            processor.processImage(inputImage);
            processor.saveImage(outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
