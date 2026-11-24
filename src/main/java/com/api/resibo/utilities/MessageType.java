package com.api.resibo.utilities;

/**
 * Response message constants. Add entries as modules are built, rather than
 * inlining message strings at call sites.
 */
public class MessageType {

    public static final String API_HEALTH_CHECK = "Resibo API is healthy!";

    private MessageType() {
        // constants only
    }
}
