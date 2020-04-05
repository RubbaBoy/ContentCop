package com.uddernetworks.contentcop.image;

import com.uddernetworks.contentcop.ImageProcessor;
import com.uddernetworks.contentcop.database.DatabaseImage;
import com.uddernetworks.contentcop.utility.SEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public abstract class PerceptualProcessor implements ImageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerceptualProcessor.class);

    static final int SIZE = 128;
    static final double SIZE_D = SIZE * SIZE;

    final ImageStore imageStore;

    protected PerceptualProcessor(ImageStore imageStore) {
        this.imageStore = imageStore;
    }

    private int getDifference(byte[] bytes, BitSet testing) {
        var original = BitSet.valueOf(bytes);
        var xor = original.size() > testing.size()
                ? xor(original, testing)
                : xor(testing, original);
        return xor.cardinality();
    }

    private BitSet xor(BitSet one, BitSet two) {
        var original = BitSet.valueOf(one.toByteArray());
        original.xor(two);
        return original;
    }

    @Override
    public Stream<Map.Entry<DatabaseImage, Double>> getMatching(long server, BitSet hash) throws ExecutionException, InterruptedException {
        var hashBytes = hash.toByteArray();

        return imageStore.<Map.Entry<DatabaseImage, Double>>iterateUntilImages(server, image ->
                new SEntry<>(image, getDifference(hashBytes, image.getBitSet()) / SIZE_D)).get()
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .limit(5)
                .peek(entry -> entry.setValue(1 - entry.getValue()));
    }

}
