package ru.ancevt.util.args;

import java.util.ArrayList;
import java.util.List;

public class Args {

	public static void main(String[] args) {

		final String source = "test --debug-mode -p 22 --strict-mode true -t http --host  'lol ancevt.ru' --speed 120";

		Args a = new Args(source);

		String protocol = a.getString(new String[] { "--protocol", "-t" });
		String host = a.getString("--host", "-h");
		int port = a.getInt("--port", "-p");

		boolean strictMode = a.getBoolean("--strict-mode", "-s");
		double speed = a.getDouble("--speed");

		boolean debugMode = a.contains("--debug-mode", "-d");
		System.out.println("URL: " + protocol + "://" + host + ":" + port + "/");
		System.out.println("Debug mode: " + (debugMode ? "ON" : "OFF"));
		System.out.println("Connection speed: " + speed);
		System.out.println("Strict mode: " + strictMode);
	}

	private static final String SPACE_CHARS = "\n\t\r\b ";

	private static final int DEFAULT_INTEGER_VALUE = 0;
	private static final double DEFAULT_DOUBLE_VALUE = 0.0;
	private static final float DEFAULT_FLOAT_VALUE = 0.0f;
	private static final boolean DEFAULT_BOOLEAN_VALUE = false;
	private static final long DEFAULT_LONG_VALUE = 0L;
	private static final short DEFAULT_SHORT_VALUE = 0;
	private static final byte DEFAULT_BYTE_VALUE = 0;
	private static final String DEFAULT_STRING_VALUE = null;
	private static final char DEFAULT_CHAR_VALUE = '\0';

	private String[] args;

	private Args(String[] args) {
		this.args = args;
	}

	public Args(String source) {
		this(split(source));
	}

	private static String[] split(final String source) {
		final List<String> list = new ArrayList<>();
		final StringBuilder stringBuilder = new StringBuilder();
		int str = -1;
		int i = 0;
		final int len = source.length();
		while (i < len) {
			final char c = source.charAt(i++);
			if (c == '\\') {
				if (i == len)
					break;
				final char c2 = source.charAt(i++);
				stringBuilder.append(c2);
				continue;
			}
			if (str == -1) {
				if (c == '"' || c == '\'') {
					str = c;
					continue;
				}
				if (SPACE_CHARS.indexOf(c) != -1) {
					if (stringBuilder.length() != 0) {
						list.add(stringBuilder.toString());
						stringBuilder.setLength(0);
					}
					continue;
				}
			} else {
				if (c == str) {
					str = -1;
					continue;
				}
			}
			stringBuilder.append(c);
		}
		if (stringBuilder.length() != 0)
			list.add(stringBuilder.toString());

		return list.toArray(new String[] {});
	}
	
	public int size() {
		return args.length;
	}

	public boolean contains(String key) {
		for (String s : args)
			if (s.equals(key))
				return true;
		return false;
	}

	public boolean contains(String... keys) {
		for (String k : keys)
			if (contains(k))
				return true;
		return false;
	}
	
	public String getString(int index, String defaultValue) {
		return args.length <= index ? defaultValue : args[index];
	}
	
	public String getString(int index) {
		return getString(index, DEFAULT_STRING_VALUE);
	}

	public String getString(String key) {
		return getString(key, null);
	}

