package bdv;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.viewer.render.DefaultMipmapOrdering;
import bdv.viewer.render.MipmapOrdering;
import bdv.viewer.render.SetCacheHints;

public class VolatileSpimSource< T extends NumericType< T >, V extends Volatile< T > & NumericType< V > >
		extends AbstractSpimSource< V >
		implements MipmapOrdering, SetCacheHints
{
	protected final SpimSource< T > nonVolatileSource;

	protected final ViewerSetupImgLoader< ?, V > imgLoader;

	protected final MipmapOrdering mipmapOrdering;

	@SuppressWarnings( "unchecked" )
	public VolatileSpimSource( final AbstractSpimData< ? > spimData, final int setup, final String name )
	{
		super( spimData, setup, name );
		nonVolatileSource = new SpimSource< T >( spimData, setup, name );
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		imgLoader = ( ViewerSetupImgLoader< ?, V > ) ( ( ViewerImgLoader ) seq.getImgLoader() ).getSetupImgLoader( setup );
		if ( MipmapOrdering.class.isInstance( imgLoader ) )
			mipmapOrdering = ( ( MipmapOrdering ) imgLoader );
		else
			mipmapOrdering = new DefaultMipmapOrdering( this );
		loadTimepoint( 0 );
	}

	@Override
	public V getType()
	{
		return imgLoader.getVolatileImageType();
	}

	public SpimSource< T > nonVolatile()
	{
		return nonVolatileSource;
	}

	@Override
	protected RandomAccessibleInterval< V > getImage( final int timepointId, final int level )
	{
		return imgLoader.getVolatileImage( timepointId, level );
	}

	@Override
	protected AffineTransform3D[] getMipmapTransforms()
	{
		return imgLoader.getMipmapTransforms();
	}

	@Override
	public MipmapHints getMipmapHints( final AffineTransform3D screenTransform, final int timepoint, final int previousTimepoint )
	{
		return mipmapOrdering.getMipmapHints( screenTransform, timepoint, previousTimepoint );
	}

	@Override
	public void setCacheHints( final int level, final CacheHints cacheHints )
	{
		if ( cacheHints != null )
		{
			final RandomAccessibleInterval< V > source = currentSources[ level ];
			// The type check is currently necessary because it might be a
			// constant RandomAccessibleInterval (for missing images, see
			// Hdf5ImageLoader#getMissingDataImage)
			if ( CachedCellImg.class.isInstance( source ) )
				( ( CachedCellImg< ?, ? > ) source ).setCacheHints( cacheHints );
		}
	}
}
