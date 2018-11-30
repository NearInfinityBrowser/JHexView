package tv.porst.jhexview;

import java.awt.Color;

public class ColoredRange implements Comparable<ColoredRange>
{
  private final Color fcolor;

  private final long start;

  private final int size;

  private final Color bgcolor;

  public ColoredRange(long start, int size, Color fcolor, Color bgcolor)
  {

    this.start = start;
    this.size = size;
    this.fcolor = fcolor;
    this.bgcolor = bgcolor;
  }

  @Override
  public int compareTo(ColoredRange arg0)
  {
    return (int) (start - arg0.start);
  }

  public boolean containsOffset(long offset)
  {
    return offset >= start && offset < start + size;
  }

  public Color getBackgroundColor()
  {
    return bgcolor;
  }

  public Color getColor()
  {
    return fcolor;
  }

  public int getSize()
  {
    return size;
  }

  public long getStart()
  {
    return start;
  }
}