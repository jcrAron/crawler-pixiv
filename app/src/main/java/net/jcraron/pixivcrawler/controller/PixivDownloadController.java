package net.jcraron.pixivcrawler.controller;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.http.client.CookieStore;

import net.jcraron.pixivcrawler.ImageInfo;
import net.jcraron.pixivcrawler.database.FlagTable;
import net.jcraron.pixivcrawler.database.ImaeInfoTable;
import net.jcraron.pixivcrawler.pixiv.HPixiv;
import net.jcraron.pixivcrawler.pixiv.HPixiv.ImageInfoImp;
import net.jcraron.pixivcrawler.pixiv.PixivHttpHandle;
import net.jcraron.pixivcrawler.util.CookieLoader;
import net.jcraron.pixivcrawler.util.FileTypeUtil;
import net.jcraron.pixivcrawler.util.IOUtil;
import net.jcraron.pixivcrawler.util.exception.DecryptException;

public class PixivDownloadController implements Closeable {
	private PixivHttpHandle httpHandle;
	private HPixiv pixiv;
	private ScheduledExecutorService schedule;

	private File saveImageDir;
	private File httpCookies;

	private PriorityBlockingQueue<WithPriority<WaittingResult<Boolean>>> downloadQueue;
	/** synchronized set */
	private Set<String> waitingId;

	private Connection databaseConnection;
	private ImaeInfoTable imageInfoTable;
	private FlagTable flagTable;

	public PixivDownloadController(File saveImageDir, File database, File httpCookies) throws SQLException, IOException, DecryptException {
		this.saveImageDir = saveImageDir;
		this.httpCookies = httpCookies;
		initSQL(database);
		initPixiv(httpCookies);
		initSchedule();
	}

	protected final record WithPriority<T> (int priority, T attached) {
	}

