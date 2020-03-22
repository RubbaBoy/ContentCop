package com.uddernetworks.contentcop.image;

import com.uddernetworks.contentcop.database.DatabaseImage;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;

public interface ImageStore {

    /**
     * Initialized the default/existing values. This is dependant on the implementation.
     *
     * @return The {@link CompletableFuture} of the process
     */
    CompletableFuture<Void> init();

    /**
     * Adds a single {@link DatabaseImage} to the store. The default implementation adds this to the database as well.
     *
     * @param image The image to add
     */
    CompletableFuture<Void> addImage(DatabaseImage image);

    /**
     * Performs {@link #addImage(DatabaseImage)} multiple times, more efficiently than simply invoking the method
     * repeatedly.
     *
     * @param images The images to add
     */
    CompletableFuture<Void> addImages(List<DatabaseImage> images);

    /**
     * Invoked test for every image from the given guild, until the test returns non-null.
     *
     * @param <T> The returning type of <code>test</code>
     * @param server The server to get the images from
     * @param test The function to invoke
     * @return A {@link CompletableFuture} of the task
     */
    <T> Future<Stream<T>> iterateUntilImages(long server, Function<DatabaseImage, T> test);
}
