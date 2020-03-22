package com.uddernetworks.contentcop.image;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.BitSet;
import java.util.Optional;

/**
 * An {@link com.uddernetworks.contentcop.ImageProcessor} using Average Hashing, or aHashing.
 * This is faster than {@link DHashImageProcessor}, but mildly more inaccurate.
 */
public class AHashImageProcessor extends PerceptualProcessor {

    public AHashImageProcessor(ImageStore imageStore) {
        super(imageStore);
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
            return Optional.empty();
        }
    }
}
