/*
 * Modified Oct. 2014 by argent77
 * - added single argument to dataChanged() prototype
 */
package tv.porst.jhexview;

import java.util.EventListener;

public interface IDataChangedListener extends EventListener
{
  void dataChanged(DataChangedEvent event);
}
