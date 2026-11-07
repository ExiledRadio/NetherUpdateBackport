package com.exiledradio.netherupdatebackport.selftest;

/** One stream's headless test. */
public interface SelfTest {

    /** The stream name, as passed to {@code -Pselftest=}. */
    String name();

    void run(Check check) throws Exception;
}
