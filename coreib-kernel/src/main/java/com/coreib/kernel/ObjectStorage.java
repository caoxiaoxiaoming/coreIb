package com.coreib.kernel;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/** Private object-storage boundary. Implementations must return short-lived signed access links. */
public interface ObjectStorage {
    StoredObject put(String namespace, String objectName, String contentType, InputStream content);

    URI signedGetUrl(String objectKey, Duration validity);

    void delete(String objectKey);

    record StoredObject(String objectKey, long size, String contentType) {
    }
}
