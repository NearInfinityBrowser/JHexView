/*
 * Modified Oct. 2014 by argent77
 * - replaced first argument from byte array to byte
 */
package tv.porst.jhexview;

import java.awt.Color;

public interface IColormap
{
  /**
   * Determines whether the byte at the given offset should be colored or not.
   *
   * @param value
   *          The byte value that can be used to determine the return value.
   * @param currentOffset
   *          The absolute offset of the byte in question.
   *
   * @return True if the byte should be colored. False, otherwise.
   */
  boolean colorize(byte value, long currentOffset);

  /**
   * Returns the background color that should be used to color the byte at the
   * given offset.
   *
   * @param value
   *          The byte value that can be used to determine the return value.
   * @param currentOffset
   *          The absolute offset of the byte in question.
   *
   * @return The background color to be used by that byte. Null, if the default
   *         background color should be used,
   */
  Color getBackgroundColor(byte value, long currentOffset);

  /**
   * Returns the foreground color that should be used to color the byte at the
   * given offset.
   *
   * @param value
   *          The byte value that can be used to determine the return value.
   * @param currentOffset
   *          The absolute offset of the byte in question.
   *
   * @return The foreground color to be used by that byte. Null, if the default
   *         foreground color should be used,
   */
  Color getForegroundColor(byte value, long currentOffset);
}
