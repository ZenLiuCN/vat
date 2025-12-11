package vat.api.utils;

import io.vertx.core.buffer.Buffer;

///
/// @author Zen.Liu
/// @since 2025-11-19


public interface BufferCodec {
    Buffer encode(Buffer v);

    Buffer decode(Buffer v);

    interface Chars {
        default CharSequence encode(byte[] b) {
            return encode(Buffer.buffer(b));
        }

        CharSequence encode(Buffer v);

        Buffer decode(CharSequence v);

        Chars BASE94 = new Base94();

        record Base94(int code) implements Chars {
            public Base94(char code) {
                this((int) code);
            }

            public Base94() {
                this(33);
            }

            @Override
            public CharSequence encode(Buffer v) {
                var b = new StringBuilder();
                var buf = 0;
                var bits = 0;
                for (int i = 0; i < v.length(); i++) {
                    buf |= ((v.getByte(i) & 0xFF) << bits);
                    bits += 8;
                    while (bits >= 6) {

                        b.append((char) ((buf & 0x3F) + code));
                        buf >>>= 6;
                        bits -= 6;
                    }
                }
                if (bits > 0) b.append((char) (((byte) (buf & 0x3F)) + code));
                return b;
            }

            @Override
            public Buffer decode(CharSequence v) {
                var b = Buffer.buffer();
                var buf = 0;
                var bits = 0;
                for (int i = 0; i < v.length(); i++) {
                    buf |= ((int) (v.charAt(i)) - code) << bits;
                    bits += 6;
                    while (bits >= 8) {
                        b.appendByte((byte) (buf & 0xFF));
                        buf >>>= 8;
                        bits -= 8;
                    }
                }
                return b;
            }

        }

        Chars BASE32 = new Base32();

        record Base32() implements Chars {
            static final char[] DIGITS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
            static final int[] BASE32LOOKUP = {
                    0xFF, 0xFF, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
                    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
                    0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                    0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
                    0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
                    0x17, 0x18, 0x19, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
                    0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                    0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
                    0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
                    0x17, 0x18, 0x19, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
            };


            @Override
            public CharSequence encode(Buffer v) {
                int i = 0, index = 0, digit = 0;
                int currByte, nextByte;
                var base32 = new StringBuilder((v.length() + 7) * 8 / 5);
                while (i < v.length()) {
                    currByte = (v.getByte(i) >= 0) ? v.getByte(i) : (v.getByte(i) + 256);
                    /* Is the current digit going to span a byte boundary? */
                    if (index > 3) {
                        if ((i + 1) < v.length()) {
                            var next = v.getByte(i + 1);
                            nextByte = (next >= 0) ? next : (next + 256);
                        } else {
                            nextByte = 0;
                        }
                        digit = currByte & (0xFF >> index);
                        index = (index + 5) % 8;
                        digit <<= index;
                        digit |= nextByte >> (8 - index);
                        i++;
                    } else {
                        digit = (currByte >> (8 - (index + 5))) & 0x1F;
                        index = (index + 5) % 8;
                        if (index == 0)
                            i++;
                    }
                    base32.append(DIGITS[digit]);
                }
                return base32;
            }

            static void orAssign(Buffer b, int offset, int d) {
                if (offset >= b.length()) b.appendByte((byte) (d));
                else
                    b.setByte(offset, (byte) (b.getByte(offset) | d));
            }

            @Override
            public Buffer decode(CharSequence encoded) {
                int i, index, lookup, offset, digit;
                var max=encoded.length() * 5 / 8;
                var bytes = Buffer.buffer(max);
                for (i = 0, index = 0, offset = 0; i < encoded.length(); i++) {
                    lookup = encoded.charAt(i) - '0';
                    /* Skip chars outside the lookup table */
                    if (lookup < 0 || lookup >= BASE32LOOKUP.length) {
                        throw new IllegalArgumentException("invalid Base32 code");
                    }
                    digit = BASE32LOOKUP[lookup];
                    /* If this digit is not in the table, ignore it */
                    if (digit == 0xFF) {
                        throw new IllegalArgumentException("invalid Base32 code");
                    }
                    if (index <= 3) {
                        index = (index + 5) % 8;
                        if (index == 0) {
                            orAssign(bytes, offset, digit);
                            offset++;
                            if (offset >= max)
                                break;
                        } else {
                            orAssign(bytes, offset, digit << (8 - index));
                        }
                    } else {
                        index = (index + 5) % 8;
                        orAssign(bytes, offset, (digit >>> index));
                        offset++;

                        if (offset >=max) {
                            break;
                        }
                        orAssign(bytes, offset, digit << (8 - index));
                    }
                }
                return bytes;
            }
        }
    }

}
