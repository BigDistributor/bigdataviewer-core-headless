package bdv.img.openconnectome;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.util.Fraction;
import bdv.AbstractViewerImgLoader;
import bdv.img.cache.Cache;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class OpenConnectomeImageLoader extends AbstractViewerImgLoader< UnsignedByteType, VolatileUnsignedByteType >
{
	private final int numScales;

	private final double[][] mipmapResolutions;

	private final long[][] imageDimensions;

	private final int[][] blockDimensions;

	private final AffineTransform3D[] mipmapTransforms;

	protected VolatileGlobalCellCache< VolatileByteArray > cache;

	public OpenConnectomeImageLoader( final String baseUrl, final String token, final String mode )
	{
		super( new UnsignedByteType(), new VolatileUnsignedByteType() );

		final OpenConnectomeTokenInfo info = tryFetchTokenInfo( baseUrl, token, 20 );

		numScales = info.dataset.cube_dimension.size();

		mipmapResolutions = info.getLevelScales( mode );
		imageDimensions = info.getLevelDimensions( mode );
		blockDimensions = info.getLevelCellDimensions();
		mipmapTransforms = info.getLevelTransforms( mode );

		cache = new VolatileGlobalCellCache< VolatileByteArray >(
				new OpenConnectomeVolatileArrayLoader( baseUrl, token, mode, info.getMinZ() ), 1, 1, numScales, 10 );
	}

	/**
	 * Fetch the list of public tokens from an OpenConnectome volume cutout
	 * service, e.g. {@linkplain http://openconnecto.me/ocp/ca/public_tokens/}.
	 *
	 * @param baseUrl
	 *            e.g. "http://openconnecto.me/ocp/ca"
	 * @return a list of {@link String Strings}
	 * @throws JsonSyntaxException
	 * @throws JsonIOException
	 * @throws IOException
	 */
	final static public String[] fetchTokenList( final String baseUrl ) throws JsonSyntaxException, JsonIOException, IOException
	{
		final Gson gson = new Gson();
		final URL url = new URL( baseUrl + "/public_tokens/" );
		final String[] tokens = gson.fromJson( new InputStreamReader( url.openStream() ), String[].class );
		return tokens;
	}

	/**
	 * Fetch information for a token from an OpenConnectome volume cutout
	 * service, e.g. {@linkplain http://openconnecto.me/ocp/ca/<token>/info/}.
	 *
	 * @param baseUrl
	 *            e.g. "http://openconnecto.me/ocp/ca"
	 * @param token
	 * @return an {@link OpenConnectomeTokenInfo} instance that carries the
	 *         token information
	 * @throws JsonSyntaxException
	 * @throws JsonIOException
	 * @throws IOException
	 */
	final static public OpenConnectomeTokenInfo fetchTokenInfo( final String baseUrl, final String token )
			throws JsonSyntaxException, JsonIOException, IOException
	{
		final Gson gson = new Gson();
		final URL url = new URL( baseUrl + "/" + token + "/info/" );
		return gson.fromJson( new InputStreamReader( url.openStream() ), OpenConnectomeTokenInfo.class );
	}

	/**
	 * Try to fetch the list of public tokens from an OpenConnectome volume
	 * cutout service, e.g.
	 * {@linkplain http://openconnecto.me/ocp/ca/public_tokens/}.
	 *
	 * @param baseUrl
	 *            e.g. "http://openconnecto.me/ocp/ca"
	 * @param maxNumTrials
	 *            the maximum number of trials
	 *
	 * @return a list of {@link String Strings} or <code>null</code> if
	 *         <code>maxNumTrials</code> were executed without success
	 */
	final static public String[] tryFetchTokenList( final String baseUrl, final int maxNumTrials )
	{
		String[] tokens = null;
		for ( int i = 0; i < maxNumTrials && tokens == null; ++i )
		{
			try
			{
				tokens = fetchTokenList( baseUrl );
				break;
			}
			catch ( final Exception e )
			{}
			try
			{
				Thread.sleep( 100 );
			}
			catch ( final InterruptedException e )
			{}
		}
		return tokens;
	}

	/**
	 * Try to fetch information for a token from an OpenConnectome volume cutout
	 * service, e.g. {@linkplain http://openconnecto.me/ocp/ca/<token>/info/}.
	 *
	 * @param baseUrl
	 *            e.g. "http://openconnecto.me/ocp/ca"
	 * @param token
	 * @param maxNumTrials
	 * @return an {@link OpenConnectomeTokenInfo} instance that carries the
	 *         token information or <code>null</code> if
	 *         <code>maxNumTrials</code> were executed without success
	 */
	final static public OpenConnectomeTokenInfo tryFetchTokenInfo( final String baseUrl, final String token, final int maxNumTrials )
	{
		OpenConnectomeTokenInfo info = null;
		for ( int i = 0; i < maxNumTrials && info == null; ++i )
		{
			try
			{
				info = fetchTokenInfo( baseUrl, token );
				break;
			}
			catch ( final Exception e )
			{}
			try
			{
				Thread.sleep( 100 );
			}
			catch ( final InterruptedException e )
			{}
		}
		return info;
	}

	@Override
	public RandomAccessibleInterval< UnsignedByteType > getImage( final ViewId view, final int level )
	{
		final CachedCellImg< UnsignedByteType, VolatileByteArray > img = prepareCachedImage( view, level, LoadingStrategy.BLOCKING );
		final UnsignedByteType linkedType = new UnsignedByteType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileUnsignedByteType > getVolatileImage( final ViewId view, final int level )
	{
		final CachedCellImg< VolatileUnsignedByteType, VolatileByteArray > img = prepareCachedImage( view, level, LoadingStrategy.VOLATILE );
		final VolatileUnsignedByteType linkedType = new VolatileUnsignedByteType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public double[][] getMipmapResolutions( final int setup )
	{
		return mipmapResolutions;
	}

	@Override
	public int numMipmapLevels( final int setup )
	{
		return numScales;
	}

	/**
	 * (Almost) create a {@link CachedCellImg} backed by the cache. The created image
	 * needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked
	 * type} before it can be used. The type should be either {@link ARGBType}
	 * and {@link VolatileARGBType}.
	 */
	protected < T extends NativeType< T > > CachedCellImg< T, VolatileByteArray > prepareCachedImage( final ViewId view, final int level, final LoadingStrategy loadingStrategy )
	{
		final long[] dimensions = imageDimensions[ level ];
		final int[] cellDimensions = blockDimensions[ level ];

		final int priority = numScales - 1 - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellCache< VolatileByteArray > c = cache.new VolatileCellCache( view.getTimePointId(), view.getViewSetupId(), level, cacheHints );
		final VolatileImgCells< VolatileByteArray > cells = new VolatileImgCells< VolatileByteArray >( c, new Fraction(), dimensions, cellDimensions );
		final CachedCellImg< T, VolatileByteArray > img = new CachedCellImg< T, VolatileByteArray >( cells );
		return img;
	}

	@Override
	public Cache getCache()
	{
		return cache;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms( final int setup )
	{
		return mipmapTransforms;
	}
}