	protected boolean isExistOnSqlAndAlarm(ImageInfo image) {
		try {
			if (imageInfoTable.isExist(image)) {
				System.out.println("Existing " + image.getImageId());
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected void downloadFollowingArtistWorks() {
		System.out.println("Getting following artist...");
		try {
			int pageSize = pixiv.getFollowingTotalAmount() / 24 + 1;
			for (int page = 1; page <= pageSize; page++) {
				String[] artistIds = pixiv.getFollowing(page);
				for (String artistId : artistIds) {
					ImageInfo[] imageinfos = pixiv.getImageInfo_artist(artistId);
					for (ImageInfo image : imageinfos) {
						if (waitingId.contains(image.getImageId()) && isExistOnSqlAndAlarm(image)) {
							continue;
						}
						try {
							waitingId.add(image.getImageId());
							scheduleDownload(Integer.valueOf(image.getImageId()), () -> {
								boolean ret = this.downloadAndRecord(image);
								waitingId.remove(image.getImageId());
								return ret;
							});
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		finally {
			System.out.println("Finish search of following artist!! ");
		}
	}

	protected void downloadNewestWorks() {
		System.out.println("Getting the newest...");
		try {
			for (int page = 1; page <= 35; page++) {
				ImageInfoImp[] imageinfos = pixiv.getNewestImageFromFollowing(page);
				for (ImageInfoImp imageinfo : imageinfos) {
					if (waitingId.add(imageinfo.getImageId()) && isExistOnSqlAndAlarm(imageinfo)) {
						continue;
					}
					try {
						waitingId.add(imageinfo.getImageId());
						scheduleDownload(Integer.valueOf(imageinfo.getImageId()), () -> {
							boolean ret = this.downloadAndRecord(imageinfo);
							waitingId.remove(imageinfo.getImageId());
							return ret;
						});
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} finally {
			System.out.println("Finish search of the newest!! ");
		}
	}

	public WaittingResult<Boolean> scheduleDownload(int priority, Callable<Boolean> runnable) throws InterruptedException {
		WaittingResult<Boolean> ret = new WaittingResult<>(runnable);
		downloadQueue.offer(new WithPriority<>(priority, ret));
		return ret;
	}

	/**
	 * @return true if the image was never crawled and the image is valid.
	 * @throws SQLException
	 */
	public boolean downloadAndRecord(ImageInfo image) throws SQLException {
		if (isExistOnSqlAndAlarm(image)) {
			return false;
		}
		if (!image.isValidImageId()) {
			System.out.println("Invalid " + image.getImageId());
			return false;
		}
		Iterator<InputStream> iterator = image.getImages();
		File parent = new File(saveImageDir, image.getArtistId());
		int count = image.getImageCount();
		for (int i = 0; iterator.hasNext(); i++) {
			try (InputStream input = iterator.next()) {
				File imagefile = new File(parent, image.getImageId() + (count > 1 ? "_p" + i : "") + FileTypeUtil.getFileType(input).getSuffix());
				System.out.println("Download " + imagefile.getPath());
				IOUtil.saveImage(imagefile, input, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (iterator.hasNext()) {
				delay(500, TimeUnit.MILLISECONDS);
			}
		}
		imageInfoTable.insert(image);
		image.setBookmark();
		return true;
	}

	/** @param savefile create File path without suffix name */
	public void download(ImageInfo image, Function<Integer, File> savefile) {
		Iterator<InputStream> iterator = image.getImages();
		for (int i = 0; iterator.hasNext(); i++) {
			try (InputStream input = iterator.next()) {
				IOUtil.saveImage(new File(savefile.apply(i).getAbsolutePath() + FileTypeUtil.getFileType(input).getSuffix()), input, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected CookieStore decryptCookieStore() throws IOException, DecryptException {
		File copyCookies = new File("originCookies");
		File copyMasterKey = new File("originMasterkey");
		File originCookies = new File(CookieLoader.WINDOWS_CHROME_COOKIE_PATH);
		File originMasterKey = new File(CookieLoader.WINDOWS_CHROME_MASTER_KEY_PATH);
		IOUtil.copy(originCookies, copyCookies);
		IOUtil.copy(originMasterKey, copyMasterKey);
		CookieStore cookstore = CookieLoader.readChromeCookies(copyCookies, copyMasterKey, "host_key LIKE '%pixiv%'");
		copyCookies.delete();
		copyMasterKey.delete();
		return cookstore;
	}

	public void delay(long time, TimeUnit unit) {
		try {
			schedule.schedule(() -> {
				return true;
			}, time, unit).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	protected void initSQL(File database) throws IOException, SQLException {
		IOUtil.tryCreateFile(database);
		String databaseUrl = "jdbc:sqlite:" + database.getAbsolutePath();
		this.databaseConnection = DriverManager.getConnection(databaseUrl);
		this.imageInfoTable = new ImaeInfoTable(databaseConnection, "image_info");
		imageInfoTable.create();
		this.flagTable = new FlagTable(databaseConnection, "flag");
		flagTable.create();
		System.out.println("initialized SQL");
	}

	protected void initPixiv(File httpCookies) throws IOException, DecryptException {
		if (httpCookies.exists()) {
			IOUtil.loadObjects(httpCookies, (in) -> httpHandle = (PixivHttpHandle) in.readObject());
		} else {
			this.httpHandle = new PixivHttpHandle(decryptCookieStore());
		}
		this.pixiv = new HPixiv(httpHandle);
		System.out.println("initialized pixiv");
	}

	protected void initSchedule() {
		this.schedule = Executors.newScheduledThreadPool(4);
		this.downloadQueue = new PriorityBlockingQueue<>(1024, (a, b) -> Integer.compare(b.priority(), a.priority()));
		this.waitingId = Collections.synchronizedSet(new HashSet<>());
		schedule.scheduleAtFixedRate(this::downloadFollowingArtistWorks, 0, 1, TimeUnit.DAYS);
		schedule.scheduleAtFixedRate(this::downloadNewestWorks, 0, 2, TimeUnit.HOURS);
		schedule.scheduleAtFixedRate(() -> System.out.println("DOWNLOAD QUEUE LEFt: " + downloadQueue.size()), 5, 5, TimeUnit.MINUTES);
		schedule.scheduleWithFixedDelay(() -> {
			try {
				while (!downloadQueue.isEmpty()) {
					if (!downloadQueue.poll().attached().call()) {
						delay(1, TimeUnit.SECONDS);
					} else {
						return;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 0, 5, TimeUnit.SECONDS);
		System.out.println("initialized schedule");
	}

	public static Runnable catchException(Runnable runnable, Consumer<Exception> catcher) {
		Runnable runnableCatcher = () -> {
			try {
				runnable.run();
			} catch (Exception e) {
				catcher.accept(e);
			}
		};
		return runnableCatcher;
	}

	@Override
	public void close() {
		try {
			IOUtil.saveObjects(httpCookies, (out) -> out.writeObject(pixiv.getHttpHandle()));
			if (pixiv != null)
				pixiv.getHttpHandle().close();
			if (databaseConnection != null)
				databaseConnection.close();
			if (schedule != null)
				schedule.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
