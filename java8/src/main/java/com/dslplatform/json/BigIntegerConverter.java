package com.dslplatform.json;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

public abstract class BigIntegerConverter {

	public static final JsonReader.ReadObject<BigInteger> READER = new JsonReader.ReadObject<BigInteger>() {
		@Nullable
		@Override
		public BigInteger read(JsonReader reader) throws IOException {
			return reader.wasNull() ? null : deserialize(reader);
		}
	};
	public static final JsonWriter.WriteObject<BigInteger> WRITER = new JsonWriter.WriteObject<BigInteger>() {
		@Override
		public void write(JsonWriter writer, @Nullable BigInteger value) {
			serialize(value, writer);
		}
	};

	private static BigInteger parseNumberGeneric(final char[] buf, final int len, final JsonReader reader) throws ParsingException {
		int end = len;
		while (end > 0 && Character.isWhitespace(buf[end - 1])) {
			end--;
		}
		if (end > reader.maxNumberDigits) {
			throw reader.newParseErrorWith("Too many digits detected in number", len, "", "Too many digits detected in number", end, "");
		}
		try {
			return new BigInteger(new String(buf, 0, end));
		} catch (NumberFormatException nfe) {
			throw reader.newParseErrorAt("Error parsing number", len, nfe);
		}
	}

	public static void serialize(@Nullable final BigInteger value, final JsonWriter sw) {
		if (value == null) {
			sw.writeNull();
		} else {
			sw.writeAscii(value.toString());
		}
	}

	public static BigInteger deserialize(final JsonReader reader) throws IOException {
		if (reader.last() == '"') {
			final int len = reader.parseString();
			return parseNumberGeneric(reader.chars, len, reader);
		}
		final int start = reader.scanNumber();
		int end = reader.getCurrentIndex();
		int len = end - start;
		if (len > 18) {
			end = reader.findNonWhitespace(end);
			len = end - start;
			if (end == reader.length()) {
				final NumberConverter.NumberInfo info = NumberConverter.readLongNumber(reader, start);
				return parseNumberGeneric(info.buffer, info.length, reader);
			} else if (len > 18) {
				return parseNumberGeneric(reader.prepareBuffer(start, len), len, reader);
			}
		}
		final byte[] buf = reader.buffer;
		final byte ch = buf[start];
		int i = start;
		long value = 0;
		if (ch == '-') {
			i = start + 1;
			if (i == end) NumberConverter.numberException(reader, start, end, "Digit not found");
			for (; i < end; i++) {
				final int ind = buf[i] - 48;
				if (ind < 0 || ind > 9) {
					if (i > start + 1 && reader.allWhitespace(i, end)) return BigInteger.valueOf(value);
					NumberConverter.numberException(reader, start, end, "Unknown digit", (char)ch);
				}
				value = (value << 3) + (value << 1) - ind;
			}
			return BigInteger.valueOf(value);
		}
		if (i == end) NumberConverter.numberException(reader, start, end, "Digit not found");
		for (; i < end; i++) {
			final int ind = buf[i] - 48;
			if (ind < 0 || ind > 9) {
				if (ch == '+' && i > start + 1 && reader.allWhitespace(i, end)) return BigInteger.valueOf(value);
				else if (ch != '+' && i > start && reader.allWhitespace(i, end)) return BigInteger.valueOf(value);
				NumberConverter.numberException(reader, start, end, "Unknown digit", (char)ch);
			}
			value = (value << 3) + (value << 1) + ind;
		}
		return BigInteger.valueOf(value);
	}
}