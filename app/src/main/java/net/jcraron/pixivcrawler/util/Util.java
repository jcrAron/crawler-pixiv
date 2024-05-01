package net.jcraron.pixivcrawler.util;

import java.util.zip.CRC32;
import java.util.zip.Deflater;

public class Util {
	public static int deflate(byte[] input, byte[] output) {
		Deflater deflater = new Deflater();
		deflater.setInput(input);
		deflater.finish();
		int size = deflater.deflate(output);
		deflater.end();
		return size;
	}

	public static long crc32(byte[] data) {
		CRC32 crc32 = new CRC32();
		crc32.update(data);
		return crc32.getValue();
	}
}
