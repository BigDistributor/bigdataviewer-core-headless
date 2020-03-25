package bdv.tools.brightness;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * Adapted from http://stackoverflow.com/a/3072979/230513
 */
public class ColorIcon implements Icon
{
	private final int width;

	private final int height;

	private final boolean drawAsCircle;

	private final int arcWidth;

	private final int arcHeight;

	private final boolean drawOutline;

	private final Color color;

	private final int size; // == min(width, height)

	private final int ox;

	private final int oy;

	public ColorIcon( final Color color )
	{
		this( color, 16, 16, true );
	}

	public ColorIcon( final Color color, final int width, final int height, final boolean drawAsCircle )
	{
		this( color, width, height, drawAsCircle, 3, 3, false );
	}

	public ColorIcon( final Color color, final int width, final int height, final int arcWidth, final int arcHeight )
	{
		this( color, width, height, false, arcWidth, arcHeight, false );
	}

	public ColorIcon( final Color color, final int width, final int height, final int arcWidth, final int arcHeight, final boolean drawOutline )
	{
		this( color, width, height, false, arcWidth, arcHeight, drawOutline );
	}

	private ColorIcon( final Color color, final int width, final int height, final boolean drawAsCircle, final int arcWidth, final int arcHeight, final boolean drawOutline )
	{
		this.color = color;
		this.width = width;
		this.height = height;
		this.drawAsCircle = drawAsCircle;
		this.arcWidth = arcWidth;
		this.arcHeight = arcHeight;
		this.drawOutline = drawOutline;

		size = Math.min( width, height );
		ox = ( width - size ) / 2;
		oy = ( height - size ) / 2;
	}

	@Override
	public void paintIcon( final Component c, final Graphics g, final int x, final int y )
	{
		final Graphics2D g2d = ( Graphics2D ) g;
		g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		final int x0 = x + ox;
		final int y0 = y + oy;
		if ( color == null )
		{
			g2d.setColor( new Color( 0xffbcbc ) );
			g2d.fillArc( x0, y0, size, size, 0, 120 );
			g2d.setColor( new Color( 0xbcffbc ) );
			g2d.fillArc( x0, y0, size, size, 120, 120 );
			g2d.setColor( new Color( 0xbcbcff ) );
			g2d.fillArc( x0, y0, size, size, 240, 120 );
		}
		else
		{
			g2d.setColor( color );
			if ( drawAsCircle )
				g2d.fillOval( x0, y0, size, size );
			else
				g2d.fillRoundRect( x, y, width, height, arcWidth, arcHeight );

			if ( drawOutline )
			{
				g2d.setColor( c.isFocusOwner() ? new Color( 0x8FC4F9 ) : Color.gray );
				if ( drawAsCircle )
					g2d.drawOval( x0, y0, size, size );
				else
					g2d.drawRoundRect( x, y, width, height, arcWidth, arcHeight );
			}
		}
	}

	@Override
	public int getIconWidth()
	{
		return width;
	}

	@Override
	public int getIconHeight()
	{
		return height;
	}
}
