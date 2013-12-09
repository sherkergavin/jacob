/*
 * JACOB - CBOR implementation in Java.
 * 
 * (C) Copyright - 2013 - J.W. Janssen <j.w.janssen@lxtreme.nl>
 *
 * Licensed under Apache License v2.0.
 */
package jacob;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test cases for decoding data in the CBOR format.
 */
@RunWith(Parameterized.class)
public class CborInputStreamInt64Test extends CborInputStreamTestBase<Long> {
    private final Class<? extends Exception> m_exceptionClass;

    public CborInputStreamInt64Test(long output, int[] encodedInput, Class<? extends Exception> exceptionType) {
        super(encodedInput, output);

        m_exceptionClass = exceptionType;
    }

    @Parameters(name = "{index}: decoding 64-bit integer {0}")
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList( //
            new Object[] { 0, new int[] { 0x1b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, NONE }, // 0
            new Object[] { 1, new int[] { 0x1b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 }, NONE }, // 1
            new Object[] { 1000000, new int[] { 0x1b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0f, 0x42, 0x40 }, NONE }, // 2
            new Object[] { 1000000000000L, new int[] { 0x1b, 0x00, 0x00, 0x00, 0xe8, 0xd4, 0xa5, 0x10, 0x00 }, NONE }, // 3
            new Object[] { -1, new int[] { 0x3b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, NONE }, // 4
            new Object[] { -1000000, new int[] { 0x3b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0f, 0x42, 0x3f }, NONE }, // 5
            new Object[] { -1000000000000L, new int[] { 0x3b, 0x00, 0x00, 0x00, 0xe8, 0xd4, 0xa5, 0x0f, 0xff }, NONE }, // 6
            new Object[] { Long.MAX_VALUE, new int[] { 0x1b, 0x7f, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff }, NONE }, // 7
            new Object[] { Long.MIN_VALUE, new int[] { 0x3b, 0x7f, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff }, NONE }, // 8
            new Object[] { 4294967295L, new int[] { 0x1a, 0xff, 0xff, 0xff, 0xff }, IOException.class }, // 9
            new Object[] { -65537, new int[] { 0x40, 0x00, 0x00, 0x00, 0x01 }, IOException.class }, // 10
            new Object[] { 65535, new int[] { 0x20, 0x00, 0x00, 0x00, 0x01 }, IOException.class }, // 11
            new Object[] { 500, new int[] { 0x19, 0x01, 0xf4 }, IOException.class }, // 12
            new Object[] { 0, new int[] { 0x00 }, IOException.class } // 13
            );
    }

    @Test
    public void test() throws IOException {
        if (m_exceptionClass == null) {
            assertEquals(m_expectedOutput.longValue(), m_stream.readInt64());
        } else {
            try {
                m_stream.readInt64();

                fail(m_exceptionClass);
            }
            catch (Exception e) {
                assertException(m_exceptionClass, e);
            }
        }
    }
}
