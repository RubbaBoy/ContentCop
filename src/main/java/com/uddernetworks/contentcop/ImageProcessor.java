package com.uddernetworks.contentcop;

import com.uddernetworks.contentcop.database.DatabaseImage;

import java.io.InputStream;
import java.util.BitSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public interface ImageProcessor {

    Optional<BitSet> getHash(InputStream imageStream);

    Stream<Map.Entry<DatabaseImage, Double>> getMatching(long server, BitSet hash) throws ExecutionException, InterruptedException;

}
