package com.uddernetworks.contentcop.image;

import com.uddernetworks.contentcop.ImageProcessor;
import com.uddernetworks.contentcop.database.DatabaseImage;
import com.uddernetworks.contentcop.database.DatabaseManager;
import com.uddernetworks.contentcop.utility.SEntry;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class PerceptualImageProcessor implements ImageProcessor {

    private static final int SIZE = 128;
    private static final double SIZE_D = SIZE * SIZE;

    private final DatabaseManager databaseManager;
    private final ImageStore imageStore;

    public PerceptualImageProcessor(DatabaseManager databaseManager, ImageStore imageStore) {
        this.databaseManager = databaseManager;
        this.imageStore = imageStore;
    }

    @Override
    public Optional<BitSet> getHash(InputStream imageStream) {
        try (imageStream) {
            var resized = Scalr.resize(ImageIO.read(imageStream), Scalr.Method.SPEED, Scalr.Mode.FIT_EXACT, SIZE, SIZE, Scalr.OP_GRAYSCALE);

            var bytes = new byte[SIZE * SIZE];
            var total = 0L;

            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    total += (bytes[x + (y * SIZE)] = (byte) resized.getRGB(x, y));
                }
            }

            var grayscale = total / (SIZE * SIZE);

            var buffer = new BitSet();
            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] >= grayscale) {
                    buffer.set(i);
                }
            }

            return Optional.of(buffer);
        } catch (Exception ignored) {
            ignored.printStackTrace();
            return Optional.empty();
        }
    }

    private int getDifference(byte[] bytes, BitSet testing) {
        var original = BitSet.valueOf(bytes);
        original.xor(testing);
        return original.cardinality();
    }

    @Override
    public Stream<Entry<DatabaseImage, Double>> getMatching(long server, BitSet hash) throws ExecutionException, InterruptedException {
        var hashBytes = hash.toByteArray();

        return imageStore.<Entry<DatabaseImage, Double>>iterateUntilImages(server, image ->
                new SEntry<>(image, getDifference(hashBytes, image.getBitSet()) / SIZE_D)).get()
                .sorted(Comparator.comparingDouble(Entry::getValue))
                .limit(5)
                .peek(entry -> entry.setValue(1 - entry.getValue()));
    }
}
