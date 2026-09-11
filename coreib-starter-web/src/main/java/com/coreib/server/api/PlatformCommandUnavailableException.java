package com.coreib.server.api;

public class PlatformCommandUnavailableException extends RuntimeException {
    public PlatformCommandUnavailableException() { super("Platform directory commands require an enabled database or a custom adapter"); }

    public PlatformCommandUnavailableException(String message) {
        super(message);
    }
}
