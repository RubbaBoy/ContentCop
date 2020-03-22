package com.uddernetworks.contentcop.utility;

import java.util.AbstractMap;
import java.util.Map;

public class SEntry<K, V> extends AbstractMap.SimpleEntry<K, V> {

    public SEntry(K key, V value) {
        super(key, value);
    }

    public SEntry(Map.Entry<? extends K, ? extends V> entry) {
        super(entry);
    }
}
