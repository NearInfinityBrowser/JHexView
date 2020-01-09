/*
 * JHexView component by Sebastian Porst (sp@porst.tv)
 * Modified Oct. 2014 by argent77
 * - removed usage of separate offscreen image
 * - added header row
 * - added more color definitions
 * - changed colored block to fixed positions
 * - added support for Undo/Redo, Copy/Paste, Find bytes/text
 * - added visual feedback for modified data
 */
package tv.porst.jhexview;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.event.EventListenerList;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import tv.porst.splib.convert.ConvertHelpers;
import tv.porst.splib.gui.GuiHelpers;
import tv.porst.splib.gui.caret.ICaretListener;
import tv.porst.splib.gui.caret.JCaret;

/**
 * The JHexView component is a Java component that can be used to display data
 * in hexadecimal format.
 *
 * @author sp
 * @author argent77
 *
 */
public final class JHexView extends JComponent
{
  private static final long serialVersionUID = -2402458562501988128L;

  /**
   * Two characters are needed to display a byte in the hex window.
   */
  private static final int CHARACTERS_PER_BYTE = 2;

  /**
   * Lookup table to convert byte values into printable strings.
   */
  private static final String[] HEX_BYTES = {
    "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F",
    "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1A", "1B", "1C", "1D", "1E", "1F",
    "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "2A", "2B", "2C", "2D", "2E", "2F",
    "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "3A", "3B", "3C", "3D", "3E", "3F",
    "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "4A", "4B", "4C", "4D", "4E", "4F",
    "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "5A", "5B", "5C", "5D", "5E", "5F",
    "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "6A", "6B", "6C", "6D", "6E", "6F",
    "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "7A", "7B", "7C", "7D", "7E", "7F",
    "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "8A", "8B", "8C", "8D", "8E", "8F",
    "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "9A", "9B", "9C", "9D", "9E", "9F",
    "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "AA", "AB", "AC", "AD", "AE", "AF",
    "B0", "B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9", "BA", "BB", "BC", "BD", "BE", "BF",
    "C0", "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "CA", "CB", "CC", "CD", "CE", "CF",
    "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "DA", "DB", "DC", "DD", "DE", "DF",
    "E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9", "EA", "EB", "EC", "ED", "EE", "EF",
    "F0", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "FA", "FB", "FC", "FD", "FE", "FF" };

  private static final int PADDING_OFFSETVIEW = 20;

  private static final int NIBBLES_PER_BYTE = 2;

