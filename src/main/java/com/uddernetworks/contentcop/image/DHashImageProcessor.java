package com.uddernetworks.contentcop.image;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Optional;

import static com.uddernetworks.contentcop.image.DHashImageProcessor.Offset.ALPHA;
import static com.uddernetworks.contentcop.image.DHashImageProcessor.Offset.BLUE;
import static com.uddernetworks.contentcop.image.DHashImageProcessor.Offset.GREEN;
import static com.uddernetworks.contentcop.image.DHashImageProcessor.Offset.RED;

/**
 * An {@link com.uddernetworks.contentcop.ImageProcessor} using Difference hashing, or dHashing.
 * This is a bit slower than {@link AHashImageProcessor}, but much more accurate. This is the reccommended
 * algorithm to use in most cases.
 */
public class DHashImageProcessor extends PerceptualProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DHashImageProcessor.class);

    public DHashImageProcessor(ImageStore imageStore) {
        super(imageStore);
    }

    @Override
    public Optional<BitSet> getHash(InputStream imageStream) {
        try (imageStream) {
            var resized = Scalr.resize(ImageIO.read(imageStream), Scalr.Method.SPEED, Scalr.Mode.FIT_EXACT, SIZE, SIZE);

            var i = 0;
            var bits = new BitSet();
            for (var y = 0; y < SIZE; y++) {
                var left = getBrighness(resized.getRGB(0, y));

                for (var x = 1; x < SIZE; x++) {
                    var right = getBrighness(resized.getRGB(x, y));

                    System.out.println(left + " > " + right);
                    bits.set(i++, left > right);

                    left = right;
                }
            }

            LOGGER.info("Creating hash: {}", stringShit(bits));

            return Optional.of(bits);
        } catch (Exception ignored) {
            ignored.printStackTrace();
            System.out.println("Shit is here error!!!");
            return Optional.empty();
        }
    }

    private String stringShit(BitSet bits) {
        return bits.isEmpty() ? "" : new BigInteger(bits.toByteArray()).toString(2);
    }

    private static int getBrighness(int rgb) {
        // -13223617 is Discord gray
        return ALPHA.color(rgb) == 0 ? -13223617 : (int) Math.floor((RED.color(rgb) * 0.299) + (GREEN.color(rgb) * 0.587) + (BLUE.color(rgb) * 0.114));
    }

    enum Offset {
        RED(16), GREEN(8), BLUE(0), ALPHA(24);

        private final int shift;

        Offset(int shift) {
            this.shift = shift;
        }

        public int getShift() {
            return shift;
        }

        public int color(int rgb) {
            return (rgb >> shift) & 0xFF;
        }
    }
}
