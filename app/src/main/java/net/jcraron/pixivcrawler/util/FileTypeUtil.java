package net.jcraron.pixivcrawler.util;

import java.io.IOException;
import java.io.InputStream;

public class FileTypeUtil {
	public enum FileType {
		JPEG("FFD8FF", ".jpg"), PNG("89504E47", ".png"), GIF("47494638", ".gif"), TIFF("49492A00", ".tif"), BMP("424D", ".bmp"), CAD("41433130", ".dwg"), PSD("38425053", ".psd"),
		RTF("7B5C727466", ".rtf"), XML("3C3F786D6C", ".xml"), HTML("68746D6C3E", ".html"), ZIP("504B0304", ".zip"), RAR("52617221", ".rar"), WAV("57415645", ".wav"), AVI("41564920", ".avi"),
		NONE(new byte[0], "");

		private String suffix;
		private byte[] code;

		FileType(byte[] code, String suffix) {
			this.suffix = suffix;
			this.code = code;
		}

		FileType(String code, String suffix) {
			this.suffix = suffix;
			byte[] bytes = new byte[code.length() / 2];
			for (int i = 0, off = 0; i < bytes.length; i++, off += 2) {
				bytes[i] = (byte) Integer.parseInt(code.substring(off, off + 2), 16);
			}
			this.code = bytes;
		}

		public boolean isSameType(byte[] other) {
			int len = this.code.length;
			if (len < other.length) {
				return false;
			}
			for (int i = 0; i < len; i++) {
				if (this.code[i] != other[i]) {
					return false;
				}
			}
			return true;
		}

		public boolean isSameType(InputStream stream) throws IOException {
			int len = this.code.length;
			byte[] otherBytes = new byte[len];
			stream.mark(len);
			stream.read(otherBytes);
			stream.reset();
			return this.isSameType(otherBytes);
		}

		public String getSuffix() {
			return suffix;
		}
	}

	public static FileType getFileType(byte[] bytes) {
		for (FileType type : FileType.values()) {
			if (type != FileType.NONE && type.isSameType(bytes)) {
				return type;
			}
		}
		return FileType.NONE;
	}

	public static FileType getFileType(InputStream stream) throws IOException {
		for (FileType type : FileType.values()) {
			if (type != FileType.NONE && type.isSameType(stream)) {
				return type;
			}
		}
		return FileType.NONE;
	}
}