  /**
   * A stroke definition used for showing a hint box in the view that doesn't currently has
   * the input focus.
   */
  private static final Stroke DOTTED_STROKE =
      new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                      1.0f, new float[]{1.0f}, 0.0f);

  /**
   * List containing listeners of all supported types.
   */
  private final EventListenerList m_listeners = new EventListenerList();

  /**
   * Stores offset values that have been modified by the user.
   * The value indicates how often the data at the position has been modified. (Important when
   * undoing actions).
   */
  private final TreeMap<Long, Integer> m_modifiedOffsets = new TreeMap<Long, Integer>();

  /**
   * Manages the undo/redo functionality
   */
  private final UndoManager m_undo = new UndoManager();

  /**
   * The data set that is displayed in the component.
   */
  private IDataProvider m_dataProvider;

  /**
   * Number of bytes shown per row.
   */
  private int m_bytesPerRow = 16;

  /**
   * Font used to draw the data.
   */
  private Font m_font = new Font(GuiHelpers.getMonospaceFont(), 0, 12);

  /**
   * Currently selected position. Note that this field is twice as large as the
   * length of data because nibbles can be selected.
   */
  private long m_selectionStart = 0;

  /**
   * Current selection length in nibbles. This value can be negative if nibbles
   * before the current position are selected.
   */
  private long m_selectionLength = 0;

  /**
   * Determines the window where the caret is shown.
   */
  private Views m_activeView = Views.HEX_VIEW;

  /**
   * Width of the hex view in pixels.
   */
  private int m_hexViewWidth = 270;

  /**
   * Width of the space between columns in pixels.
   */
  private int m_columnSpacing = 4;

  /**
   * Number of bytes per column.
   */
  private int m_bytesPerColumn = 2;

  /**
   * Background color of the header view.
   */
  private Color m_bgColorHeader = Color.WHITE;

  /**
   * Background color of the offset view.
   */
  private Color m_bgColorOffset = Color.GRAY;

  /**
   * Background color of the hex view.
   */
  private Color m_bgColorHex = Color.WHITE;

  /**
   * Background color of the ASCII view.
   */
  private Color m_bgColorAscii = Color.WHITE;

  /**
   * Font color of the header view.
   */
  private Color m_fontColorHeader = new Color(0x0000BF);

  /**
   * Font color of the offset view.
   */
  private Color m_fontColorOffsets = Color.WHITE;

  /**
   * Font color of the hex view.
   */
  private Color m_fontColorHex1 = Color.BLUE;

  /**
   * Font color of the hex view.
   */
  private Color m_fontColorHex2 = new Color(0x3399FF);

  /**
   * Font color of the ASCII view.
   */
  private Color m_fontColorAscii = new Color(0x339900);

  /**
   * Font color for data that has been modified by the user
   */
  private Color m_fontColorModified = Color.RED;

  /**
   * Used to store the height of a single row. This value is updated every time
   * the component is drawn.
   */
  private int m_rowHeight = 12;

  /**
   * Used to store the width of a single character. This value is updated every
   * time the component is drawn.
   */
  private int m_charWidth = 8;

  /**
   * Scrollbar that is used to scroll through the dataset.
   */
  private final JScrollBar m_scrollbar = new JScrollBar(JScrollBar.VERTICAL, 0, 1, 0, 1);

  /**
   * Horizontal scrollbar that is used to scroll through the dataset.
   */
  private final JScrollBar m_horizontalScrollbar = new JScrollBar(JScrollBar.HORIZONTAL, 0, 1, 0, 1);

  /**
   * The first visible row.
   */
  private int m_firstRow = 0;

  /**
   * The first visible column.
   */
  private int m_firstColumn = 0;

  /**
   * Address of the first offset in the data set.
   */
  private long m_baseAddress = 0;

  /**
   * Last x-coordinate of the mouse cursor in the component.
   */
  private int m_lastMouseX = 0;

  /**
   * Last y-coordinate of the mouse cursor in the component.
   */
  private int m_lastMouseY = 0;

  /**
   * Flag that determines whether the component reacts to user input or not.
   */
  private boolean m_enabled = false;

  /**
   * Blinking caret of the component.
   */
  private JCaret m_caret = new JCaret();

  /**
   * Color that is used to draw all text in disabled components.
   */
  private final Color m_disabledColor = Color.GRAY;

  /**
   * Left-padding of the hex view in pixels.
   */
  private final int m_paddingHexLeft = 10;

  /**
   * Left-padding of the ASCII view in pixels.
   */
  private final int m_paddingAsciiLeft = 10;

  /**
   * Top-padding of all views in pixels.
   */
  private final int m_paddingTop = 16;

  /**
   * Height of a drawn character in the component.
   */
  private int m_charHeight = 8;

  /**
   * Maximum positive height (ascent) of a drawn character in the component.
   */
  private int m_charMaxAscent = 8;

  /**
   * Maximum negative height (descent) of a drawn character in the component.
   */
  private int m_charMaxDescent = 3;

  /**
   * Color that is used to highlight data when the mouse cursor hovers of the
   * data.
   */
  private final Color m_colorHighlight = Color.LIGHT_GRAY;

  /**
   * Start with an undefined definition status.
   */
  private DefinitionStatus m_status = DefinitionStatus.UNDEFINED;

  /**
   * The menu creator is used to create popup menus when the user right-clicks
   * on the hex view control.
   */
  private IMenuCreator m_menuCreator;

  /**
   * Current addressing mode (32bit or 64bit)
   */
  private AddressMode m_addressMode = AddressMode.BIT32;

  /**
   * Width of the offset view part of the component.
   */
  private int m_offsetViewWidth;

  /**
   * Manager that keeps track of specially colored byte ranges.
   */
  private final ColoredRangeManager[] m_coloredRanges = new ColoredRangeManager[10];

  /**
   * Timer that is used to refresh the component if no data for the selected
   * range is available.
   */
  private Timer m_updateTimer;

  /**
   * Flag that indicates whether the component is being drawn for the first
   * time.
   */
  private boolean m_firstDraw = true;

  /**
   * Default internal listener that is used to handle various events.
   */
  private final InternalListener m_listener = new InternalListener();

  /**
   * Action that's executed when the user presses the left arrow key.
   */
  private final ActionLeft m_leftAction = new ActionLeft(0);

  /**
   * Action that's executed when the user presses the shift+left arrow key.
   */
  private final ActionLeft m_shiftLeftAction = new ActionLeft(InputEvent.SHIFT_DOWN_MASK);

  /**
   * Action that's executed when the user presses the right arrow key.
   */
  private final ActionRight m_rightAction = new ActionRight(0);

  /**
   * Action that's executed when the user presses the shift+right arrow key.
   */
  private final ActionRight m_shiftRightAction = new ActionRight(InputEvent.SHIFT_DOWN_MASK);

  /**
   * Action that's executed when the user presses the up arrow key.
   */
  private final ActionUp m_upAction = new ActionUp();

  /**
   * Action that's executed when the user presses the down arrow key.
   */
  private final ActionDown m_downAction = new ActionDown();

  /**
   * Action that's executed when the user presses the page up key.
   */
  private final ActionPageUp m_pageUpAction = new ActionPageUp();

  /**
   * Action that's executed when the user presses the page down key.
   */
  private final ActionPageDown m_pageDownAction = new ActionPageDown();

  /**
   * Action that's executed when the user presses the home key.
   */
  private final ActionHome m_homeLineAction = new ActionHome(false);

  /**
   * Action that's executed when the user presses the ctrl+home key.
   */
  private final ActionHome m_homeDocAction = new ActionHome(true);

  /**
   * Action that's executed when the user presses the end key.
   */
  private final ActionEnd m_endLineAction = new ActionEnd(false);

  /**
   * Action that's executed when the user presses the ctrl+end key.
   */
  private final ActionEnd m_endDocAction = new ActionEnd(true);

  /**
   * Action that's executed when the user presses the tab key.
   */
  private final ActionTab m_tabAction = new ActionTab();

  /**
   * Action that's executed when the user presses the shortcut ctrl+A.
   */
  private final ActionShortcut m_SelectAllAction =
      new ActionShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                                                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

  /**
   * Action that's executed when the user presses the shortcut ctrl+V.
   */
  private final ActionShortcut m_PasteTextAction =
      new ActionShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                                                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

  /**
   * Action that's executed when the user presses the shortcut ctrl+C.
   */
  private final ActionShortcut m_CopyTextAction =
      new ActionShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                                                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

  /**
   * Action that's executed when the user presses the shortcut ctrl+Z.
   */
  private final ActionShortcut m_UndoAction =
      new ActionShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                                                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

  /**
   * Action that's executed when the user presses the shortcut ctrl+Y.
   */
  private final ActionShortcut m_RedoAction =
      new ActionShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                                                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

  private int m_lastHighlightedNibble;

  /**
   * Determines whether to draw a title header.
   */
  private boolean m_headerVisible = true;

  /**
   * Defines the font style for the header text.
   */
  private int m_headerFontStyle = Font.PLAIN;

  /**
   * Determines whether to use an assigned color map to colorize data.
   */
  private boolean m_colorMapEnabled = true;

  /**
   * Determines whether to draw vertical lines between the individual views.
   */
  private boolean m_separatorsVisible = true;

  /**
   * Defines whether to show data that has been modified by the user in a separate color.
   */
  private boolean m_showModified = false;

  /**
   * Determines whether to highlight the byte under the mouse cursor.
   */
  private boolean m_mouseOverHighlighted = true;

  private IColormap m_colormap;

  private Color m_selectionColor = Color.YELLOW;

  /**
   * Determines whether the bytes inside a column are flipped or not.
   */
  private boolean m_flipBytes = false;

  /**
   * Creates a new hex viewer.
   */
  public JHexView()
  {
    setDoubleBuffered(true);

    // Setting default colors for undefined areas
    setBackground(Color.WHITE);
    setForeground(Color.BLACK);

    for (int i = 0; i < m_coloredRanges.length; i++) {
      m_coloredRanges[i] = new ColoredRangeManager();
    }

    // Necessary to receive input
    setFocusable(true);

    setLayout(new BorderLayout());

    // Set the initial font
    setFont(m_font);

    initListeners();

    initHotkeys();

    initScrollbar();

    setTransferHandler(new HexTransferHandler());

    setScrollBarMaximum();

    updateOffsetViewWidth();

    // By default, this component is disabled.
    setEnabled(false);
  }

  /**
   * Calculates current character and row sizes.
   */
  private void calculateSizes()
  {
    Graphics g = getGraphics();
    if (g != null) {
      try {
        m_rowHeight = getRowHeight(g);
        m_charHeight = getCharAscent(g);
        m_charMaxAscent = getCharMaxAscent(g);
        m_charMaxDescent = getCharMaxDescent(g);
        m_charWidth = getCharacterWidth(g);
      } finally {
        g.dispose();
        g = null;
      }
    }
  }

  private void changeBy(final ActionEvent event, final int length)
  {
    if ((event.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK) {
      if (getSelectionStart() + getSelectionLength() + length < 0) {
        setSelectionLength(-getSelectionStart());
      }
      else {
        if (getSelectionStart() + getSelectionLength() + length < 2 * m_dataProvider
            .getDataLength()) {
          setSelectionLength(getSelectionLength() + length);
        }
        else {
          setSelectionLength(2 * m_dataProvider.getDataLength() - getSelectionStart());
        }
      }
    }
    else {
      if (getSelectionStart() + getSelectionLength() + length < 0) {
        setSelectionStart(0);
      }
      else if (getSelectionStart() + getSelectionLength() + length < 2 * m_dataProvider
          .getDataLength()) {
        setSelectionStart(getSelectionStart() + getSelectionLength() + length);
      }
      else {
        setSelectionStart(2 * m_dataProvider.getDataLength());
      }

      setSelectionLength(0);
    }

    final long newPosition = getSelectionStart() + getSelectionLength();

    if (newPosition < 2 * getFirstVisibleByte()) {
      scrollToPosition(newPosition);
    }
    else if (newPosition >= 2 * (getFirstVisibleByte() + getMaximumVisibleBytes())) {

      final long invisibleNibbles = newPosition - 2
          * (getFirstVisibleByte() + getMaximumVisibleBytes());

      final long scrollpos = 2 * getFirstVisibleByte() + 2 * m_bytesPerRow + invisibleNibbles;

      scrollToPosition(scrollpos);
    }

    m_caret.setVisible(true);
    repaint();
  }

  /**
   * Decrements the number of modifications to the data at the specified offset or removes it completely.
   * @param offset The position of data that has been modified by the user.
   * @param forceRemove If true, remove the offset regardless of how many times it has been modified.
   * @return True if the data at this offset had been modified, false otherwise.
   */
  private boolean clearModified(long offset, boolean forceRemove)
  {
    Long key = Long.valueOf(offset);
    Integer value = m_modifiedOffsets.get(key);
    if (value != null) {
      if (value.intValue() > 1 && forceRemove == false) {
        value = Integer.valueOf(value.intValue() - 1);
        m_modifiedOffsets.put(key, value);
      } else {
        m_modifiedOffsets.remove(key);
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Draws the content of the ASCII panel.
   *
   * @param g
   *          The graphics context of the hex panel.
   */
  private void drawAsciiPanel(final Graphics g)
  {
    final int characterWidth = getCharacterWidth(g);

    final int initx = getAsciiViewLeft() + m_paddingAsciiLeft;

    int x = initx;
    int y = m_paddingTop + getHeaderHeight();

    // Drawing offset title
    if (m_headerVisible) {
      Font oldFont = getFont();
      g.setFont(oldFont.deriveFont(m_headerFontStyle));
      g.setColor(m_fontColorHeader);
      String title = getHeaderTitleAscii(m_addressMode);
      g.drawString(title, x, m_paddingTop);
      g.setFont(oldFont);
    }

    if (isEnabled()) {
      // Choose the right color for the ASCII view
      g.setColor(m_fontColorAscii);
    }
    else {
      g.setColor(m_disabledColor != m_bgColorAscii ? m_disabledColor : Color.WHITE);
    }

    byte[] data = null;
    char[] chars = null;
    int bytesToDraw;

    if (m_status == DefinitionStatus.DEFINED) {
      bytesToDraw = getBytesToDraw();
      data = m_dataProvider.getData(getFirstVisibleOffset(), bytesToDraw);
      chars = ConvertHelpers.toChar(data);
    }
    else {
      bytesToDraw = getMaximumVisibleBytes();
    }

    long currentOffset = getFirstVisibleOffset();

    for (int i = 0; i < bytesToDraw; i++, currentOffset++) {
      ColoredRange range = findColoredRange(currentOffset);

      if (range != null && currentOffset + bytesToDraw < range.getStart()) {
        range = null;
      }

      if (i != 0 && i % m_bytesPerRow == 0) {
        // If the end of a row is reached, reset the
        // x-coordinate and increase the y-coordinate.
        x = initx;
        y += m_rowHeight;
      }

      if (m_status == DefinitionStatus.DEFINED) {
        char c = chars[i];
        c = ConvertHelpers.isPrintableCharacter(c) ? c : '.';

        final String dataString = String.valueOf(c);

        if (isEnabled()) {
          // Fixed: Highlighting in debugger memory window is wrong in regards
          // to the endianess selected
          final long normalizedOffset = m_flipBytes ? (currentOffset & -m_bytesPerColumn)
              + m_bytesPerColumn - (currentOffset % m_bytesPerColumn) - 1 : currentOffset;

          if (isSelectedOffset(normalizedOffset)) {

            g.setColor(m_selectionColor);
            g.fillRect(x, y - m_charMaxAscent, m_charWidth, m_charMaxAscent + m_charMaxDescent);

            // Choose the right color for the ASCII view
            if (isShowModified() && isModified(currentOffset)) {
              g.setColor(m_fontColorModified);
            } else {
              g.setColor(m_fontColorAscii);
            }
          }
          else if (range != null && range.containsOffset(currentOffset)) {
            final Color bgColor = range.getBackgroundColor();

            if (bgColor != null) {
              g.setColor(bgColor);
            }

            g.fillRect(x, y - m_charMaxAscent, m_charWidth, m_charMaxAscent + m_charMaxDescent);
            g.setColor(range.getColor());
          }
          else if (m_colorMapEnabled && m_colormap != null && m_colormap.colorize(data[i], currentOffset)) {
            final Color backgroundColor = m_colormap.getBackgroundColor(data[i], currentOffset);
            final Color foregroundColor;
            if (isShowModified() && isModified(currentOffset)) {
              foregroundColor = m_fontColorModified;
            } else {
              foregroundColor = m_colormap.getForegroundColor(data[i], currentOffset);
            }

            if (backgroundColor != null) {
              g.setColor(backgroundColor);
              g.fillRect(x, y - m_charMaxAscent, m_charWidth, m_charMaxAscent + m_charMaxDescent);
            }

            if (foregroundColor != null) {
              g.setColor(foregroundColor);
            } else {
              g.setColor(m_fontColorAscii);
            }
          }
          else {
            // Choose the right color for the ASCII view
            if (isShowModified() && isModified(currentOffset)) {
              g.setColor(m_fontColorModified);
            } else {
              g.setColor(m_fontColorAscii);
            }
          }

        }
        else {
          g.setColor(m_disabledColor != m_bgColorAscii ? m_disabledColor : Color.WHITE);
        }

        g.drawString(dataString, x, y);
      }
      else {
        g.drawString("?", x, y);
      }

      x += characterWidth;

      if (range != null && range.getStart() + range.getSize() <= currentOffset) {
        range = findColoredRange(currentOffset);

        if (range != null && currentOffset + bytesToDraw < range.getStart()) {
          range = null;
        }
      }
    }
  }

  /**
   * Draws the background of the hex panel.
   *
   * @param g
   *          The graphics context of the hex panel.
   */
  private void drawBackground(final Graphics g)
  {
    int x, y, w, h;

    // clearing whole component
    g.setColor(getBackground());
    x = y = 0; w = getWidth(); h = getHeight();
    g.fillRect(x, y, w, h);

    // Draw the background of the header view
    if (m_headerVisible) {
      g.setColor(m_bgColorHeader);
      x = -m_firstColumn * m_charWidth;
      y = 0;
      int asciiWidth = m_firstColumn * m_charWidth + getWidth()
                       - (m_hexViewWidth + m_offsetViewWidth) - m_scrollbar.getWidth();
      w = m_offsetViewWidth + m_hexViewWidth + asciiWidth;
      h = getHeaderHeight();
      g.fillRect(x, y, w, h);
    }

    // Draw the background of the offset view
    g.setColor(m_bgColorOffset);
    x = -m_firstColumn * m_charWidth;
    y = getHeaderHeight();
    w = m_offsetViewWidth;
    h = getHeight();
    g.fillRect(x, y, w, h);

    // Draw the background of the hex view
    g.setColor(m_bgColorHex);
    g.fillRect(-m_firstColumn * m_charWidth + m_offsetViewWidth, y, m_hexViewWidth, getHeight());

    // Draw the background of the ASCII view
    g.setColor(m_bgColorAscii);
    x = -m_firstColumn * m_charWidth + m_hexViewWidth + m_offsetViewWidth;
    y = getHeaderHeight();
//    w = m_firstColumn * m_charWidth + getWidth() - (m_hexViewWidth + m_offsetViewWidth) - m_scrollbar.getWidth();
    w = m_bytesPerRow * m_charWidth + 2*m_paddingAsciiLeft;
    h = getHeight() - m_horizontalScrollbar.getHeight();
    g.fillRect(x, y, w, h);

    // Draw the lines that separate the individual views
    if (m_separatorsVisible) {
      g.setColor(Color.BLACK);
      g.drawLine(-m_firstColumn * m_charWidth + m_offsetViewWidth, y, -m_firstColumn * m_charWidth
                 + m_offsetViewWidth, getHeight());
      g.drawLine(-m_firstColumn * m_charWidth + m_offsetViewWidth + m_hexViewWidth, y, -m_firstColumn
                 * m_charWidth + m_offsetViewWidth + m_hexViewWidth, getHeight());
    }
  }

  /**
   * Draws the caret.
   *
   * @param g
   */
  private void drawCaret(final Graphics g)
  {
    if (!isEnabled()) {
      return;
    }

    if (getCurrentOffset() < getFirstVisibleByte()
        || getCurrentColumn() > getFirstVisibleByte() + getMaximumVisibleBytes()) {
      return;
    }

    final int characterSize = getCharacterWidth(g);

    if (m_activeView == Views.HEX_VIEW) {
      drawCaretHexWindow(g, characterSize, m_rowHeight, false);
      drawCaretAsciiWindow(g, characterSize, m_rowHeight, true);
    }
    else {
      drawCaretAsciiWindow(g, characterSize, m_rowHeight, false);
      drawCaretHexWindow(g, characterSize, m_rowHeight, true);
    }

  }

  /**
   * Draws the caret in the ASCII window.
   *
   * @param g
   *          The graphic context of the ASCII panel.
   * @param characterWidth
   *          The width of a single character.
   * @param characterHeight
   *          The height of a single character.
   * @param showHint
   *          If true, show a hint box instead of the caret.
   */
  private void drawCaretAsciiWindow(final Graphics g, final int characterWidth,
                                    final int characterHeight, boolean showHint)
  {
    final int currentRow = getCurrentRow() - m_firstRow;
    final int currentColumn = getCurrentColumn();
    final int currentCharacter = currentColumn / 2;

    // Calculate the position of the first character in the row
    final int startLeft = 9 + m_offsetViewWidth + m_hexViewWidth;

    // Calculate the position of the current character in the row
    final int x = -m_firstColumn * m_charWidth + startLeft + currentCharacter * characterWidth;

    // Calculate the position of the row
    final int y = m_paddingTop + getHeaderHeight() - m_charHeight + characterHeight * currentRow;

    if (showHint) {
      Graphics2D g2 = (Graphics2D)g;
      Stroke oldStroke = g2.getStroke();
      g2.setStroke(DOTTED_STROKE);
      g2.drawRect(x, y, characterWidth, characterHeight);
      g2.setStroke(oldStroke);
    } else {
      if (m_caret.isVisible()) {
        m_caret.draw(g, x, y, characterHeight);
      }
    }
  }

  /**
   * Draws the caret in the hex window.
   *
   * @param g
   *          The graphic context of the hex panel.
   * @param characterWidth
   *          The width of a single character.
   * @param characterHeight
   *          The height of a single character.
   * @param showHint
   *          If true, show a hint box instead of the caret.
   */
  private void drawCaretHexWindow(final Graphics g, final int characterWidth,
                                  final int characterHeight, boolean showHint)
  {
    final int currentRow = getCurrentRow() - m_firstRow;
    final int currentColumn = getCurrentColumn();

    // Calculate the position of the first character in the row.
    final int startLeft = 9 + m_offsetViewWidth;

    // Calculate the extra padding between columns.
    final int paddingColumns = currentColumn / (2 * m_bytesPerColumn) * m_columnSpacing;

    // Calculate the position of the character in the row.
    final int x = -m_firstColumn * m_charWidth + startLeft + currentColumn * characterWidth
                  + paddingColumns;

    // Calculate the position of the row.
    final int y = m_paddingTop + getHeaderHeight() - m_charHeight + characterHeight * currentRow;

    if (showHint) {
      Graphics2D g2 = (Graphics2D)g;
      Stroke oldStroke = g2.getStroke();
      g2.setStroke(DOTTED_STROKE);
      g2.drawRect(x, y, characterWidth*2+1, characterHeight);
      g2.setStroke(oldStroke);
    } else {
      if (m_caret.isVisible()) {
        m_caret.draw(g, x, y, characterHeight);
      }
    }
  }

  /**
   * Draws the content of the hex view.
   *
   * @param g
   *          The graphics context of the hex panel.
   */
  private void drawHexView(final Graphics g)
  {
    final int standardSize = 2 * getCharacterWidth(g);

    final int firstX = -m_firstColumn * m_charWidth + m_paddingHexLeft + m_offsetViewWidth;

    // drawing hex title
    if (m_headerVisible) {
      Font oldFont = getFont();
      g.setFont(oldFont.deriveFont(m_headerFontStyle));
      g.setColor(m_fontColorHeader);
      int x = firstX;
      for (int i = 0; i < m_bytesPerRow; i++) {
        if (i != 0) {
          if (i % m_bytesPerColumn == 0) {
            x += m_columnSpacing;
          }
        }
        g.drawString(HEX_BYTES[i & 0xFF], x, m_paddingTop);
        x += standardSize;
      }
      g.setFont(oldFont);
    }

    int x = firstX;
    int y = m_paddingTop + getHeaderHeight();

    boolean evenColumn = true;

    byte[] data = null;
    int bytesToDraw;

    if (m_status == DefinitionStatus.DEFINED) {
      bytesToDraw = getBytesToDraw();
      data = m_dataProvider.getData(getFirstVisibleOffset(), bytesToDraw);
    }
    else {
      bytesToDraw = getMaximumVisibleBytes();
    }

    long currentOffset = getFirstVisibleOffset();

    // Iterate over all bytes in the data set and
    // print their hex value to the hex view.
    for (int i = 0; i < bytesToDraw; i++, currentOffset++) {
      final ColoredRange range = findColoredRange(currentOffset);

      if (i != 0) {
        if (i % m_bytesPerRow == 0) {
          // If the end of a row was reached, reset the x-coordinate
          // and set the y-coordinate to the next row.

          x = firstX;
          y += m_rowHeight;

          evenColumn = true;
        }
        else if (i % m_bytesPerColumn == 0) {
          // Add some spacing after each column.
          x += m_columnSpacing;

          evenColumn = !evenColumn;
        }
      }

      if (isEnabled()) {
        // determine whether to colorize additional horizontal space before or after the value
        int preSpaceX = 0, postSpaceX = 0;
        if (i % m_bytesPerColumn == 0) {
          preSpaceX = m_columnSpacing / 2;
        }
        if (i % m_bytesPerColumn == m_bytesPerColumn - 1) {
          postSpaceX = m_columnSpacing / 2;
        }

        if (isSelectedOffset(currentOffset)) {
          g.setColor(m_selectionColor);
          g.fillRect(x - preSpaceX, y - m_charMaxAscent,
                     2 * m_charWidth + preSpaceX + postSpaceX, m_charMaxAscent + m_charMaxDescent);

          // Choose the right color for the hex view
          g.setColor(evenColumn ? m_fontColorHex1 : m_fontColorHex2);
        }
        else if (range != null && range.containsOffset(currentOffset)) {
          final Color bgColor = range.getBackgroundColor();

          if (bgColor != null) {
            g.setColor(bgColor);
          }

          g.fillRect(x - preSpaceX, y - m_charMaxAscent,
                     2 * m_charWidth + preSpaceX + postSpaceX, m_charMaxAscent + m_charMaxDescent);
          g.setColor(range.getColor());
        }
        else {
          if (m_colorMapEnabled && m_colormap != null && m_colormap.colorize(data[i], currentOffset)) {
            final Color backgroundColor = m_colormap.getBackgroundColor(data[i], currentOffset);
            final Color foregroundColor;
            if (isShowModified() && isModified(currentOffset)) {
              foregroundColor = m_fontColorModified;
            } else {
              foregroundColor = m_colormap.getForegroundColor(data[i], currentOffset);
            }

            if (backgroundColor != null) {
              g.setColor(backgroundColor);
              g.fillRect(x - preSpaceX, y - m_charMaxAscent,
                         2 * m_charWidth + preSpaceX + postSpaceX, m_charMaxAscent + m_charMaxDescent);
            }

            if (foregroundColor != null) {
              g.setColor(foregroundColor);
            } else {
              g.setColor(evenColumn ? m_fontColorHex1 : m_fontColorHex2);
            }
          }
          else {
            // Choose the right color for the hex view
            if (isShowModified() && isModified(currentOffset)) {
              g.setColor(m_fontColorModified);
            } else {
              g.setColor(evenColumn ? m_fontColorHex1 : m_fontColorHex2);
            }
          }
        }
      }
      else {
        g.setColor(m_disabledColor != m_bgColorHex ? m_disabledColor : Color.WHITE);
      }

      if (m_status == DefinitionStatus.DEFINED) {
        // Number of bytes shown in the current column
        final int columnBytes = Math.min(m_dataProvider.getDataLength() - i, m_bytesPerColumn);

        final int dataPosition = m_flipBytes ? (i / m_bytesPerColumn) * m_bytesPerColumn
            + (columnBytes - (i % columnBytes) - 1) : i;

        // Print the data
        g.drawString(HEX_BYTES[data[dataPosition] & 0xFF], x, y);
      }
      else {
        g.drawString("??", x, y);
      }

      // Update the position of the x-coordinate
      x += standardSize;
    }
  }

  /**
   * Draws highlighting of bytes when the mouse hovers over them.
   *
   * @param g
   *          The graphics context where the highlighting is drawn.
   */
  private void drawMouseOverHighlighting(final Graphics g)
  {
    if (m_mouseOverHighlighted) {
      g.setColor(m_colorHighlight);

      m_lastHighlightedNibble = getNibbleAtCoordinate(m_lastMouseX, m_lastMouseY);

      if (m_lastHighlightedNibble == -1) {
        return;
      }

      // Find out in which view the mouse currently resides.
      final Views lastHighlightedView = m_lastMouseX >= getAsciiViewLeft() ? Views.ASCII_VIEW
          : Views.HEX_VIEW;

      if (lastHighlightedView == Views.HEX_VIEW) {
        // If the mouse is in the hex view just one nibble must be highlighted.
        final Rectangle r = getNibbleBoundsHex(m_lastHighlightedNibble);
        g.fillRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
      }
      else if (lastHighlightedView == Views.ASCII_VIEW) {
        // If the mouse is in the ASCII view it is necessary
        // to highlight two nibbles.

        final int first = 2 * m_lastHighlightedNibble / 2; // Don't change.

        Rectangle r = getNibbleBoundsHex(first);
        g.fillRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());

        r = getNibbleBoundsHex(first + 1);
        g.fillRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
      }

      // Highlight the byte in the ASCII panel too.
      final Rectangle r = getByteBoundsAscii(m_lastHighlightedNibble);
      g.fillRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
    }
  }

  /**
   * Draws the offsets in the offset view.
   *
   * @param g
   *          The graphics context of the hex panel.
   */
  private void drawOffsets(final Graphics g)
  {
    final int x = -m_firstColumn * m_charWidth + 10;

    // Drawing offset title
    if (m_headerVisible) {
      Font oldFont = getFont();
      g.setFont(oldFont.deriveFont(m_headerFontStyle));
      g.setColor(m_fontColorHeader);
      String title = getHeaderTitleOffset(m_addressMode);
      g.drawString(title, x, m_paddingTop);
      g.setFont(oldFont);
    }

    if (isEnabled()) {
      // Choose the right color for the offset text
      g.setColor(m_fontColorOffsets);
    }
    else {
      g.setColor(m_disabledColor != m_bgColorOffset ? m_disabledColor : Color.WHITE);
    }

    final int bytesToDraw;
    if (m_status == DefinitionStatus.DEFINED && m_dataProvider.getDataLength() > 0) {
      bytesToDraw = getBytesToDraw();
    }
    else {
      bytesToDraw = m_bytesPerRow;
    }

    final String formatString = getAddressModeFormat(m_addressMode);

    // Iterate over the data and print the offsets
    for (int i = 0; i < bytesToDraw; i += m_bytesPerRow) {
      final long address = m_baseAddress + m_firstRow * m_bytesPerRow + i;

      final String offsetString = String.format(formatString, address);
      final int currentRow = i / m_bytesPerRow;

      int y = m_paddingTop + getHeaderHeight() + currentRow * m_rowHeight;
      g.drawString(offsetString, x, y);
    }
  }

  private ColoredRange findColoredRange(final long currentOffset)
  {
    for (final ColoredRangeManager element : m_coloredRanges) {

      final ColoredRange range = element.findRangeWith(currentOffset);

      if (range != null) {
        return range;
      }
    }

    return null;
  }

  /**
   * Based on reference implementation from
   *   https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_string_search_algorithm
   *
   * Returns the start index of the first occurrence of the specified pattern.
   * If the pattern is not found, then -1 is returned.
   * @param startPos The position within the data to start searching.
   * @param length The length of the data section to search.
   * @param pattern The pattern to search.
   * @param caseSensitive Indicates whether to compare case-sensitive or not.
   * @return The start index of the first match, or -1 otherwise.
   */
  private int findIndexOf(int startPos, int length, byte[] pattern, boolean caseSensitive)
  {
    if (pattern.length == 0) {
      return startPos;
    }

    IDataProvider data = getData();

    int dataLength = data.getDataLength();
    if (startPos < 0) startPos = 0;
    if (length < 0) length = 0;
    if (startPos+length > dataLength) length = dataLength - startPos;
    if (length <= 0) {
      return -1;
    }

    // normalizing search string
    for (int i = 0; i < pattern.length; i++) {
      pattern[i] = normalizeByte(pattern[i], caseSensitive);
    }

    int[] byteTable = findMakeByteTable(pattern);
    int[] offsetTable = findMakeOffsetTable(pattern);
    for (int i = startPos + pattern.length - 1, j; i < startPos+length;) {
      byte b;
      for (j = pattern.length - 1;
           pattern[j] == (b = normalizeByte(data.getData(i, 1)[0], caseSensitive));
           i--, j--) {
        if (j == 0) {
          return i;
        }
      }
      i += Math.max(offsetTable[pattern.length - 1 - j], byteTable[b & 255]);
    }
    return -1;
  }

  /**
   * Converts the specified byte value into a lower-cased counterpart if caseSensitive is false.
   */
  private byte normalizeByte(byte value, boolean caseSensitive)
  {
    if (caseSensitive == false) {
      char ch = ConvertHelpers.toChar(value);
      if (ConvertHelpers.isPrintableCharacter(ch)) {
        return ConvertHelpers.toByte(Character.toLowerCase(ch));
      }
    }
    return value;
  }

  /**
   * Based on reference implementation from
   *   https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_string_search_algorithm
   *
   * Makes the jump table based on the mismatched byte information.
   */
  private int[] findMakeByteTable(byte[] pattern)
  {
    int[] table = new int[256];
    for (int i = 0; i < table.length; i++) {
      table[i] = pattern.length;
    }
    for (int i = 0; i < pattern.length - 1; i++) {
      table[pattern[i] & 255] = pattern.length - 1 - i;
    }
    return table;
  }

  /**
   * Based on reference implementation from
   *   https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_string_search_algorithm
   *
   * Makes the jump table based on the scan offset which mismatch occurs.
   */
  private int[] findMakeOffsetTable(byte[] pattern)
  {
    int[] table = new int[pattern.length];
    int lastPrefixPos = pattern.length;
    for (int i = pattern.length - 1; i >= 0; i--) {
      if (findIsPrefix(pattern, i + 1)) {
        lastPrefixPos = i + 1;
      }
      table[pattern.length - 1 - i] = lastPrefixPos - i + pattern.length - 1;
    }
    for (int i = 0; i < pattern.length - 1; i++) {
      int slen = findSuffixLength(pattern, i);
      table[slen] = pattern.length - 1 - i + slen;
    }
    return table;
  }

  /**
   * Based on reference implementation from
   *   https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_string_search_algorithm
   *
   * Is pattern[p:end] a prefix of pattern?
   */
  private boolean findIsPrefix(byte[] pattern, int p)
  {
    for (int i = p, j = 0; i < pattern.length; i++, j++) {
      if (pattern[i] != pattern[j]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Based on reference implementation from
   *   https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_string_search_algorithm
   *
   * Returns the maximum length of the subpattern ends at p and is a suffix.
   */
  private int findSuffixLength(byte[] pattern, int p)
  {
    int len = 0;
    for (int i = p, j = pattern.length - 1; i >= 0 && pattern[i] == pattern[j]; i--, j--) {
      len += 1;
    }
    return len;
  }

  /**
   * Notifies all registered HexListeners that the view has been changed.
   * @param view The new view.
   */
  private void fireHexListener(Views view)
  {
    HexViewEvent event = null;
    Object[] l = m_listeners.getListenerList();
    for (int i = l.length - 2; i >= 0; i -= 2) {
      if (l[i] == IHexViewListener.class) {
        if (event == null) {
          event = new HexViewEvent(this, view);
        }
        ((IHexViewListener)l[i+1]).stateChanged(event);
      }
    }
  }

  /**
   * Notifies all registered HexListeners that the selection or cursor position has changed.
   * @param start Start of the selection or cursor position in nibbles.
   * @param length Selection length in nibbles.
   */
  private void fireHexListener(long start, long length)
  {
    HexViewEvent event = null;
    Object[] l = m_listeners.getListenerList();
    for (int i = l.length - 2; i >= 0; i -= 2) {
      if (l[i] == IHexViewListener.class) {
        if (event == null) {
          event = new HexViewEvent(this, start, length);
        }
        ((IHexViewListener)l[i+1]).stateChanged(event);
      }
    }
  }

  /**
   * Notifies all registered UndoableEditListeners that an undoable event has been triggered.
   */
  private void fireUndoableEditListener(UndoableEdit edit)
  {
    UndoableEditEvent event = null;
    Object[] l = m_listeners.getListenerList();
    for (int i = l.length - 2; i >= 0; i -= 2) {
      if (l[i] == UndoableEditListener.class) {
        if (event == null) {
          event = new UndoableEditEvent(this, edit);
        }
        ((UndoableEditListener)l[i+1]).undoableEditHappened(event);
      }
    }
  }

  /**
   * Returns the number digits required to fully print an offset in the given address mode.
   * @param mode The address mode.
   * @return The number of digits.
   */
  private int getAddressDigits(AddressMode mode)
  {
    switch (mode) {
      case BIT8:  return 2;
      case BIT16: return 4;
      case BIT24: return 6;
      case BIT32: return 8;
      case BIT40: return 10;
      case BIT48: return 12;
      case BIT56: return 14;
      default:    return 16;
    }
  }

  /**
   * Returns a format string for displaying an offset in the given address mode.
   * @param mode The address mode.
   * @return Format string for given address mode.
   */
  private String getAddressModeFormat(AddressMode mode)
  {
    return String.format("%%0%1$dX", getAddressDigits(mode));
  }

  /**
   * Returns the left coordinate of the ASCII view.
   *
   * @return The left coordinate of the ASCII view.
   */
  private int getAsciiViewLeft()
  {
    return getHexViewLeft() + getHexViewWidth();
  }

  /**
   * Returns the bounds of a byte in the ASCII view.
   *
   * @param position
   *          The index of one of the nibbles that belong to the byte.
   *
   * @return The bounds of the byte in the ASCII view.
   */
  private Rectangle getByteBoundsAscii(final int position)
  {
    if (position < 2 * getFirstVisibleByte()) {
      return new Rectangle(-1, -1, -1, -1);
    }

    if (position > 2 * getFirstVisibleByte() + 2 * getMaximumVisibleBytes()) {
      return new Rectangle(-1, -1, -1, -1);
    }

    final int relativePosition = (position - 2 * getFirstVisibleByte()) / 2;

    final int row = relativePosition / m_bytesPerRow;
    final int character = relativePosition % m_bytesPerRow;

    final int x = getAsciiViewLeft() + m_paddingAsciiLeft + character * m_charWidth;
    final int y = m_paddingTop + getHeaderHeight() - m_charHeight + row * m_rowHeight;

    return new Rectangle(x, y, m_charWidth, m_charHeight);
  }

  /**
   * Returns the number of bytes that need to be displayed.
   *
   * @return The number of bytes that need to be displayed.
   */
  private int getBytesToDraw()
  {
    final int firstVisibleByte = getFirstVisibleByte();

    final int maxBytes = getMaximumVisibleBytes() + m_bytesPerRow;

    final int restBytes = m_dataProvider.getDataLength() - firstVisibleByte;

    return Math.min(maxBytes, restBytes);
  }

  /**
   * Returns the character size of a single character on the given graphics
   * context.
   *
   * @param g
   *          The graphics context.
   *
   * @return The size of a single character.
   */
  private int getCharacterWidth(final Graphics g)
  {
    return (int) g.getFontMetrics().getStringBounds("0", g).getWidth();
  }

  /**
   * Determines the positive height (ascent) of a character in a graphical context.
   *
   * @param g
   *          The graphical context.
   *
   * @return The positive height of a character in the graphical context.
   */
  private int getCharAscent(final Graphics g)
  {
    return g.getFontMetrics().getAscent();
  }

  /**
   * Determines the maximum positive height (ascent) of a character in a graphical context.
   *
   * @param g The graphical context.
   * @return The maximum positive height of a character in the graphical context.
   */
  private int getCharMaxAscent(final Graphics g)
  {
    return g.getFontMetrics().getMaxAscent();
  }

  /**
   * Determines the maximum negative height (descent) of a character in a graphical context.
   * @param g The graphical context.
   * @return The maximum negative height in the graphical context.
   */
  private int getCharMaxDescent(final Graphics g)
  {
    return g.getFontMetrics().getMaxDescent();
  }

  /**
   * Returns the size of a hex view column in pixels (includes column spacing).
   *
   * @return The size of a hex view column in pixels.
   */
  private int getColumnSize()
  {
    return NIBBLES_PER_BYTE * m_bytesPerColumn * m_charWidth + m_columnSpacing;
  }

  /**
   * Returns the column of the byte at the current position.
   *
   * @return The column of the byte at the current position.
   */
  private int getCurrentColumn()
  {
    return (int) getCurrentNibble() % (NIBBLES_PER_BYTE * m_bytesPerRow);
  }

  /**
   * Returns the nibble at the caret position.
   *
   * @return The nibble at the care position.
   */
  private long getCurrentNibble()
  {
    return getSelectionStart() + getSelectionLength();
  }

  /**
   * Returns the row of the byte at the current position.
   *
   * @return The row of the byte at the current position.
   */
  private int getCurrentRow()
  {
    return (int) getCurrentNibble() / (NIBBLES_PER_BYTE * m_bytesPerRow);
  }

  /**
   * Returns the number of bytes before the first visible byte.
   *
   * @return The number of bytes before the first visible byte.
   */
  private int getEarlierBytes()
  {
    return m_firstRow * m_bytesPerRow;
  }

  /**
   * Returns the first visible byte.
   *
   * @return The first visible byte.
   */
  private int getFirstVisibleByte()
  {
    return m_firstRow * m_bytesPerRow;
  }

  /**
   * Returns the height of the header panel.
   */
  private int getHeaderHeight()
  {
    int retVal = 0;
    if (m_headerVisible) {
      Graphics g = getGraphics();
      if (g != null) {
        try {
          retVal = getCharMaxAscent(g) + getCharMaxDescent(g);
        } finally {
          g.dispose();
          g = null;
        }
      }
    }
    return retVal;
  }

  /**
   * Returns the title for the offset column. Result depends on the actual width of the offset column.
   * @param mode Current address mode.
   * @return The String "Offset" in different lengths.
   */
  private String getHeaderTitleOffset(AddressMode mode)
  {
    final int length = getAddressDigits(mode);
    String retVal;
    if (length < 2) {
      retVal = "";
    } else if (length < 4) {
      retVal = "Of";
    } else if (length < 6) {
      retVal = "Ofs.";
    } else if (length < 9) {
      retVal = "Offset";
    } else {
      retVal = "Offset(h)";
    }
    return retVal;
  }

  /**
   * Returns the title for the ascii column. Result depends on the actual width of the offset column.
   * @param mode Current address mode.
   * @return The String "Ascii" in different lengths.
   */
  private String getHeaderTitleAscii(AddressMode mode)
  {
    final int length = m_bytesPerRow;
    String retVal;
    if (length < 5) {
      retVal = "";
    } else {
      retVal = "ASCII";
    }
    return retVal;
  }


  /**
   * Returns the left position of the hex view.
   *
   * @return The left position of the hex view.
   */
  private int getHexViewLeft()
  {
    return -m_firstColumn * m_charWidth + m_offsetViewWidth;
  }

  /**
   * Returns the maximum number of visible bytes.
   *
   * @return The maximum number of visible bytes.
   */
  private int getMaximumVisibleBytes()
  {
    return getNumberOfVisibleRows() * m_bytesPerRow;
  }

  /**
   * Returns the index of the nibble below given coordinates.
   *
   * @param x
   *          The x coordinate.
   * @param y
   *          The y coordinate.
   *
   * @return The nibble index at the coordinates or -1 if there is no nibble at
   *         the coordinates.
   */
  private int getNibbleAtCoordinate(final int x, final int y)
  {
    if (m_dataProvider == null) {
      return -1;
    }

    if (x < getHexViewLeft() + m_paddingHexLeft) {
      return -1;
    }

    if (y >= m_paddingTop + getHeaderHeight() - m_font.getSize()) {

      if (x >= getHexViewLeft() && x < getHexViewLeft() + getHexViewWidth()) {
        // Cursor is in hex view
        return getNibbleAtCoordinatesHex(x, y);
      }
      else if (x >= getAsciiViewLeft()) {
        // Cursor is in ASCII view
        return getNibbleAtCoordinatesAscii(x, y);
      }
    }

    return -1;
  }

  /**
   * Returns the index of the nibble below given coordinates in the ASCII view.
   *
   * @param x
   *          The x coordinate.
   * @param y
   *          The y coordinate.
   *
   * @return The nibble index at the coordinates or -1 if there is no nibble at
   *         the coordinates.
   */
  private int getNibbleAtCoordinatesAscii(final int x, final int y)
  {
    // Normalize the x coordinate to inside the ASCII view
    final int normalizedX = x - (getAsciiViewLeft() + m_paddingAsciiLeft);

    if (normalizedX < 0) {
      return -1;
    }

    // Find the row at the coordinate
    final int row = (y - (m_paddingTop + getHeaderHeight() - m_charHeight)) / m_rowHeight;

    final int earlierPositions = 2 * getEarlierBytes();

    if (normalizedX / m_charWidth >= m_bytesPerRow) {
      return -1;
    }

    final int character = 2 * (normalizedX / m_charWidth);

    final int position = earlierPositions + 2 * row * m_bytesPerRow + character;

    if (position >= 2 * m_dataProvider.getDataLength()) {
      return -1;
    }
    else {
      return position;
    }
  }

  /**
   * Returns the index of the nibble below given coordinates in the hex view.
   *
   * @param x
   *          The x coordinate.
   * @param y
   *          The y coordinate.
   *
   * @return The nibble index at the coordinates or -1 if there is no nibble at
   *         the coordinates.
   */
  private int getNibbleAtCoordinatesHex(final int x, final int y)
  {
    // Normalize the x coordinate to inside the hex view
    final int normalizedX = x - (getHexViewLeft() + m_paddingHexLeft);

    final int columnSize = getColumnSize();

    // Find the column at the specified coordinate.
    final int column = normalizedX / columnSize;

    // Return if the cursor is at the spacing at the end of a line.
    if (column >= m_bytesPerRow / m_bytesPerColumn) {
      return -1;
    }

    // Find the coordinate relative to the beginning of the column.
    final int xInColumn = normalizedX % columnSize;

    // Find the nibble inside the column.
    final int nibbleInColumn = xInColumn / m_charWidth;

    // Return if the cursor is in the spacing between columns.
    if (nibbleInColumn >= 2 * m_bytesPerColumn) {
      return -1;
    }

    // Find the row at the coordinate
    final int row = (y - (m_paddingTop + getHeaderHeight() - m_charHeight)) / m_rowHeight;

    final int earlierPositions = 2 * getEarlierBytes();

    final int position = earlierPositions + 2 * (row * m_bytesPerRow + column * m_bytesPerColumn)
        + nibbleInColumn;

    if (position >= 2 * m_dataProvider.getDataLength()) {
      return -1;
    }
    else {
      return position;
    }
  }

  /**
   * Returns the bounds of a nibble in the hex view.
   *
   * @param position
   *          The index of the nibble.
   *
   * @return The bounds of the nibble in the hex view.
   */
  private Rectangle getNibbleBoundsHex(final int position)
  {
    if (position < 2 * getFirstVisibleByte()) {
      return new Rectangle(-1, -1, -1, -1);
    }

    if (position > 2 * getFirstVisibleByte() + 2 * getMaximumVisibleBytes()) {
      return new Rectangle(-1, -1, -1, -1);
    }

    final int relativePosition = position - 2 * getFirstVisibleByte();

    final int columnSize = getColumnSize();

    final int row = relativePosition / (2 * m_bytesPerRow);
    final int column = relativePosition % (2 * m_bytesPerRow) / (2 * m_bytesPerColumn);
    final int nibble = relativePosition % (2 * m_bytesPerRow) % (2 * m_bytesPerColumn);

    final int x = getHexViewLeft() + m_paddingHexLeft + column * columnSize + nibble * m_charWidth;
    final int y = m_paddingTop + getHeaderHeight() - m_charHeight + row * m_rowHeight;

    return new Rectangle(x, y, m_charWidth, m_charHeight);
  }

  /**
   * Returns the number of visible rows.
   *
   * @return The number of visible rows.
   */
  private int getNumberOfVisibleRows()
  {
    final int rawHeight = getHeight() - m_paddingTop - getHeaderHeight() - m_horizontalScrollbar.getHeight();
    return rawHeight / m_rowHeight + (rawHeight % m_rowHeight == 0 ? 0 : 1);
  }

  /**
   * Determines the height of the current font in a graphical context.
   *
   * @param g
   *          The graphical context.
   *
   * @return The height of the current font in the graphical context.
   */
  private int getRowHeight(final Graphics g)
  {
    return g.getFontMetrics().getHeight();
  }

  private long getSelectionStart()
  {
    return m_selectionStart;
  }

  /**
   * Initializes the keys that can be used by the user inside the component.
   */
  private void initHotkeys()
  {
    int none = 0;
    int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    int shift = Event.SHIFT_MASK;

    // Don't change focus on TAB
    setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new HashSet<KeyStroke>());

    final InputMap inputMap = this.getInputMap();
    final ActionMap actionMap = this.getActionMap();

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, none), "LEFT");
    actionMap.put("LEFT", m_leftAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, shift), "shift LEFT");
    actionMap.put("shift LEFT", m_shiftLeftAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, none), "RIGHT");
    actionMap.put("RIGHT", m_rightAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, shift), "shift RIGHT");
    actionMap.put("shift RIGHT", m_shiftRightAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, none), "UP");
    actionMap.put("UP", m_upAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, shift), "shift UP");
    actionMap.put("shift UP", m_upAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, none), "DOWN");
    actionMap.put("DOWN", m_downAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, shift), "shift DOWN");
    actionMap.put("shift DOWN", m_downAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, none), "PAGE_DOWN");
    actionMap.put("PAGE_DOWN", m_pageDownAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, shift), "shift PAGE_DOWN");
    actionMap.put("shift PAGE_DOWN", m_pageDownAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, none), "PAGE_UP");
    actionMap.put("PAGE_UP", m_pageUpAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, shift), "shift PAGE_UP");
    actionMap.put("shift PAGE_UP", m_pageUpAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, none), "HOME");
    actionMap.put("HOME", m_homeLineAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, shift), "shift HOME");
    actionMap.put("shift HOME", m_homeLineAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, ctrl), "ctrl HOME");
    actionMap.put("ctrl HOME", m_homeDocAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, ctrl+shift), "ctrl shift HOME");
    actionMap.put("ctrl shift HOME", m_homeDocAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, none), "END");
    actionMap.put("END", m_endLineAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, shift), "shift END");
    actionMap.put("shift END", m_endLineAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, ctrl), "ctrl END");
    actionMap.put("ctrl END", m_endDocAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, ctrl+shift), "ctrl shift END");
    actionMap.put("ctrl shift END", m_endDocAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, none), "TAB");
    actionMap.put("TAB", m_tabAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, ctrl), "ctrl A");
    actionMap.put("ctrl A", m_SelectAllAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, ctrl), "ctrl V");
    actionMap.put("ctrl V", m_PasteTextAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrl), "ctrl C");
    actionMap.put("ctrl C", m_CopyTextAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrl), "ctrl Z");
    actionMap.put("ctrl Z", m_UndoAction);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ctrl), "ctrl Y");
    actionMap.put("ctrl Y", m_RedoAction);
  }

  /**
   * Initializes all internal listeners.
   */
  private void initListeners()
  {
    // Add the input listeners
    addMouseListener(m_listener);
    addMouseMotionListener(m_listener);
    addMouseWheelListener(m_listener);
    addFocusListener(m_listener);
    addComponentListener(m_listener);
    addKeyListener(m_listener);
    addUndoableEditListener(m_listener);

    m_caret.addCaretListener(m_listener);
  }

  /**
   * Creates and initializes the scroll bar that is used to scroll through the
   * data.
   */
  private void initScrollbar()
  {
    m_scrollbar.addAdjustmentListener(m_listener);

    add(m_scrollbar, BorderLayout.EAST);

    m_horizontalScrollbar.addAdjustmentListener(m_listener);

    add(m_horizontalScrollbar, BorderLayout.SOUTH);
  }

  /**
   * Determines whether data to be displayed is available.
   *
   * @return True, if data is available. False, otherwise.
   */
  private boolean isDataAvailable()
  {
    return m_dataProvider != null;
  }

  private boolean isInsideAsciiView(final int x, final int y)
  {
    return y >= m_paddingTop + getHeaderHeight() - m_font.getSize() && x >= getAsciiViewLeft();
  }

  private boolean isInsideHexView(final int x, final int y)
  {
    return y >= m_paddingTop + getHeaderHeight() - m_font.getSize() && x >= getHexViewLeft()
        && x < getHexViewLeft() + getHexViewWidth();
  }

  /**
   * Determines whether a certain position is visible in the view.
   *
   * @param position
   *          The position in question.
   *
   * @return True, if the position is visible. False, otherwise.
   */
  private boolean isPositionVisible(final long position)
  {
    final int firstVisible = getFirstVisibleByte();
    final int lastVisible = firstVisible + getMaximumVisibleBytes();

    return position >= 2 * firstVisible && position <= 2 * lastVisible;
  }

  private boolean isSelectedOffset(long currentOffset)
  {
    currentOffset = currentOffset - m_baseAddress;

    if (getSelectionLength() == 0) {
      return false;
    }
    else if (getSelectionLength() > 0) {
      return currentOffset >= getSelectionStart() / 2
          && 2 * currentOffset < getSelectionStart() + getSelectionLength();
    }
    else {
      return currentOffset >= (getSelectionStart() + getSelectionLength()) / 2
          && 2 * currentOffset < getSelectionStart();
    }
  }

  /**
   * Resets the current graphic buffer and prepares it for another round of
   * drawing.
   */
  private void resetBufferedGraphic(Graphics g)
  {
    g.setColor(getBackground());
    g.fillRect(0, 0, getWidth(), getHeight());
    g.setFont(m_font);
  }

  /**
   * Scrolls the scroll bar so that it matches the given position.
   *
   * @param position
   *          The position to scroll to.
   */
  private void scrollToPosition(final long position)
  {
    m_scrollbar.setValue((int) position / (2 * m_bytesPerRow));
  }

  /**
   * Moves the current position of the caret and notifies the listeners about
   * the position change.
   *
   * @param newPosition
   *          The new position of the caret.
   */
  private void setCurrentPosition(final long newPosition)
  {
    // setSelectionStart(newPosition);
    m_selectionStart = newPosition; // Avoid notifying twice

    if (!isPositionVisible(getSelectionStart())) {
      scrollToPosition(getSelectionStart());
    }

    fireHexListener(getSelectionStart(), 0);
  }

  /**
   * Adds the specified offset of the modified data to the list or increment its use if already existing.
   * @param offset The position of data that has been modified by the user.
   * @return The number of modifications done to the data at the specified offset before.
   */
  private int setModified(long offset)
  {
    int retVal = 0;
    if (offset >= 0L) {
      Long key = Long.valueOf(offset);
      Integer value = m_modifiedOffsets.get(key);
      if (value != null) {
        retVal = value.intValue();
      }
      value = Integer.valueOf(retVal + 1);
      m_modifiedOffsets.put(key, value);
    }
    return retVal;
  }

  /**
   * Updates the maximum scroll range of the scroll bar depending on the number
   * of bytes in the current data set.
   */
  private void setScrollBarMaximum()
  {
    if (m_dataProvider == null) {
      m_scrollbar.setMaximum(1);
      m_horizontalScrollbar.setMaximum(1);
    }
    else {
      final int visibleRows = getNumberOfVisibleRows();

      final int totalRows = m_dataProvider.getDataLength() / m_bytesPerRow;
      int scrollRange = 2 + totalRows - visibleRows;

      if (scrollRange < 0) {
        scrollRange = 0;
        m_scrollbar.setValue(0);
        m_scrollbar.setEnabled(false);
      }
      else {
        m_scrollbar.setEnabled(true);
      }

      m_scrollbar.setValue(Math.min(m_scrollbar.getValue(), scrollRange));
      m_scrollbar.setMaximum(scrollRange+visibleRows);
      m_scrollbar.setVisibleAmount(visibleRows);
      m_scrollbar.setBlockIncrement(visibleRows);

      final int totalWidth = getAsciiViewLeft() + m_paddingAsciiLeft + m_charWidth * m_bytesPerRow;

      final int realWidth = getWidth() - m_scrollbar.getWidth();

      if (realWidth >= totalWidth) {
        m_horizontalScrollbar.setValue(0);
        m_horizontalScrollbar.setEnabled(false);
      }
      else {
        m_horizontalScrollbar.setMaximum((totalWidth - realWidth) / m_charWidth + 1);
        m_horizontalScrollbar.setEnabled(true);
      }
    }
  }

  private void setSelectionStart(final long selectionStart)
  {
    if (selectionStart != m_selectionStart) {
      m_selectionStart = selectionStart;

      fireHexListener(m_selectionStart, m_selectionLength);
    }
  }

  private void updateHexViewWidth()
  {
    m_hexViewWidth = 15 + getColumnSize() * getBytesPerRow() / getBytesPerColumn();
  }

  /**
   * Calculates and sets the size of the offset view depending on the currently
   * selected address mode.
   */
  private void updateOffsetViewWidth()
  {
    final int addressBytes = getAddressDigits(m_addressMode);
    m_offsetViewWidth = PADDING_OFFSETVIEW + m_charWidth * addressBytes;
  }

  /**
   * Calculates and sets the preferred size of the component.
   */
  private void updatePreferredSize()
  {
    // TODO: Improve this
    final int width = m_offsetViewWidth + m_hexViewWidth + 18 * m_charWidth + m_scrollbar.getWidth();
    setPreferredSize(new Dimension(width, getHeight()));
    revalidate();
  }

  /**
   * Paints the hex window.
   */
  @Override
  protected void paintComponent(final Graphics gx)
  {
    super.paintComponent(gx);

    // Make room for a new graphic
    resetBufferedGraphic(gx);

    // Calculate current sizes of characters and rows
    calculateSizes();

    updateOffsetViewWidth();

    if (m_firstDraw) {
      m_firstDraw = false;

      // The first time the component is drawn, its size must be set.
      updateHexViewWidth();
      updatePreferredSize();
    }

    // Draw the background of the hex panel
    drawBackground(gx);

    // Draw the offsets column
    drawOffsets(gx);

    if (isEnabled()) {
      // Only draw the cursor "shadow" if the component is enabled.
      drawMouseOverHighlighting(gx);
    }

    // If the component has defined data, it can be drawn.
    if (m_status == DefinitionStatus.DEFINED && m_dataProvider != null) {

      final int bytesToDraw = getBytesToDraw();

      if (bytesToDraw != 0 && !m_dataProvider.hasData(getFirstVisibleOffset(), bytesToDraw)) {
        // At this point the component wants to draw data but the data
        // provider does not have the data yet. The hope is that the data
        // provider can reload the data. Until this happens, set the
        // component's status to UNDEFINED and create a timer that
        // periodically rechecks if the missing data is finally available.

        setDefinitionStatus(DefinitionStatus.UNDEFINED);
        setEnabled(false);

        if (m_updateTimer != null) {
          m_updateTimer.setRepeats(false);
          m_updateTimer.stop();
        }

        m_updateTimer = new Timer(1000, new ActionWaitingForData(getFirstVisibleOffset(),
            bytesToDraw));
        m_updateTimer.setRepeats(true);
        m_updateTimer.start();

        return;
      }
    }

    if (isDataAvailable() || m_status == DefinitionStatus.UNDEFINED) {
      // Draw the hex data
      drawHexView(gx);

      // Draw the ASCII data
      drawAsciiPanel(gx);

      // Show the caret if necessary
      if (hasFocus()) {
        drawCaret(gx);
      }
    }
  }

  /**
   * Adds a new event listener to the list of event listeners.
   *
   * @param listener
   *          The new event listener.
   *
   * @throws NullPointerException
   *           Thrown if the listener argument is null.
   */
  public void addHexListener(final IHexViewListener listener)
  {
    if (listener == null) {
      throw new NullPointerException("Error: Listener can't be null");
    }

    m_listeners.add(IHexViewListener.class, listener);
  }

  public void addUndoableEditListener(UndoableEditListener listener)
  {
    if (listener == null) {
      throw new NullPointerException("Error: Listener can't be null");
    }

    m_listeners.add(UndoableEditListener.class, listener);
  }

  /**
   * Clears all offsets that have been marked as modified.
   */
  public void clearModified()
  {
    m_modifiedOffsets.clear();

    repaint();
  }

  /**
   * Colorizes a range of bytes in special colors. To keep the default text or
   * background color, it is possible to pass null as these colors.
   *
   * @param offset
   *          The start offset of the byte range.
   * @param size
   *          The number of bytes in the range.
   * @param color
   *          The text color that is used to color that range.
   * @param bgcolor
   *          The background color that is used to color that range.
   *
   * @throws IllegalArgumentException
   *           Thrown if offset is negative or size is not positive.
   */
  public void colorize(final int level, final long offset, final int size, final Color color,
                       final Color bgcolor)
  {
    if (offset < 0) {
      throw new IllegalArgumentException("Error: Offset can't be negative");
    }

    if (size <= 0) {
      throw new IllegalArgumentException("Error: Size must be positive");
    }

    if (level < 0 || level >= m_coloredRanges.length) {
      throw new IllegalArgumentException("Error: Invalid level");
    }

    m_coloredRanges[level].addRange(new ColoredRange(offset, size, color, bgcolor));

    repaint();
  }

  public void dispose()
  {
    removeMouseListener(m_listener);
    removeMouseMotionListener(m_listener);
    removeMouseWheelListener(m_listener);
    removeFocusListener(m_listener);
    removeComponentListener(m_listener);
    removeKeyListener(m_listener);

    m_caret.removeListener(m_listener);

    m_caret.stop();
  }

  /** Returns whether a redo is possible. */
  public boolean canRedo()
  {
    return m_undo.canRedo();
  }

  /** Returns whether an undo is possible. */
  public boolean canUndo()
  {
    return m_undo.canUndo();
  }

  /**
   * Transfers the currently selected range of data to the system clipboard.
   * Does nothing if no selection is available.
   */
  public void copy()
  {
    if (getSelectionLength() > 0) {
      m_CopyTextAction.actionPerformed(new ActionEvent(this, Event.ACTION_EVENT, ""));
    }
  }

  /**
   * Returns a a flag that indicates whether the bytes inside a column are
   * flipped or not.
   *
   * @return True, if the bytes are flipped. False, otherwise.
   */
  public boolean doFlipBytes()
  {
    return m_flipBytes;
  }

  /**
   * Attempts to find the next occurrence of keyword in the ascii view of the data,
   * starting at the specified offset.
   * @param offset The start offset for the search.
   * @param keyword The keyword to search.
   * @param caseSensitive Indicates whether to search case sensitive.
   * @return The start position of the match, or -1 if no match has been found.
   */
  public int findAscii(int offset, String keyword, boolean caseSensitive)
  {
    if (getDefinitionStatus() == DefinitionStatus.DEFINED) {
      byte[] pattern;
      if (keyword != null) {
        // converting string into byte array
        pattern = new byte[keyword.length()];
        for (int i = 0; i < pattern.length; i++) {
          pattern[i] = (byte)(keyword.charAt(i) & 255);
        }
      } else {
        pattern = new byte[0];
      }

      int len = getData().getDataLength() - offset;
      return findIndexOf(offset, len, pattern, caseSensitive);
    }
    return -1;
  }

  /**
   * Attempts to find the next occurrence of keyword in the hex view of the data,
   * starting at the specified offset.
   * @param offset The start offset for the search.
   * @param keyword The keyword to search.
   * @return The start position of the match, or -1 if no match has been found.
   */
  public int findHex(int offset, byte[] keyword)
  {
    if (getDefinitionStatus() == DefinitionStatus.DEFINED) {
      if (keyword == null) {
        keyword = new byte[0];
      }
      int len = getData().getDataLength() - offset;
      return findIndexOf(offset, len, keyword, false);
    }
    return -1;
  }

  /**
   * Returns the currently selected view.
   * @return The currently selected view.
   */
  public Views getActiveView()
  {
    return m_activeView;
  }

  /**
   * Returns the currently used address mode.
   *
   * @return The currently used address mode.
   */
  public AddressMode getAddressMode()
  {
    return m_addressMode;
  }

  /**
   * Returns the current background color of the ASCII view.
   *
   * @return The current background color of the ASCII view.
   */
  public Color getBackgroundColorAsciiView()
  {
    return m_bgColorAscii;
  }

  /**
   * Returns the current background color of the header view.
   *
   * @return The current background color of the header view.
   */
  public Color getBackgroundColorHeader()
  {
    return m_bgColorHeader;
  }

  /**
   * Returns the current background color of the hex view.
   *
   * @return The current background color of the hex view.
   */
  public Color getBackgroundColorHexView()
  {
    return m_bgColorHex;
  }

  /**
   * Returns the current background color of the offset view.
   *
   * @return The current background color of the offset view.
   */
  public Color getBackgroundColorOffsetView()
  {
    return m_bgColorOffset;
  }

  /**
   * Returns the current base address.
   *
   * @return The current base address.
   */
  public long getBaseAddress()
  {
    return m_baseAddress;
  }

  /**
   * Returns the number of bytes displayed per column.
   *
   * @return The number of bytes displayed per column.
   */
  public int getBytesPerColumn()
  {
    return m_bytesPerColumn;
  }

  /**
   * Returns the current number of bytes displayed per row.
   *
   * @return The current number of bytes displayed per row.
   */
  public int getBytesPerRow()
  {
    return m_bytesPerRow;
  }

  /**
   * Returns the currently assigned caret color.
   * @return
   */
  public Color getCaretColor()
  {
    return m_caret.getColor();
  }

  /**
   * Returns the currently assigned color map, if any.
   * @return The currently assigned color map.
   */
  public IColormap getColorMap()
  {
    return m_colormap;
  }

  /**
   * Returns the spacing between columns in pixels.
   *
   * @return The spacing between columns.
   */
  public int getColumnSpacing()
  {
    return m_columnSpacing;
  }

  /**
   * Returns the offset at the current caret position.
   *
   * @return The offset at the current caret position.
   */
  public long getCurrentOffset()
  {
    final long currentOffset = m_baseAddress + getCurrentNibble() / 2;

    return m_flipBytes ? (currentOffset & -m_bytesPerColumn) + m_bytesPerColumn
        - (currentOffset % m_bytesPerColumn) - 1 : currentOffset;
  }

  /**
   * Returns the currently used data provider.
   *
   * @return The currently used data provider.
   */
  public IDataProvider getData()
  {
    return m_dataProvider;
  }

  /**
   * Returns the current definition status.
   *
   * @return The current definition status.
   */
  public DefinitionStatus getDefinitionStatus()
  {
    return m_status;
  }

  /**
   * Returns the first selected offset.
   *
   * @return The first selected offset.
   */
  public long getFirstSelectedOffset()
  {
    if (m_selectionLength >= 0) {
      return (m_baseAddress + m_selectionStart) / 2;
    }
    else {
      return (m_baseAddress + m_selectionStart + m_selectionLength) / 2;
    }
  }

  /**
   * Returns the first visible offset.
   *
   * @return The first visible offset.
   */
  public long getFirstVisibleOffset()
  {
    return getBaseAddress() + getFirstVisibleByte();
  }

  @Override
  public Font getFont()
  {
    return m_font;
  }

  /**
   * Returns the current font color of the ASCII view.
   *
   * @return The current font color of the ASCII view.
   */
  public Color getFontColorAsciiView()
  {
    return m_fontColorAscii;
  }

  /**
   * Returns the current font color of the header view.
   *
   * @return The current font color of the header view.
   */
  public Color getFontColorHeader()
  {
    return m_fontColorHeader;
  }

  /**
   * Returns the current font color of even columns in the hex view.
   *
   * @return The current font color of even columns in the hex view.
   */
  public Color getFontColorHexView1()
  {
    return m_fontColorHex1;
  }

  /**
   * Returns the current font color of odd columns in the hex view.
   *
   * @return The current font color of odd columns in the hex view.
   */
  public Color getFontColorHexView2()
  {
    return m_fontColorHex2;
  }

  /**
   * Returns the current font color of the offset view.
   *
   * @return The current font color of the offset view.
   */
  public Color getFontColorOffsetView()
  {
    return m_fontColorOffsets;
  }

  /**
   * Returns the size of the font that is used to draw all data.
   *
   * @return The size of the font that is used to draw all data.
   */
  public int getFontSize()
  {
    return m_font.getSize();
  }

  /**
   * Returns the font style used for header text.
   *
   * @return The font style used for header text.
   */
  public int getHeaderFontStyle()
  {
    return m_headerFontStyle;
  }

  /**
   * Returns the current width of the hex view.
   *
   * @return The current width of the hex view.
   */
  public int getHexViewWidth()
  {
    return m_hexViewWidth;
  }

  public long getLastOffset()
  {
    return getBaseAddress() + m_dataProvider.getDataLength();
  }

  /**
   * Returns the last selected offset.
   *
   * @return The last selected offset.
   */
  public long getLastSelectedOffset()
  {
    // In this method it is necessary to round up. This is because
    // half a selected byte counts as a fully selected byte.

    if (m_selectionLength >= 0) {
      return (m_baseAddress + m_selectionStart + m_selectionLength) / 2
          + (m_baseAddress + m_selectionStart + m_selectionLength) % 2;
    }
    else {
      return (m_baseAddress + m_selectionStart) / 2 + (m_baseAddress + m_selectionStart) % 2;
    }
  }

  /**
   * Returns the number of modifications done to the data at the specified offset.
   * @param offset The offset of the modified data.
   * @return Number of modifications done to the data at the specified offset.
   */
  public int getModifiedCount(long offset)
  {
    Integer value = m_modifiedOffsets.get(Long.valueOf(offset));
    if (value != null) {
      return value.intValue();
    } else {
      return 0;
    }
  }

  /**
   * Returns all offsets of data that has been modified by the user.
   * @return An array of offsets of modified data.
   */
  public long[] getModifiedOffsets()
  {
    long[] retVal = new long[m_modifiedOffsets.size()];
    if (!m_modifiedOffsets.isEmpty()) {
      int i = 0;
      Iterator<Long> iter = m_modifiedOffsets.keySet().iterator();
      while (iter.hasNext()) {
        retVal[i] = iter.next().longValue();
        i++;
      }
    }
    return retVal;
  }

  /**
   * Returns whether the byte under the mouse cursor will be highlighted.
   * @return The highlighted state of bytes under the current mouse cursor position
   */
  public boolean getMouseOverHighlighted()
  {
    return m_mouseOverHighlighted;
  }

  /** Returns the name of the last redoable action added to the list. */
  public String getRedoPresentationName()
  {
    if (canRedo()) {
      return m_undo.getRedoPresentationName();
    } else {
      return "";
    }
  }

  /**
   * Returns the current selection background color.
   *
   * @return The current selection background color.
   */
  public Color getSelectionColor()
  {
    return m_selectionColor;
  }

  public long getSelectionLength()
  {
    return m_selectionLength;
  }

  /** Returns the name of the last undoable action added to the list. */
  public String getUndoPresentationName()
  {
    if (canUndo()) {
      return m_undo.getUndoPresentationName();
    } else {
      return "";
    }
  }

  public int getVisibleBytes()
  {
    final int visibleBytes = getMaximumVisibleBytes();

    if (m_dataProvider.getDataLength() - getFirstVisibleByte() >= visibleBytes) {
      return visibleBytes;
    }
    else {
      return m_dataProvider.getDataLength() - getFirstVisibleByte();
    }
  }

  /**
   * Scrolls to a given offset.
   *
   * @param offset
   *          The offset to scroll to.
   *
   * @throws IllegalArgumentException
   *           Thrown if the offset is out of bounds.
   */
  public void gotoOffset(final long offset)
  {
    if (m_dataProvider == null) {
      throw new IllegalStateException("Error: No data provider active");
    }

    if (getCurrentOffset() == offset) {

      if (!isPositionVisible(getSelectionStart())) {
        scrollToPosition(getSelectionStart());
      }

      return;
    }

    final long realOffset = offset - m_baseAddress;

    if (realOffset < 0 || realOffset >= m_dataProvider.getDataLength()) {
      throw new IllegalArgumentException("Error: Invalid offset");
    }

    setCurrentPosition(2 * realOffset);
  }

  /**
   * Returns whether a color map is used to colorize data.
   * @return True, if data will be colorized. False, otherwise.
   */
  public boolean isColorMapEnabled()
  {
    return m_colorMapEnabled;
  }

  /**
   * Returns the status of the component.
   *
   * @return True, if the component is enabled. False, otherwise.
   */
  @Override
  public boolean isEnabled()
  {
    return m_enabled;
  }

  /**
   * Returns whether the title header is visible.
   */
  public boolean isHeaderVisible()
  {
    return m_headerVisible;
  }

  /**
   * Returns whether data has been modified by the user.
   * @return True if data has been modified by the user, false otherwise.
   */
  public boolean isModified()
  {
    return !m_modifiedOffsets.isEmpty();
  }

  /**
   * Returns whether the value at the specified offset has been modified by the user.
   * @param offset The data offset.
   * @return True if the data at the specified offset has been modified by the user, false otherwise.
   */
  public boolean isModified(long offset)
  {
    return m_modifiedOffsets.containsKey(Long.valueOf(offset));
  }

  /** Returns whether vertical lines between the individual views are visible. */
  public boolean isSeparatorsVisible()
  {
    return m_separatorsVisible;
  }

  /**
   * Returns whether to apply a separate color to modified data.
   */
  public boolean isShowModified()
  {
    return m_showModified;
  }

  /**
   * Transfers the contents of the system clipboard into the hex viewer. Data starting at the current
   * cursor position will be overwritten.
   */
  public void paste()
  {
    m_PasteTextAction.actionPerformed(new ActionEvent(this, Event.ACTION_EVENT, ""));
  }

  /** Performs a redo action if available. */
  public void redo()
  {
    if (canRedo()) {
      m_undo.redo();
    }
  }

  /** Removes all undoable edit actions from the list. */
  public void resetUndo()
  {
    m_undo.die();
  }

  /**
   * Registers the specified shortcut for its predefined action.
   * @param shortcut The shortcut to register.
   */
  public void registerShortcut(Shortcut shortcut)
  {
    int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    final InputMap inputMap = this.getInputMap();

    switch (shortcut) {
      case CTRL_A:
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, ctrl), "ctrl A");
        break;
      case CTRL_C:
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrl), "ctrl C");
        break;
      case CTRL_V:
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, ctrl), "ctrl V");
        break;
      case CTRL_Y:
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ctrl), "ctrl Y");
        break;
      case CTRL_Z:
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrl), "ctrl Z");
        break;
    }
  }

  public void removeHexListener(final IHexViewListener listener)
  {
    if (listener == null) {
      throw new NullPointerException("Error: Listener can't be null");
    }

    m_listeners.remove(IHexViewListener.class, listener);
  }

  public void removeUndoableEditListener(UndoableEditListener listener)
  {
    if (listener == null) {
      throw new NullPointerException("Error: Listener can't be null");
    }

    m_listeners.remove(UndoableEditListener.class, listener);
  }

  /**
   * Selects all data in the component.
   */
  public void selectAll()
  {
    m_SelectAllAction.actionPerformed(new ActionEvent(this, Event.ACTION_EVENT, ""));
  }

  /**
   * Selects a new active view.
   * @param view The view to select.
   */
  public void setActiveView(Views view)
  {
    if (view != null && view != m_activeView) {
      m_tabAction.actionPerformed(new ActionEvent(this, Event.ACTION_EVENT, ""));
    }
  }

  /**
   * Sets the currently used address mode.
   *
   * @param mode
   *          The new address mode.
   *
   * @throws NullPointerException
   *           Thrown if the new address mode is null.
   */
  public void setAddressMode(final AddressMode mode)
  {
    if (mode == null) {
      throw new NullPointerException("Error: Address mode can't be null");
    }

    m_addressMode = mode;

    updateOffsetViewWidth();
    updatePreferredSize();
  }

  /**
   * Sets the current background color of the ASCII view.
   *
   * @param color
   *          The new background color of the ASCII view.
   *
   * @throws NullPointerException
   *           Thrown if the new color is null.
   */
  public void setBackgroundColorAsciiView(final Color color)
  {
    if (color == null) {
      throw new NullPointerException("Error: Color can't be null");
    }

    m_bgColorAscii = color;

    repaint();
  }

  /**
   * Sets the current background color of the header view.
   *
   * @param color
   *          The new background color of the header view.
   *
   * @throws NullPointerException
   *           Thrown if the new color is null.
   */
  public void setBackgroundColorHeader(final Color color)
  {
    if (color == null) {
      throw new NullPointerException("Error: Color can't be null");
    }

    m_bgColorHeader = color;

    repaint();
  }

  /**
   * Sets the current background color of the hex view.
   *
   * @param color
   *          The new background color of the hex view.
   *
   * @throws NullPointerException
   *           Thrown if the new color is null.
   */
  public void setBackgroundColorHexView(final Color color)
  {
    if (color == null) {
      throw new NullPointerException("Error: Color can't be null");
    }

    m_bgColorHex = color;

    repaint();
  }

  /**
   * Sets the current background color of the offset view.
   *
   * @param color
   *          The new background color of the offset view.
   *
   * @throws NullPointerException
   *           Thrown if the new color is null.
   */
  public void setBackgroundColorOffsetView(final Color color)
  {
    if (color == null) {
      throw new NullPointerException("Error: Color can't be null");
    }

    m_bgColorOffset = color;

    repaint();
  }

  /**
   * Sets the current base address.
   *
   * @param baseAddress
   *          The current base address.
   *
   * @throws IllegalArgumentException
   *           Thrown if the new base address is negative.
   */
  public void setBaseAddress(final long baseAddress)
  {
    if (baseAddress < 0) {
      throw new IllegalArgumentException("Error: Base address can't be negative");
    }

    this.m_baseAddress = baseAddress;

    repaint();
  }

  /**
   * Sets the number of bytes displayed per column.
   *
   * @param bytes
   *          The new number of bytes per column.
   *
   * @throws IllegalArgumentException
   *           Thrown if the new number of bytes is smaller than 1 or bigger
   *           than the number of bytes per row.
   */
  public void setBytesPerColumn(final int bytes)
  {
    if (bytes <= 0) {
      throw new IllegalArgumentException("Error: Number of bytes must be positive");
    }

    if (bytes > m_bytesPerRow) {
      throw new IllegalArgumentException(
          "Error: Number of bytes can't be more than the number of bytes per row");
    }

    m_bytesPerColumn = bytes;

    updateHexViewWidth();
    updatePreferredSize();

    repaint();
  }

  /**
   * Sets the current number of bytes displayed per row.
   *
   * @param value
   *          The new number of bytes displayed per row.
   *
   * @throws IllegalArgumentException
   *           Thrown if the new number is smaller than 1.
   */
  public void setBytesPerRow(final int value)
  {
    if (value <= 0) {
      throw new IllegalArgumentException("Error: Value must be positive");
    }

    m_bytesPerRow = value;

    repaint();
  }

  /**
   * Assigns a new color to the caret.
   * @param color The new caret color.
   */
  public void setCaretColor(final Color color)
  {
    m_caret.setColor(color);
  }

  /**
   * Assigns a new color map.
   * @param colormap The new color map.
   */
  public void setColormap(final IColormap colormap)
  {
    m_colormap = colormap;

    repaint();
  }

  /**
   * Specify whether to enable the currently assigned color map.
   */
  public void setColorMapEnabled(boolean set)
  {
    if (set != m_colorMapEnabled) {
      m_colorMapEnabled = set;

      repaint();
    }
  }

  /**
   * Sets the spacing between columns.
   *
   * @param spacing
   *          The spacing between columns in pixels.
   *
   * @throws IllegalArgumentException
   *           Thrown if the new spacing is smaller than 1.
   */
  public void setColumnSpacing(final int spacing)
  {
    if (spacing <= 0) {
      throw new IllegalArgumentException("Error: Spacing must be positive");
    }

    m_columnSpacing = spacing;

    repaint();
  }

  /**
   * Sets the caret to a new offset.
   *
   * @param offset
   *          The new offset.
   */
  public void setCurrentOffset(final long offset)
  {
    if (m_dataProvider == null) {
      return;
    }

    if (offset < getBaseAddress() || offset > getBaseAddress() + m_dataProvider.getDataLength()) {
      throw new IllegalArgumentException("Error: Invalid offset");
    }

    setCurrentPosition(CHARACTERS_PER_BYTE * (offset - m_baseAddress));
  }

  /**
   * Sets the current data provider.
   *
   * It is valid to pass null as the new data provider. This clears the display.
   *
   * @param data
   *          The new data provider.
   */
  public void setData(final IDataProvider data)
  {
    /**
     * Remove the data listener from the old data source.
     */
    if (m_dataProvider != null) {
      m_dataProvider.removeListener(m_listener);
    }

    m_dataProvider = data;

    /**
     * Add a data listener to the new data source so that the component can be
     * updated when the data changes.
     */
    if (data != null) {
      m_dataProvider.addListener(m_listener);
    }

    setCurrentPosition(0);

    setScrollBarMaximum();

    repaint();
  }

  /**
   * Changes the definition status of the JHexView component. This flag
   * determines whether real data or ?? are displayed.
   *
   * @param status
   *          The new definition status.
   *
   * @throws NullPointerException
   *           Thrown if the new definition status is null.
   */
  public void setDefinitionStatus(final DefinitionStatus status)
  {
    if (status == null) {
      throw new NullPointerException("Error: Definition status can't be null");
    }

    m_status = status;

    repaint();
  }

  /**
   * Enables or disables the component.
   *
   * @param enabled
   *          True to enable the component, false to disable it.
   */
  @Override
  public void setEnabled(final boolean enabled)
  {
    if (enabled == m_enabled) {
      return;
    }

    this.m_enabled = enabled;

    if (this.m_enabled) {
      setScrollBarMaximum();
    }

    repaint();
  }

  public void setFlipBytes(final boolean flip)
  {
    if (m_flipBytes == flip) {
      return;
    }

    m_flipBytes = flip;

    repaint();
  }

  @Override
  public void setFont(Font font)
  {
    if (font != m_font) {
      if (font == null) {
        font = new Font(GuiHelpers.getMonospaceFont(), Font.PLAIN, getFontSize());
      }
      m_font = font;
      super.setFont(m_font);

      // The proportions of the hex window change significantly.
      // Just start over when the next repaint event comes.
      m_firstDraw = true;

      repaint();
    }
  }

  /**
   * Sets the current font color of the ASCII view.
   *
   * @param color
   *          The new font color of the ASCII view.
   *
   * @throws NullPointerException
   *           Thrown if the new color is null.
   */
  public void setFontColorAsciiView(final Color color)
  {
    if (color == null) {
      throw new NullPointerException("Error: Color can't be null");
    }

    m_fontColorAscii = color;

    repaint();
  }

  /**
   * Sets the current font color of the header view.
   *
   * @param color
   *          The new font color of the header view.
   *
   * @throws NullPointerException
   *           Thrown if the new color is null.
   */
  public void setFontColorHeader(final Color color)
  {
    if (color == null) {
      throw new NullPointerException("Error: Color can't be null");
    }

    m_fontColorHeader = color;

    repaint();
  }

  /**
   * Sets the current font color of even columns in the hex view.
   *
   * @param color
   *          The new font color of even columns in the hex view.
   *
   * @throws NullPointerException
   *           Thrown if the new color is null.
   */
  public void setFontColorHexView1(final Color color)
  {
    if (color == null) {
      throw new NullPointerException("Error: Color can't be null");
    }

    m_fontColorHex1 = color;

    repaint();
  }

  /**
   * Sets the current font color of odd columns in the hex view.
   *
   * @param color
   *          The new font color of odd columns in the hex view.
   *
   * @throws NullPointerException
   *           Thrown if the new color is null.
   */
  public void setFontColorHexView2(final Color color)
  {
    if (color == null) {
      throw new NullPointerException("Error: Color can't be null");
    }

    m_fontColorHex2 = color;

    repaint();
  }

  /**
   * Sets the current font color of the offset view.
   *
   * @param color
   *          The new font color of the offset view.
   *
   * @throws NullPointerException
   *           Thrown if the new color is null.
   */
  public void setFontColorOffsetView(final Color color)
  {
    if (color == null) {
      throw new NullPointerException("Error: Color can't be null");
    }

    m_fontColorOffsets = color;

    repaint();
  }

  /**
   * Sets the font color for data that has been modified by the user.
   *
   * @param color
   *          The font color for data that has been modified by the user.
   *
   * @throws NullPointerException
   *           Thrown if the new color is null.
   */
  public void setFontColorModified(final Color color)
  {
    if (color == null) {
      throw new NullPointerException("Error: Color can't be null");
    }

    m_fontColorModified = color;

    repaint();
  }

  /**
   * Sets the size of the font that is used to draw all data.
   *
   * @param size
   *          The size of the font that is used to draw all data.
   *
   * @throws IllegalArgumentException
   *           Thrown if the new font size is smaller than 1.
   */
  public void setFontSize(final int size)
  {
    if (size <= 0) {
      throw new IllegalArgumentException("Error: Font size must be positive");
    }

    final Font curFont = getFont();
    setFont(curFont.deriveFont((float)size));
  }

  /**
   * Sets the font style that is used to draw all data.
   *
   * @param style
   *          The font style that is used to draw all data.
   */
  public void setFontStyle(final int style)
  {
    final Font curFont = getFont();
    setFont(curFont.deriveFont(style));
  }

  /**
   * Sets the font style used for header text.
   *
   * @param style Font style used for header text.
   */
  public void setHeaderFontStyle(final int style)
  {
    if (style != m_headerFontStyle) {
      m_headerFontStyle = style;

      repaint();
    }
  }

  /**
   * Set whether to draw a title header over the hex data
   * @param set
   */
  public void setHeaderVisible(boolean set)
  {
    if (m_headerVisible != set) {
      m_headerVisible = set;

      // The proportions of the hex window change significantly.
      // Just start over when the next repaint event comes.
      m_firstDraw = true;

      repaint();
    }
  }

  /**
   * Sets the width of the hex view.
   *
   * @param width
   *          The new width of the offset view.
   *
   * @throws IllegalArgumentException
   *           Thrown if the new width is smaller than 1.
   */
  public void setHexViewWidth(final int width)
  {
    if (width <= 0) {
      throw new IllegalArgumentException("Error: Width must be positive");
    }

    m_hexViewWidth = width;

    repaint();
  }

  /**
   * Sets the menu creator of the hex view control.
   *
   * @param creator
   *          The new menu creator. If this parameter is null, no context menu
   *          is shown in the component.
   */
  public void setMenuCreator(final IMenuCreator creator)
  {
    m_menuCreator = creator;
  }

  /**
   * Enables or disables the highlights state of bytes under the mouse cursor.
   * @param highlight The highlighted state of bytes under the mouse cursor.
   */
  public void setMouseOverHighlighted(boolean highlight)
  {
    if (highlight != m_mouseOverHighlighted) {
      m_mouseOverHighlighted = highlight;
      repaint();
    }
  }

  /**
   * Sets the current selection background color.
   *
   * @param color
   *          The new selection background color.
   *
   * @throws NullPointerException
   *           Thrown if the new color is null.
   */
  public void setSelectionColor(final Color color)
  {
    if (color == null) {
      throw new NullPointerException("Error: Color can't be null");
    }

    m_selectionColor = color;

    repaint();
  }

  public void setSelectionLength(final long selectionLength)
  {
    if (selectionLength != m_selectionLength) {
      m_selectionLength = selectionLength;

      fireHexListener(m_selectionStart, m_selectionLength);

      repaint();
    }
  }

  /**
   * Shows or hides vertical lines between the individual views.
   * @param show The visibility state of vertical lines between the individual views.
   */
  public void setSeparatorsVisible(boolean show)
  {
    if (show != m_separatorsVisible) {
      m_separatorsVisible = show;

      repaint();
    }
  }

  /**
   * Enable whether to show modified data in a separate color.
   * @param show
   */
  public void setShowModified(boolean show)
  {
    if (show != m_showModified) {
      m_showModified = show;

      repaint();
    }
  }

