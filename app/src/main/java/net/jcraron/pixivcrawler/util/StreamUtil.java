package net.jcraron.pixivcrawler.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class StreamUtil {

	public static void readZipedStream(InputStream originZip, ZipOutputStream zip) throws IOException {
		try (ZipInputStream originZip_reader = new ZipInputStream(originZip)) {
			ZipEntry originZipEntry = null;
			while ((originZipEntry = originZip_reader.getNextEntry()) != null) {
				zip.putNextEntry(originZipEntry);
				for (int i = 0; i < originZipEntry.getSize(); i++) {
					zip.write(originZip_reader.read());
				}
				zip.closeEntry();
			}
		}
	}
	
}
