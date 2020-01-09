/*
 * Modified Oct. 2014 by argent77
 * - added JavaDoc comments
 * - removed prototype: byte[] getData()
 */
package tv.porst.jhexview;

public interface IDataProvider
{
  /**
   * Adds a listener to the DataProvider that will receive events whenever data has been changed.
   * @param listener The listener that will receive the DataChangedEvents.
   */
  void addListener(IDataChangedListener listener);

  /**
   * Returns a data segment of a given length as byte array.
   * @param offset The start offset of the data.
   * @param length The length of the data in bytes.
   * @return Requested data as byte array.
   */
  byte[] getData(long offset, int length);

  /**
   * Returns the data length in number of bytes.
   * @return The data length in number of bytes.
   */
  int getDataLength();

  /**
   * Returns whether the requested data segment is available.
   * @param offset The start offset of the requested data.
   * @param length The length of the requested data in bytes.
   * @return True if the requested data segment is available, false otherwise.
   */
  boolean hasData(long start, int length);

  /** Returns whether the data stream provided by this instance is writeable. */
  boolean isEditable();

  /**
   * Returns whether the data segment requested by the last call of {@link #hasData(long, int)}
   * is available.
   * @return True if data segment is not available. False if the data segment can be fetched by
   *         {@link #getData(long, int)}.
   */
  boolean keepTrying();

  /**
   * Removes a listener from the DataProvider.
   * @param listener The listener to remove.
   */
  void removeListener(IDataChangedListener listener);

  /**
   * Writes the specified data at the given offset. Note: Only works if {@link #isEditable()}
   * returns <code>true</code>.
   * @param offset The start offset for the data to be written.
   * @param data The data to write.
   */
  void setData(long offset, byte[] data);
}