//  @Override
//  public void setVisible(boolean aFlag)
//  {
//    if (aFlag != super.isVisible()) {
//      if (aFlag == true) {
//        updateCanvas(false);
//        super.setVisible(aFlag);
//      } else {
//        super.setVisible(aFlag);
//        if (bufferGraphics != null) {
//          bufferGraphics.dispose();
//        }
//        bufferGraphics = null;
//        img.flush();
//        img = null;
//      }
//    }
//  }

  /**
   * Removes special colorization from a range of bytes.
   *
   * @param offset
   *          The start offset of the byte range.
   * @param size
   *          The number of bytes in the byte range.
   *
   * @throws IllegalArgumentException
   *           Thrown if offset is negative or size is not positive.
   */
  public void uncolorize(final int level, final long offset, final int size)
  {
    if (offset < 0) {
      throw new IllegalArgumentException("Error: Offset can't be negative");
    }

    if (size <= 0) {
      throw new IllegalArgumentException("Error: Size must be positive");
    }

    if (level < 0 || level >= m_coloredRanges.length) {
      throw new IllegalArgumentException("Error: Invalid level");
    }

    m_coloredRanges[level].removeRange(offset, size);

    repaint();
  }

  public void uncolorizeAll()
  {
    for (final ColoredRangeManager coloredRange : m_coloredRanges) {
      coloredRange.clear();
    }
  }

  /**
   * Removes all special range colorizations.
   */
  public void uncolorizeAll(final int level)
  {
    m_coloredRanges[level].clear();

    repaint();
  }

  /** Performs an undo action if available. */
  public void undo()
  {
    if (canUndo()) {
      m_undo.undo();
    }
  }

  /**
   * Unregisters the specified shortcut. Associated action will not be executed on shortcut afterwards.
   * @param shortcut The shortcut to unregister.
   */
  public void unregisterShortcut(Shortcut shortcut)
  {
    int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    final InputMap inputMap = this.getInputMap();

    switch (shortcut) {
      case CTRL_A:
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_A, ctrl));
        break;
      case CTRL_C:
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrl));
        break;
      case CTRL_V:
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_V, ctrl));
        break;
      case CTRL_Y:
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ctrl));
        break;
      case CTRL_Z:
        inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrl));
        break;
    }
  }

  // Abstract superclass for undoable edits in the JHexView component.
  public abstract class AbstractEdit extends AbstractUndoableEdit
  {
    private final String name;

    public AbstractEdit(String name)
    {
      this.name = name;
    }

    @Override
    public String getPresentationName()
    {
      return (name != null) ? name : "";
    }

    @Override
    public String getRedoPresentationName()
    {
      return getPresentationName();
    }

    @Override
    public String getUndoPresentationName()
    {
      return getPresentationName();
    }

    @Override
    public String toString()
    {
      return name;
    }
  }

  private class ActionDown extends AbstractAction
  {
    private static final long serialVersionUID = -6501310447863685486L;

    @Override
    public void actionPerformed(final ActionEvent event)
    {
      changeBy(event, 2 * m_bytesPerRow);
    }
  }

  private class ActionEnd extends AbstractAction
  {
    private static final long serialVersionUID = 3857972387525998638L;

    private final boolean isCtrl;

    public ActionEnd(boolean isCtrl)
    {
      this.isCtrl = isCtrl;
    }

    @Override
    public void actionPerformed(final ActionEvent event)
    {
      long change;
      if (isCtrl) {
        change = getData().getDataLength()*2 - getCurrentNibble() - 2;
      } else {
        change = (m_bytesPerRow*2) - (getCurrentNibble() % (m_bytesPerRow*2)) - 2;
      }
      changeBy(event, (int)change);
    }
  }

  private class ActionHome extends AbstractAction
  {
    private static final long serialVersionUID = 3857972387525998637L;

    private final boolean isCtrl;

    public ActionHome(boolean isCtrl)
    {
      this.isCtrl = isCtrl;
    }

    @Override
    public void actionPerformed(final ActionEvent event)
    {
      long change;
      if (isCtrl) {
        change = -getCurrentNibble();
      } else {
        change = -(getCurrentNibble() % (m_bytesPerRow*2));
      }
      changeBy(event, (int)change);
    }
  }

  private class ActionLeft extends AbstractAction
  {
    private static final long serialVersionUID = -9032577023548944503L;

    private final int modifier;

    /** @param modifier Key modifier as defined in InputEvent. */
    public ActionLeft(int modifier)
    {
      this.modifier = modifier;
    }

    @Override
    public void actionPerformed(final ActionEvent event)
    {
      if (modifier == 0 && getSelectionLength() != 0) {
        long cur = getCurrentNibble();
        long start = Math.min(getSelectionStart(), getSelectionStart()+getSelectionLength()) & ~1L;
        int p = (int)(cur - start);
        changeBy(event, -p);
      } else {
        changeBy(event, m_activeView == Views.HEX_VIEW ? -1 : -2);
      }
    }
  }

  private class ActionPageDown extends AbstractAction
  {
    private static final long serialVersionUID = 490837791577654025L;

    @Override
    public void actionPerformed(final ActionEvent event)
    {
      changeBy(event, getNumberOfVisibleRows() * m_bytesPerRow * 2);
    }
  }

  private class ActionPageUp extends AbstractAction
  {
    private static final long serialVersionUID = -7424423002191015929L;

    @Override
    public void actionPerformed(final ActionEvent event)
    {
      changeBy(event, -getNumberOfVisibleRows() * m_bytesPerRow * 2);
    }
  }

  private class ActionRight extends AbstractAction
  {
    private static final long serialVersionUID = 3857972387525998636L;

    private final int modifier;

    /** @param modifier Key modifier as defined in InputEvent. */
    public ActionRight(int modifier)
    {
      this.modifier = modifier;
    }

    @Override
    public void actionPerformed(final ActionEvent event)
    {
      if (modifier == 0 && getSelectionLength() != 0) {
        long cur = getCurrentNibble();
        long start = (Math.max(getSelectionStart(), getSelectionStart()+getSelectionLength())+1) & ~1L;
        int p = (int)(start - cur);
        changeBy(event, p);
      } else {
        changeBy(event, m_activeView == Views.HEX_VIEW ? 1 : 2);
      }
    }
  }

  /** Contains actions for all kinds of specialized shortcuts. */
  private class ActionShortcut extends AbstractAction
  {
    private static final long serialVersionUID = -3513103611571283107L;

    private final KeyStroke keyStroke;

    public ActionShortcut(KeyStroke keyStroke)
    {
      this.keyStroke = keyStroke;
    }

    @Override
    public void actionPerformed(final ActionEvent event)
    {
      int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

      if (isKeyStroke(KeyEvent.VK_A, ctrl)) {
        // "Select all" action
        setSelectionStart(getBaseAddress());
        setSelectionLength(getLastOffset() * 2);
      } else if (isKeyStroke(KeyEvent.VK_V, ctrl)) {
        // "Paste" action
        TransferHandler.getPasteAction().actionPerformed(event);
      } else if (isKeyStroke(KeyEvent.VK_C, ctrl)) {
        // "Copy" action
        TransferHandler.getCopyAction().actionPerformed(event);
      } else if (isKeyStroke(KeyEvent.VK_Z, ctrl)) {
        // "Undo" action
        undo();
      } else if (isKeyStroke(KeyEvent.VK_Y, ctrl)) {
        // "Redo" action
        redo();
      }
    }

    private boolean isKeyStroke(int key, int modifiers)
    {
      if (keyStroke != null) {
        return (keyStroke.getKeyCode() == key) && ((keyStroke.getModifiers() & modifiers) == modifiers);
      } else {
        return false;
      }
    }
  }

  private class ActionTab extends AbstractAction
  {
    private static final long serialVersionUID = -3265020583339369531L;

    @Override
    public void actionPerformed(final ActionEvent event)
    {

      // Switch between hex and ASCII view

      if (m_activeView == Views.HEX_VIEW) {
        m_activeView = Views.ASCII_VIEW;
        setSelectionStart(getSelectionStart() - getSelectionStart() % 2);
      }
      else {
        m_activeView = Views.HEX_VIEW;
      }

      fireHexListener(m_activeView);
      m_caret.setVisible(true);
      repaint();
    }
  }

  private class ActionUp extends AbstractAction
  {
    private static final long serialVersionUID = -3513103611571283106L;

    @Override
    public void actionPerformed(final ActionEvent event)
    {
      changeBy(event, -2 * m_bytesPerRow);
    }
  }

  private class ActionWaitingForData extends AbstractAction
  {
    private static final long serialVersionUID = -610823391617272365L;

    private final long m_offset;

    private final int m_size;

    private ActionWaitingForData(final long offset, final int size)
    {
      m_offset = offset;
      m_size = size;
    }

    @Override
    public void actionPerformed(final ActionEvent event)
    {
      if (m_dataProvider.hasData(m_offset, m_size)) {

        JHexView.this.setEnabled(true);
        setDefinitionStatus(DefinitionStatus.DEFINED);

        ((Timer) event.getSource()).stop();
      }
      else if (!m_dataProvider.keepTrying()) {
        ((Timer) event.getSource()).stop();
      }
    }
  }

  // Represents the undoable edit for a single byte or character.
  public class DataEdit extends AbstractEdit
  {
    private final int offset;
    private final byte oldValue, newValue;
    private final Views view;

    public DataEdit(int offset, byte oldValue, byte newValue, Views view)
    {
      super("Typing");
      this.offset = offset;
      this.oldValue = oldValue;
      this.newValue = newValue;
      this.view = view;
    }

    @Override
    public void undo() throws CannotUndoException
    {
      super.undo();
      if (getDefinitionStatus() == DefinitionStatus.DEFINED) {
        setActiveView(view);
        getData().setData(offset, new byte[]{oldValue});
        clearModified(offset, false);
        setCurrentOffset(offset);
      } else {
        throw new CannotUndoException();
      }
    }

    @Override
    public void redo() throws CannotRedoException
    {
      super.redo();
      if (getDefinitionStatus() == DefinitionStatus.DEFINED) {
        setActiveView(view);
        getData().setData(offset, new byte[]{newValue});
        setModified(offset);
        setCurrentOffset(offset + 1);
      } else {
        throw new CannotRedoException();
      }
    }
  }

  /**
   * Handles copy to and paste from clipboard actions.
   *
   * @author argent77
   *
   */
  private class HexTransferHandler extends TransferHandler
  {
    @Override
    public boolean canImport(TransferSupport support)
    {
      // we only import Strings
      return support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    public int getSourceActions(JComponent c)
    {
      return COPY;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support)
    {
      if (support.getComponent() instanceof JHexView && canImport(support)) {
        JHexView hv = (JHexView)support.getComponent();
        String data = null;
        try {
          data = (String)support.getTransferable().getTransferData(DataFlavor.stringFlavor);
          if (data != null && !support.isDrop()) {
            if (hv.getCurrentOffset() < getData().getDataLength()) {
              if (hv.getActiveView() == Views.HEX_VIEW) {
                // processing hex view
                KeyEvent event = new KeyEvent(hv, 0, 0, 0, 0, '\0');
                for (int i = 0; i < data.length(); i++) {
                  char ch = data.charAt(i);
                  if (!Character.isWhitespace(ch)) {
                    event.setKeyChar(ch);
                    hv.m_listener.keyPressed(event);
                  }
                }
              } else {
                // processing ascii view
                KeyEvent event = new KeyEvent(hv, 0, 0, 0, 0, '\0');
                for (int i = 0; i < data.length(); i++) {
                  char ch = data.charAt(i);
                  event.setKeyChar(ch);
                  hv.m_listener.keyPressed(event);
                }
              }
              return true;
            }
          }
        } catch (UnsupportedFlavorException ufe) {
          ufe.printStackTrace();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
      return false;
    }

    @Override
    protected Transferable createTransferable(JComponent c)
    {
      if (c instanceof JHexView) {
        JHexView hv = (JHexView)c;
        StringBuilder sb = new StringBuilder();

        // preparing data
        long ofs = hv.getSelectionStart() / 2L;
        int len = (int)hv.getSelectionLength() / 2;
        if (ofs+len > getData().getDataLength()) {
          len = (int)(getData().getDataLength() - ofs);
        }

        // creating data
        if (len > 0) {
          byte[] buffer = getData().getData(ofs, len);
          if (buffer != null && buffer.length > 0) {
            if (hv.getActiveView() == Views.HEX_VIEW) {
              // processing hex view
              for (int i = 0; i < buffer.length; i++) {
                sb.append(String.format("%1$02X", buffer[i] & 0xff));
                if (i+1 < buffer.length) {
                  sb.append(' ');
                }
              }
            } else {
              // processing ascii view
              char[] chars = ConvertHelpers.toChar(buffer);
              for (int i = 0; i < chars.length; i++) {
                if (!ConvertHelpers.isPrintableCharacter(chars[i])) {
                  chars[i] = '.';
                }
              }
              sb.append(chars);
            }
          }
        }

        return new StringSelection(sb.toString());
      }
      return null;
    }
  }

  /**
   * Event listeners are moved into an internal class to avoid publishing the
   * listener methods in the public interface of the JHexView.
   *
   * @author sp
   *
   */
  private class InternalListener implements AdjustmentListener, MouseListener, MouseMotionListener,
      FocusListener, ICaretListener, IDataChangedListener, ComponentListener, KeyListener,
      MouseWheelListener, UndoableEditListener
  {
    private boolean mouseButtonPressed = false;

    private void keyPressedInAsciiView(final KeyEvent event)
    {
      int offset = (int)getCurrentOffset();
      byte oldValue, newValue;

      final byte[] data = m_dataProvider.getData(getCurrentOffset(), 1);
      if (data == null || data.length == 0) {
        return;
      }
      oldValue = data[0];

      if (getSelectionStart() >= m_dataProvider.getDataLength() * 2) {
        return;
      }

      data[0] = (byte) event.getKeyChar();
      newValue = data[0];

      m_dataProvider.setData(getCurrentOffset(), data);

      // mark offset as modified
      setModified(getCurrentOffset());

      // register as undoable action
      fireUndoableEditListener(new DataEdit(offset, oldValue, newValue, getActiveView()));

//      setSelectionStart(getSelectionStart() + 2);
      changeBy(new ActionEvent(this, 0, "", 0), 2);
    }

    private void keyPressedInHexView(final KeyEvent event)
    {
      int offset = (int)getCurrentOffset();
      byte oldValue, newValue;

      final byte[] data = m_dataProvider.getData(getCurrentOffset(), 1);
      if (data == null || data.length == 0) {
        return;
      }
      oldValue = data[0];

      final long pos = m_baseAddress + getSelectionStart();

      if (getSelectionStart() >= m_dataProvider.getDataLength() * 2) {
        return;
      }

      final int value = Character.digit(event.getKeyChar(), 16);

      if (value == -1) {
        return;
      }

      if (pos % 2 == 0) {
        data[0] = (byte) (data[0] & 0x0F | value << 4);
      }
      else {
        data[0] = (byte) (data[0] & 0xF0 | value);
      }

      m_dataProvider.setData(getCurrentOffset(), data);
      newValue = data[0];

      // mark offset as modified
      setModified(getCurrentOffset());

      // register as undoable action
      fireUndoableEditListener(new DataEdit(offset, oldValue, newValue, getActiveView()));

//      setSelectionStart(getSelectionStart() + 1);
      changeBy(new ActionEvent(this, 0, "", 0), 1);
    }

    private void showPopupMenu(final MouseEvent event)
    {
      if (m_menuCreator != null) {
        final JPopupMenu menu = m_menuCreator.createMenu(getCurrentOffset());

        if (menu != null) {
          menu.show(JHexView.this, event.getX(), event.getY());
        }
      }
    }

    @Override
    public void adjustmentValueChanged(final AdjustmentEvent event)
    {
      if (event.getSource() == m_scrollbar) {
        m_firstRow = event.getValue();
      }
      else {
        m_firstColumn = event.getValue();
      }

      repaint();
    }

    @Override
    public void caretStatusChanged(final JCaret source)
    {
      repaint();
    }

    @Override
    public void componentHidden(final ComponentEvent event)
    {
    }

    @Override
    public void componentMoved(final ComponentEvent event)
    {
    }

    @Override
    public void componentResized(final ComponentEvent event)
    {
      setScrollBarMaximum();
    }

    @Override
    public void componentShown(final ComponentEvent event)
    {
    }

    @Override
    public void dataChanged(DataChangedEvent event)
    {
      setScrollBarMaximum();

      repaint();
    }

    @Override
    public void focusGained(final FocusEvent event)
    {
      m_caret.setVisible(true);
      repaint();
    }

    @Override
    public void focusLost(final FocusEvent event)
    {
      repaint();
    }

    @Override
    public void keyPressed(final KeyEvent event)
    {
      if (!isEnabled()) {
        return;
      }

      if (m_activeView == Views.HEX_VIEW) {

        if (m_dataProvider.isEditable() && ConvertHelpers.isHexCharacter(event.getKeyChar())) {
          keyPressedInHexView(event);
        }
      }
      else {

        if (m_dataProvider.isEditable() && ConvertHelpers.isPrintableCharacter(event.getKeyChar())) {
          keyPressedInAsciiView(event);
        }
      }

      repaint();
    }

    @Override
    public void keyReleased(final KeyEvent event)
    {
    }

    @Override
    public void keyTyped(final KeyEvent event)
    {
    }

    @Override
    public void mouseClicked(final MouseEvent event)
    {
    }

    @Override
    public void mouseDragged(final MouseEvent event)
    {
      if (!isEnabled()) {
        return;
      }

      if (mouseButtonPressed) {
        final int x = event.getX();
        final int y = event.getY();

        if (y < m_paddingTop - (m_rowHeight - m_charHeight)) {
          scrollToPosition(2 * getFirstVisibleByte() - 2 * m_bytesPerRow);

          if (getSelectionLength() - 2 * m_bytesPerRow < 0) {
            return;
          }

          setSelectionLength(getSelectionLength() - 2 * m_bytesPerRow);
        }
        else if (y >= m_rowHeight * getNumberOfVisibleRows()) {
          scrollToPosition(2 * getFirstVisibleByte() + 2 * m_bytesPerRow);

          if (getSelectionLength() + 2 * m_bytesPerRow > 2 * (m_dataProvider.getDataLength() - getSelectionStart())) {
            return;
          }

          setSelectionLength(getSelectionLength() + 2 * m_bytesPerRow);
        }
        else {
          final int position = getNibbleAtCoordinate(x, y);

          if (position != -1) {
            setSelectionLength(position - getSelectionStart());
            repaint();
          }
        }
      }
    }

    @Override
    public void mouseEntered(final MouseEvent event)
    {
    }

    @Override
    public void mouseExited(final MouseEvent event)
    {
    }

    @Override
    public void mouseMoved(final MouseEvent event)
    {
      m_lastMouseX = event.getX();
      m_lastMouseY = event.getY();

      repaint();
    }

    @Override
    public void mousePressed(final MouseEvent event)
    {
      if (!isEnabled()) {
        return;
      }

      if (event.getButton() == MouseEvent.BUTTON1/* || event.getButton() == MouseEvent.BUTTON3*/) {
        mouseButtonPressed = true;

        m_selectionLength = 0; // We don't want the notifiers to kick in here.
        // setSelectionLength(0);

        requestFocusInWindow();

        final int x = event.getX();
        final int y = event.getY();

        int position = getNibbleAtCoordinate(x, y);

        Views oldView = m_activeView;
        if (isInsideHexView(x, y)) {
          m_activeView = Views.HEX_VIEW;
        }
        else if (isInsideAsciiView(x, y)) {
          m_activeView = Views.ASCII_VIEW;
        }

        if (oldView != m_activeView) {
          fireHexListener(m_activeView);
        }

        m_caret.setVisible(true);

        if (position != -1) {
          // double click selects a whole word
          if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2 &&
              m_activeView == Views.ASCII_VIEW) {
            position /= 2;  // get byte position

            // starting from click-position, find word delimiter characters in both directions
            final String delimiter = ".,:;()?!-'/\"";
            char ch = ConvertHelpers.toChar(getData().getData(position, 1)[0]);
            int posStart = position, posEnd = position;
            if (!Character.isWhitespace(ch) && delimiter.indexOf(ch) < 0 &&
                getFont().canDisplay(ch)) {
              // find starting delimiter
              for (int i = 1; i < position; i++) {
                ch = ConvertHelpers.toChar(getData().getData(position-i, 1)[0]);
                if (Character.isWhitespace(ch) || delimiter.indexOf(ch) >= 0 ||
                    !getFont().canDisplay(ch)) {
                  break;
                }
                posStart--;
              }

              // find ending delimiter
              final int maxLength = getData().getDataLength() - position;
              for (int i = 1; i < maxLength; i++) {
                ch = ConvertHelpers.toChar(getData().getData(position+i, 1)[0]);
                if (Character.isWhitespace(ch) || delimiter.indexOf(ch) >= 0 ||
                    !getFont().canDisplay(ch)) {
                  break;
                }
                posEnd++;
              }
            }

            m_selectionStart = posStart * 2;
            m_selectionLength = 2 * (posEnd - posStart + 1);
            fireHexListener(m_selectionStart, m_selectionLength);
          } else {
            setCurrentPosition(position);
          }
          repaint();
        }
        else {
          // m_selectionLength = 0 must be notified in case the click position
          // is invalid.

          fireHexListener(m_selectionStart, m_selectionLength);

          repaint();
        }
      }

      if (event.isPopupTrigger()) {
        showPopupMenu(event);
      }
    }

    @Override
    public void mouseReleased(final MouseEvent event)
    {
      if (event.isPopupTrigger()) {
        showPopupMenu(event);
      }
      if (event.getButton() == MouseEvent.BUTTON1) {
        mouseButtonPressed = false;
      }
    }

    @Override
    public void mouseWheelMoved(final MouseWheelEvent e)
    {
      // Mouse wheel support for scrolling

      if (!isEnabled()) {
        return;
      }

      final int notches = e.getWheelRotation();
      m_scrollbar.setValue(m_scrollbar.getValue() + 3*notches); // scrolling 3 lines per notch
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e)
    {
      m_undo.addEdit(e.getEdit());
    }
  }

  /**
   * Enumeration that is used to switch the output format of the offsets from
   * 8 bit mode up to 64 bit mode.
   *
   */
  public enum AddressMode {
    BIT8, BIT16, BIT24, BIT32, BIT40, BIT48, BIT56, BIT64
  }

  /**
   * Enumeration that is used to decided whether real data or ??? is shown.
   *
   */
  public enum DefinitionStatus {
    DEFINED, UNDEFINED
  }

  /**
   * Enumeration that is used to decide which view of the component has the
   * focus.
   *
   */
  public enum Views {
    HEX_VIEW, ASCII_VIEW
  }

  /**
   * Shortcuts registered for this component by default.
   *
   */
  public enum Shortcut {
    /** Shortcut associated with the "Select all" action. */
    CTRL_A,
    /** Shortcut associated with the "Copy selected data" action. */
    CTRL_C,
    /** Shortcut associated with the "Paste data" action. */
    CTRL_V,
    /** Shortcut associated with the "Redo" action. */
    CTRL_Y,
    /** Shortcut associated with the "Undo" action. */
    CTRL_Z
  }
}
