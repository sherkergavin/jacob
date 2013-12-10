/*
 * JACOB - CBOR implementation in Java.
 * 
 * (C) Copyright - 2013 - J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package jacob;

import static jacob.CborConstants.*;
import static jacob.CborType.ARRAY;
import static jacob.CborType.BYTE_STRING;
import static jacob.CborType.FLOAT_SIMPLE;
import static jacob.CborType.MAP;
import static jacob.CborType.NEGATIVE_INTEGER;
import static jacob.CborType.TAG;
import static jacob.CborType.TEXT_STRING;
import static jacob.CborType.UNSIGNED_INTEGER;
import static jacob.CborType.decode;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides a decoder capable of handling CBOR encoded data from a {@link InputStream}.
 */
public class CborDecoder {
    private final InputStream m_is;

    /**
     * Creates a new {@link CborDecoder} instance.
     * 
     * @param is the actual input stream to read the CBOR-encoded data from, cannot be <code>null</code>.
     */
    public CborDecoder(InputStream is) {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null!");
        }
        m_is = is;
    }

    private static void fail(String msg, Object... args) throws IOException {
        throw new IOException(String.format(msg, args));
    }

    private static String lengthToString(int len) {
        return (len < 0) ? "no payload" : (len == ONE_BYTE) ? "one byte" : (len == TWO_BYTES) ? "two bytes"
            : (len == FOUR_BYTES) ? "four bytes" : (len == EIGHT_BYTES) ? "eight bytes" : "(unknown)";
    }

    /**
     * Prolog to reading an array value in CBOR format.
     * 
     * @return the number of elements in the array to read, or <tt>-1</tt> in case of infinite-length arrays.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public long readArrayLength() throws IOException {
        return readMajorTypeWithSize(ARRAY);
    }

    /**
     * Reads a boolean value in CBOR format.
     * 
     * @return the read boolean.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public boolean readBoolean() throws IOException {
        int b = readMajorType(FLOAT_SIMPLE);
        if (b != FALSE && b != TRUE) {
            fail("Unexpected boolean value: %d!", b);
        }
        return b == TRUE;
    }

    /**
     * Reads a "break"/stop value in CBOR format.
     * 
     * @return always <code>null</code>.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public Object readBreak() throws IOException {
        readMajorTypeExact(FLOAT_SIMPLE, BREAK);

        return null;
    }

    /**
     * Reads a byte string value in CBOR format.
     * 
     * @return the read byte string, never <code>null</code>. In case the encoded string has a length of <tt>0</tt>, an empty string is returned.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public byte[] readByteString() throws IOException {
        long len = readMajorTypeWithSize(BYTE_STRING);
        if (len > Integer.MAX_VALUE) {
            fail("String length too long!");
        }
        return readFully(new byte[(int) len]);
    }

    /**
     * Prolog to reading a byte string value in CBOR format.
     * 
     * @return the number of bytes in the string to read, or <tt>-1</tt> in case of infinite-length strings.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public long readByteStringLength() throws IOException {
        return readMajorTypeWithSize(BYTE_STRING);
    }

    /**
     * Reads a double-precision float value in CBOR format.
     * 
     * @return the read double value, values from {@link Float#MIN_VALUE} to {@link Float#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public double readDouble() throws IOException {
        readMajorTypeExact(FLOAT_SIMPLE, DOUBLE_PRECISION_FLOAT);

        return Double.longBitsToDouble(readUInt64());
    }

    /**
     * Reads a single-precision float value in CBOR format.
     * 
     * @return the read float value, values from {@link Float#MIN_VALUE} to {@link Float#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public float readFloat() throws IOException {
        readMajorTypeExact(FLOAT_SIMPLE, SINGLE_PRECISION_FLOAT);

        return Float.intBitsToFloat((int) readUInt32());
    }

    /**
     * Reads a half-precision float value in CBOR format.
     * 
     * @return the read half-precision float value, values from {@link Float#MIN_VALUE} to {@link Float#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public double readHalfPrecisionFloat() throws IOException {
        readMajorTypeExact(FLOAT_SIMPLE, HALF_PRECISION_FLOAT);

        int half = readUInt16();
        int exp = (half >> 10) & 0x1f;
        int mant = half & 0x3ff;

        double val;
        if (exp == 0) {
            val = mant * Math.pow(2, -24);
        } else if (exp != 31) {
            val = (mant + 1024) * Math.pow(2, exp - 25);
        } else if (mant != 0) {
            val = Double.NaN;
        } else {
            val = Double.POSITIVE_INFINITY;
        }

        return ((half & 0x8000) == 0) ? val : -val;
    }

    /**
     * Reads a signed or unsigned integer value in CBOR format.
     * 
     * @return the read integer value, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public long readInt() throws IOException {
        int ib = m_is.read();

        // in case of negative integers, extends the sign to all bits; otherwise zero...
        long ui = expectIntegerType(ib);
        // in case of negative integers does a ones complement
        return ui ^ readUInt(ib & 0x1f);
    }

    /**
     * Reads a signed or unsigned 16-bit integer value in CBOR format.
     * 
     * @read the small integer value, values from <tt>[-65536..65535]</tt> are supported.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying output stream.
     */
    public int readInt16() throws IOException {
        int ib = m_is.read();

        // in case of negative integers, extends the sign to all bits; otherwise zero...
        long ui = expectIntegerType(ib);
        // in case of negative integers does a ones complement
        return (int) (ui ^ readUIntExact(TWO_BYTES, ib & 0x1f));
    }

    /**
     * Reads a signed or unsigned 32-bit integer value in CBOR format.
     * 
     * @read the small integer value, values in the range <tt>[-4294967296..4294967295]</tt> are supported.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying output stream.
     */
    public long readInt32() throws IOException {
        int ib = m_is.read();

        // in case of negative integers, extends the sign to all bits; otherwise zero...
        long ui = expectIntegerType(ib);
        // in case of negative integers does a ones complement
        return ui ^ readUIntExact(FOUR_BYTES, ib & 0x1f);
    }

    /**
     * Reads a signed or unsigned 64-bit integer value in CBOR format.
     * 
     * @read the small integer value, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying output stream.
     */
    public long readInt64() throws IOException {
        int ib = m_is.read();

        // in case of negative integers, extends the sign to all bits; otherwise zero...
        long ui = expectIntegerType(ib);
        // in case of negative integers does a ones complement
        return ui ^ readUIntExact(EIGHT_BYTES, ib & 0x1f);
    }

    /**
     * Reads a signed or unsigned 8-bit integer value in CBOR format.
     * 
     * @read the small integer value, values in the range <tt>[-256..255]</tt> are supported.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying output stream.
     */
    public int readInt8() throws IOException {
        int ib = m_is.read();

        // in case of negative integers, extends the sign to all bits; otherwise zero...
        long ui = expectIntegerType(ib);
        // in case of negative integers does a ones complement
        return (int) (ui ^ readUIntExact(ONE_BYTE, ib & 0x1f));
    }

    /**
     * Prolog to reading a map of key-value pairs in CBOR format.
     * 
     * @return the number of entries in the map, >= 0.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public long readMapLength() throws IOException {
        return readMajorTypeWithSize(MAP);
    }

    /**
     * Reads a <code>null</code>-value in CBOR format.
     * 
     * @return always <code>null</code>.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public Object readNull() throws IOException {
        readMajorTypeExact(FLOAT_SIMPLE, NULL);
        return null;
    }

    /**
     * Reads a single byte value in CBOR format.
     * 
     * @return the read byte value.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public byte readSimpleValue() throws IOException {
        readMajorTypeExact(FLOAT_SIMPLE, ONE_BYTE);
        return (byte) readUInt8();
    }

    /**
     * Reads a signed or unsigned small (&lt;= 23) integer value in CBOR format.
     * 
     * @read the small integer value, values in the range <tt>[-24..23]</tt> are supported.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying output stream.
     */
    public int readSmallInt() throws IOException {
        int ib = m_is.read();

        // in case of negative integers, extends the sign to all bits; otherwise zero...
        long ui = expectIntegerType(ib);
        // in case of negative integers does a ones complement
        return (int) (ui ^ readUIntExact(-1, ib & 0x1f));
    }

    /**
     * Reads a semantic tag value in CBOR format.
     * 
     * @return the read tag value.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public long readTag() throws IOException {
        return readUInt(readMajorType(TAG));
    }

    /**
     * Reads an UTF-8 encoded string value in CBOR format.
     * 
     * @return the read UTF-8 encoded string, never <code>null</code>. In case the encoded string has a length of <tt>0</tt>, an empty string is returned.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public String readTextString() throws IOException {
        long len = readMajorTypeWithSize(TEXT_STRING);
        if (len > Integer.MAX_VALUE) {
            fail("String length too long!");
        }
        return new String(readFully(new byte[(int) len]), "UTF-8");
    }

    /**
     * Prolog to reading an UTF-8 encoded string value in CBOR format.
     * 
     * @return the length of the string to read, or <tt>-1</tt> in case of infinite-length strings.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public long readTextStringLength() throws IOException {
        return readMajorTypeWithSize(TEXT_STRING);
    }

    /**
     * Reads an undefined value in CBOR format.
     * 
     * @return always <code>null</code>.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    public Object readUndefined() throws IOException {
        readMajorTypeExact(FLOAT_SIMPLE, UNDEFINED);
        return null;
    }

    /**
     * Reads the next major type from the underlying input stream, and verifies whether it matches the given expectation.
     * 
     * @param majorType the expected major type, cannot be <code>null</code> (unchecked).
     * @return either <tt>-1</tt> if the major type was an signed integer, or <tt>0</tt> otherwise.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    protected long expectIntegerType(int ib) throws IOException {
        CborType majorType = decode(ib);
        if ((majorType != UNSIGNED_INTEGER) && (majorType != NEGATIVE_INTEGER)) {
            fail("Unexpected type: %s, expected type %s or %s!", majorType, UNSIGNED_INTEGER, NEGATIVE_INTEGER);
        }
        return -majorType.ordinal();
    }

    /**
     * Reads the next major type from the underlying input stream, and verifies whether it matches the given expectation.
     * 
     * @param majorType the expected major type, cannot be <code>null</code> (unchecked).
     * @return the read subtype, or payload, of the read major type.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    protected int readMajorType(CborType majorType) throws IOException {
        int ib = m_is.read();
        if (!majorType.isSameOrdinal(ib)) {
            fail("Unexpected type: %s, expected: %s!", decode(ib), majorType);
        }
        return ib & 0x1F;
    }

    /**
     * Reads the next major type from the underlying input stream, and verifies whether it matches the given expectations.
     * 
     * @param majorType the expected major type, cannot be <code>null</code> (unchecked);
     * @param subtype the expected subtype.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    protected void readMajorTypeExact(CborType majorType, int subtype) throws IOException {
        int st = readMajorType(majorType);
        if ((st ^ subtype) != 0) {
            fail("Unexpected subtype: %d, expected: %d!", st, subtype);
        }
    }

    /**
     * Reads the next major type from the underlying input stream, verifies whether it matches the given expectation, and decodes the payload into a size.
     * 
     * @param majorType the expected major type, cannot be <code>null</code> (unchecked).
     * @return the number of succeeding bytes, &gt;= 0.
     * @throws IOException in case of I/O problems reading the CBOR-encoded value from the underlying input stream.
     */
    protected long readMajorTypeWithSize(CborType majorType) throws IOException {
        return readSize(readMajorType(majorType));
    }

    /**
     * Reads an unsigned integer with a given length-indicator.
     * 
     * @param length the length indicator to use;
     * @return the read unsigned integer, as long value. Returns <tt>-1L</tt> if the given length was {@link CborConstants#BREAK}.
     * @throws IOException in case of I/O problems reading the unsigned integer from the underlying input stream.
     */
    protected long readSize(int length) throws IOException {
        long result = -1;
        if (length < ONE_BYTE) {
            return length;
        } else if (length == ONE_BYTE) {
            result = readUInt8();
        } else if (length == TWO_BYTES) {
            result = readUInt16();
        } else if (length == FOUR_BYTES) {
            result = readUInt32();
        } else if (length == EIGHT_BYTES) {
            result = readUInt64();
        } else if (length == BREAK) {
            return -1;
        }

        if (result < 0) {
            fail("Not well-formed CBOR integer found, invalid length: %d!", result);
        }
        return result;
    }

    /**
     * Reads an unsigned integer with a given length-indicator.
     * 
     * @param length the length indicator to use;
     * @return the read unsigned integer, as long value.
     * @throws IOException in case of I/O problems reading the unsigned integer from the underlying input stream.
     */
    protected long readUInt(int length) throws IOException {
        if (length < ONE_BYTE) {
            return length;
        } else if (length == ONE_BYTE) {
            return readUInt8();
        } else if (length == TWO_BYTES) {
            return readUInt16();
        } else if (length == FOUR_BYTES) {
            return readUInt32();
        } else if (length == EIGHT_BYTES) {
            return readUInt64();
        }

        fail("Not well-formed CBOR integer found, invalid length: %d!", length);
        return -1L; // never reached...
    }

    /**
     * Reads an unsigned 16-bit integer value
     * 
     * @return value the read value, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected int readUInt16() throws IOException {
        byte[] buf = readFully(new byte[2]);
        return (buf[0] & 0xFF) << 8 | (buf[1] & 0xFF);
    }

    /**
     * Reads an unsigned 32-bit integer value
     * 
     * @return value the read value, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected long readUInt32() throws IOException {
        byte[] buf = readFully(new byte[4]);
        return ((buf[0] & 0xFF) << 24 | (buf[1] & 0xFF) << 16 | (buf[2] & 0xFF) << 8 | (buf[3] & 0xFF)) & 0xffffffffL;
    }

    /**
     * Reads an unsigned 64-bit integer value
     * 
     * @return value the read value, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected long readUInt64() throws IOException {
        byte[] buf = readFully(new byte[8]);
        return (buf[0] & 0xFFL) << 56 | (buf[1] & 0xFFL) << 48 | (buf[2] & 0xFFL) << 40 | (buf[3] & 0xFFL) << 32 | //
            (buf[4] & 0xFFL) << 24 | (buf[5] & 0xFFL) << 16 | (buf[6] & 0xFFL) << 8 | (buf[7] & 0xFFL);
    }

    /**
     * Reads an unsigned 8-bit integer value
     * 
     * @return value the read value, values from {@link Long#MIN_VALUE} to {@link Long#MAX_VALUE} are supported.
     * @throws IOException in case of I/O problems writing the CBOR-encoded value to the underlying output stream.
     */
    protected int readUInt8() throws IOException {
        return m_is.read() & 0xff;
    }

    /**
     * Reads an unsigned integer with a given length-indicator.
     * 
     * @param length the length indicator to use;
     * @return the read unsigned integer, as long value.
     * @throws IOException in case of I/O problems reading the unsigned integer from the underlying input stream.
     */
    protected long readUIntExact(int expectedLength, int length) throws IOException {
        if (((expectedLength == -1) && (length >= ONE_BYTE)) || ((expectedLength >= 0) && (length != expectedLength))) {
            fail("Unexpected payload/length! Expected %s, but got %s.", lengthToString(expectedLength),
                lengthToString(length));
        }
        return readUInt(length);
    }

    private byte[] readFully(byte[] buf) throws IOException {
        int len = buf.length;
        int n = 0, off = 0;
        while (n < len) {
            int count = m_is.read(buf, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
        return buf;
    }
}