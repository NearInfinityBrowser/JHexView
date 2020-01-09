/*
 * Added Oct. 2014 by argent77
 */
package tv.porst.jhexview;

import java.util.EventObject;

/**
 * HexViewEvent is used to notify of changed views or selections.
 */
public class HexViewEvent extends EventObject
{
  /** Available causes for this event. */
  public enum Cause {
    /** Specified if the cursor position changed or a selection has been made. */
    SelectionChanged,
    /** Specified if the current view has been changed. */
    ViewChanged
  }

  private final Cause cause;
  private final JHexView.Views view;
  private final long start, length;

  /**
   * Constructs a HexViewEvent, caused by changing the current view.
   * @param source The HexView component.
   * @param view The new view.
   */
  public HexViewEvent(Object source, JHexView.Views view)
  {
    super(source);
    this.cause = Cause.ViewChanged;
    this.view = view;
    this.start = this.length = 0;
  }

  /**
   * Constructs a HexViewEvent, caused by changing the selection.
   * @param source The HexView component.
   * @param start The selection start in nibbles (half of a byte).
   * @param length The selection length in nibbles.
   */
  public HexViewEvent(Object source, long start, long length)
  {
    super(source);
    this.cause = Cause.SelectionChanged;
    this.view = null;
    this.start = start;
    this.length = length;
  }

  /**
   * Returns the cause for this event.
   * @return The cause for this event.
   */
  public Cause getCause()
  {
    return cause;
  }

  /**
   * Returns the currently selected view if the cause is <code>ViewChanged</code>.
   * @return The currently selected view if the cause is <code>ViewChanged</code>.
   */
  public JHexView.Views getView()
  {
    return view;
  }

  /**
   * Returns the selection start (in nibbles) if the cause is <code>SelectionChanged</code>.
   * @return The selection start (in nibbles) if the cause is <code>SelectionChanged</code>.
   */
  public long getSelectionStart()
  {
    return start;
  }

  /**
   * Returns the selection length (in nibbles) if the cause is <code>SelectionChanged</code>.
   * @return The selection length (in nibbles) if the cause is <code>SelectionChanged</code>.
   */
  public long getSelectionLength()
  {
    return length;
  }
}