	public String getString(String key, String defaultValue) {
		try {
			for (int i = 0; i < args.length; i++) {
				String currentArgs = args[i];

				if (currentArgs.equals(key))
					return args[i + 1];
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			return defaultValue;
		}
		return defaultValue;
	}

	public String getString(String[] keys) {
		for (String k : keys) {
			String value = getString(k);
			if (value != null)
				return value;
		}
		return null;
	}
	
	public int getInt(int index, int defaultValue) {
		try {
			return args.length <= index ? defaultValue : Integer.parseInt(args[index]);
		} catch(NumberFormatException ex) {
			return DEFAULT_INTEGER_VALUE;
		}
	}
	
	public int getInt(int index) {
		return getInt(index, DEFAULT_INTEGER_VALUE);
	}

	public int getInt(String key) {
		return getInt(key, 0);
	}

	public int getInt(String key, int defaultValue) {
		try {
			return Integer.parseInt(getString(key, Integer.toString(defaultValue)));
		} catch (NumberFormatException ex) {
			return DEFAULT_INTEGER_VALUE;
		}
	}

	public int getInt(String... keys) {
		try {
			return Integer.parseInt(getString(keys));
		} catch (NumberFormatException ex) {
			return DEFAULT_INTEGER_VALUE;
		}
	}
	
	public short getShort(int index, short defaultValue) {
		try {
			return args.length <= index ? defaultValue : Short.parseShort(args[index]);
		} catch(NumberFormatException ex) {
			return DEFAULT_SHORT_VALUE;
		}
	}
	
	public int getShort(int index) {
		return getShort(index, DEFAULT_SHORT_VALUE);
	}

	public short getShort(String key) {
		return getShort(key, DEFAULT_SHORT_VALUE);
	}

	public short getShort(String key, short defaultValue) {
		try {
			return Short.parseShort(getString(key, Short.toString(defaultValue)));
		} catch (NumberFormatException ex) {
			return DEFAULT_SHORT_VALUE;
		}
	}

	public short getShort(String... keys) {
		try {
			return Short.parseShort(getString(keys));
		} catch (NumberFormatException ex) {
			return DEFAULT_SHORT_VALUE;
		}
	}
	
	public byte getByte(int index, byte defaultValue) {
		try {
			return args.length <= index ? defaultValue : Byte.parseByte(args[index]);
		} catch(NumberFormatException ex) {
			return DEFAULT_BYTE_VALUE;
		}
	}
	
	public byte getByte(int index) {
		return getByte(index, DEFAULT_BYTE_VALUE);
	}

	public byte getByte(String key) {
		return getByte(key, DEFAULT_BYTE_VALUE);
	}

	public byte getByte(String key, byte defaultValue) {
		try {
			return Byte.parseByte(getString(key, Byte.toString(defaultValue)));
		} catch (NumberFormatException ex) {
			return DEFAULT_BYTE_VALUE;
		}
	}

	public byte getByte(String... keys) {
		try {
			return Byte.parseByte(getString(keys));
		} catch (NumberFormatException ex) {
			return DEFAULT_BYTE_VALUE;
		}
	}
	
	public long getLong(int index, long defaultValue) {
		try {
			return args.length <= index ? defaultValue : Long.parseLong(args[index]);
		} catch(NumberFormatException ex) {
			return DEFAULT_LONG_VALUE;
		}
	}
	
	public long getLong(int index) {
		return getLong(index, DEFAULT_LONG_VALUE);
	}

	public long getLong(String key) {
		return getLong(key, DEFAULT_LONG_VALUE);
	}

	public long getLong(String key, long defaultValue) {
		try {
			return Long.parseLong(getString(key, Long.toString(defaultValue)));
		} catch (NumberFormatException ex) {
			return DEFAULT_LONG_VALUE;
		}
	}

	public long getLong(String... keys) {
		try {
			return Long.parseLong(getString(keys));
		} catch (NumberFormatException ex) {
			return DEFAULT_LONG_VALUE;
		}
	}

	public double getDouble(int index, double defaultValue) {
		try {
			return args.length <= index ? defaultValue : Double.parseDouble(args[index]);
		} catch(NumberFormatException ex) {
			return DEFAULT_DOUBLE_VALUE;
		}
	}
	
	public double getDouble(int index) {
		return getDouble(index, DEFAULT_DOUBLE_VALUE);
	}

	public double getDouble(String key) {
		return getDouble(key, DEFAULT_DOUBLE_VALUE);
	}

	public double getDouble(String key, double defaultValue) {
		try {
			return Double.parseDouble(getString(key, Double.toString(defaultValue)));
		} catch (NullPointerException ex) {
			return DEFAULT_DOUBLE_VALUE;
		} catch (NumberFormatException ex) {
			return DEFAULT_DOUBLE_VALUE;
		}
	}

	public double getDouble(String... keys) {
		try {
			return Double.parseDouble(getString(keys));
		} catch (NullPointerException ex) {
			return DEFAULT_DOUBLE_VALUE;
		} catch (NumberFormatException ex) {
			return DEFAULT_DOUBLE_VALUE;
		}
	}
	
	public float getFloat(int index, float defaultValue) {
		try {
			return args.length <= index ? defaultValue : Float.parseFloat(args[index]);
		} catch(NumberFormatException ex) {
			return DEFAULT_FLOAT_VALUE;
		}
	}
	
	public float getFloat(int index) {
		return getFloat(index, DEFAULT_FLOAT_VALUE);
	}

	public float getFloat(String key) {
		return getFloat(key, 0.0f);
	}

	public float getFloat(String key, float defaultValue) {
		try {
			return Float.parseFloat(getString(key, Float.toString(defaultValue)));
		} catch (NullPointerException ex) {
			return DEFAULT_FLOAT_VALUE;
		} catch (NumberFormatException ex) {
			return DEFAULT_FLOAT_VALUE;
		}
	}

	public float getFloat(String... key) {
		try {
			return Float.parseFloat(getString(key));
		} catch (NullPointerException ex) {
			return DEFAULT_FLOAT_VALUE;
		} catch (NumberFormatException ex) {
			return DEFAULT_FLOAT_VALUE;
		}
	}
	
	public boolean getBoolean(int index, boolean defaultValue) {
		return args.length <= index ? defaultValue : Boolean.parseBoolean(getString(args[index], Boolean.toString(defaultValue)));
	}
	
	public boolean getBoolean(int index) {
		return getBoolean(index, DEFAULT_BOOLEAN_VALUE);
	}

	public boolean getBoolean(String key) {
		return getBoolean(key, DEFAULT_BOOLEAN_VALUE);
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		return Boolean.parseBoolean(getString(key, Boolean.toString(defaultValue)));
	}

	public boolean getBoolean(String... keys) {
		return Boolean.parseBoolean(getString(keys));
	}
	
	public char getChar(int index, char defaultValue) {
		final String string = getString(index, String.valueOf(defaultValue));
		return string != null && string.length() > 0 ? string.charAt(0) : DEFAULT_CHAR_VALUE; 
	}
	
	public char getChar(int index) {
		return getChar(index, DEFAULT_CHAR_VALUE);
	}

	public char getChar(String key) {
		return getChar(key, DEFAULT_CHAR_VALUE);
	}

	public char getChar(String key, char defaultValue) {
		final String string = getString(key, String.valueOf(defaultValue));
		return string != null && string.length() > 0 ? string.charAt(0) : DEFAULT_CHAR_VALUE; 
	}

	public char getChar(String... keys) {
		final String string = getString(keys);
		return string != null && string.length() > 0 ? string.charAt(0) : DEFAULT_CHAR_VALUE; 
	}

	@Override
	public String toString() {
		String result = new String();

		for (int i = 0; i < args.length; i++) {
			result += args[i];
		}

		return result;
	}

}
