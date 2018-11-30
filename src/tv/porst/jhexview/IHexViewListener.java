/*
 * Modified Oct. 2014 by argent77
 * - replaced arguments with single HexViewEvent argument
 */
package tv.porst.jhexview;

import java.util.EventListener;

public interface IHexViewListener extends EventListener
{
  void stateChanged(HexViewEvent event);
}
