package net.jcraron.pixivcrawler.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;

public class IOUtil {
	public static void saveImage(File file, byte[] image, boolean overwrite) throws IOException {
		if (!overwrite && file.exists()) {
			return;
		}
		tryCreateFile(file);
		try (FileOutputStream stream = new FileOutputStream(file, false)) {
			stream.write(image);
		}
	}

	public static void saveImage(File file, InputStream input, boolean overwrite) throws IOException {
		if (!overwrite && file.exists()) {
			return;
		}
		tryCreateFile(file);
		try (FileOutputStream stream = new FileOutputStream(file, false)) {
			input.transferTo(stream);
		}
	}

	public static void tryCreateFile(File file) throws IOException {
		if (!file.exists()) {
			File parent = file.getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}
			file.createNewFile();
		}
	}

	public static String readText(File file) throws IOException {
		String result = "";
		try (FileInputStream stream = new FileInputStream(file)) {
			result = new String(stream.readAllBytes());
		}
		return result;
	}

	public static void saveObjects(File file, ConsumerThrowable<ObjectOutputStream> consumer) throws IOException {
		tryCreateFile(file);
		try (FileOutputStream fileout = new FileOutputStream(file); ObjectOutputStream output = new ObjectOutputStream(fileout)) {
			consumer.accept(output);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	public static void loadObjects(File file, ConsumerThrowable<ObjectInputStream> consumer) throws IOException {
		tryCreateFile(file);
		try (FileInputStream filein = new FileInputStream(file); ObjectInputStream output = new ObjectInputStream(filein)) {
			consumer.accept(output);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	public static void copy(File source, File dest) throws IOException {
		tryCreateFile(dest);
		try (FileInputStream inputStream = new FileInputStream(source); FileOutputStream outputStream = new FileOutputStream(dest)) {
			FileChannel inputChannel = inputStream.getChannel();
			FileChannel outputChannel = outputStream.getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		}
	}
}
