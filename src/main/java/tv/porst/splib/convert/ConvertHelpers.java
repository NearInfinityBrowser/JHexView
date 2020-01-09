/*
 * Modified Oct. 2014 by argent77
 * - added new methods toByte() and toChar() with various parameter lists
 */
package tv.porst.splib.convert;

import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

/**
 * Converts helper functions for working with different data types.
 */
public final class ConvertHelpers
{
  private static final Charset DEFAULT_CHARSET = Charset.forName("US-ASCII");

  /**
   * Tests whether a given character is a valid decimal character.
   *
   * @param c
   *          The character to test.
   *
   * @return True, if the given character is a valid decimal character.
   */
  public static boolean isDecCharacter(final char c)
  {
    return c >= '0' && c <= '9';
  }

  /**
   * Tests whether a character is a valid character of a hexadecimal string.
   *
   * @param c
   *          The character to test.
   *
   * @return True, if the character is a hex character. False, otherwise.
   */
  public static boolean isHexCharacter(final char c)
  {
    return isDecCharacter(c) || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
  }

  /**
   * Tests whether a character is a printable ASCII character.
   *
   * @param c
   *          The character to test.
   *
   * @return True, if the character is a printable ASCII character. False,
   *         otherwise.
   */
  public static boolean isPrintableCharacter(final char c)
  {
    final Character.UnicodeBlock block = Character.UnicodeBlock.of(c);

    return !Character.isISOControl(c) && c != KeyEvent.CHAR_UNDEFINED && block != null
        && block != Character.UnicodeBlock.SPECIALS;
  }

  /**
   * Converts the character into a byte value, using the locale as defined by the OS.
   * @param c The character to convert.
   * @return The resulting byte value.
   */
  public static byte toByte(char c)
  {
    return toByte(new char[]{c}, DEFAULT_CHARSET)[0];
  }

  /**
   * Converts the character into a byte value, using the specified charset.
   * @param c The character to convert.
   * @param cs The Charset used for conversion.
   * @return The resulting byte value.
   */
  public static byte toByte(char c, Charset cs)
  {
    return toByte(new char[]{c}, cs)[0];
  }

  /**
   * Converts the char array into a byte array, using the locale as defined by the OS.
   * @param c The char array to convert.
   * @return The resulting byte array.
   */
  public static byte[] toByte(char[] c)
  {
    return toByte(c, DEFAULT_CHARSET);
  }

  /**
   * Converts the char array into a byte array, using the specified charset.
   * @param c The char array to convert.
   * @param cs The Charset used for conversion.
   * @return The resulting byte array.
   */
  public static byte[] toByte(char[] c, Charset cs)
  {
    if (c != null && c.length > 0) {
      if (cs == null) cs = DEFAULT_CHARSET;
      CharBuffer cb = CharBuffer.wrap(c);
      ByteBuffer bb = ByteBuffer.allocate(c.length);
      CharsetEncoder ce = cs.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
          .onUnmappableCharacter(CodingErrorAction.REPLACE);
      ce.encode(cb, bb, true);
      if (bb.hasArray()) {
        return bb.array();
      }
    }
    return new byte[0];
  }

  /**
   * Converts the byte value into a character representation,
   * using the locale as defined by the OS.
   * @param b The byte value to convert.
   * @return The character representation of the byte value.
   */
  public static char toChar(byte b)
  {
    return toChar(new byte[]{b}, DEFAULT_CHARSET)[0];
  }

  /**
   * Converts the byte value into a character representation, using the specified charset.
   * @param b The byte value to convert.
   * @param cs The Charset used for conversion.
   * @return The character representation of the byte value.
   */
  public static char toChar(byte b, Charset cs)
  {
    return toChar(new byte[]{b}, cs)[0];
  }

  /**
   * Converts the byte array into a character array, using the locale as defined by the OS.
   * @param b The byte array to convert.
   * @return The resulting char array of the same length as the byte array.
   */
  public static char[] toChar(byte[] b)
  {
    return toChar(b, DEFAULT_CHARSET);
  }

  /**
   * Converts the byte array into a character array, using the specified charset.
   * @param b The byte array to convert.
   * @param cs The Charset used for conversion.
   * @return The resulting char array of the same length as the byte array.
   */
  public static char[] toChar(byte[] b, Charset cs)
  {
    if (b != null && b.length > 0) {
      if (cs == null) cs = DEFAULT_CHARSET;
      ByteBuffer bb = ByteBuffer.wrap(b);
      CharBuffer cb = CharBuffer.allocate(b.length);
      CharsetDecoder cd = cs.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
          .onUnmappableCharacter(CodingErrorAction.REPLACE);
      cd.decode(bb, cb, true);
      if (cb.hasArray()) {
        return cb.array();
      }
    }
    return new char[0];
  }
}
