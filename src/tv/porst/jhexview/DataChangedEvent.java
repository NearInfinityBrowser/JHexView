/*
 * Added Oct. 2014 by argent77
 */
package tv.porst.jhexview;

import java.util.EventObject;

/**
 * A basic event implementation that indicates that the content of a IDataProvider object
 * has been modified.
 */
public class DataChangedEvent extends EventObject
{
  public DataChangedEvent(Object source)
  {
    super(source);
  }

}
