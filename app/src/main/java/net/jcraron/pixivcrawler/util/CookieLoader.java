package net.jcraron.pixivcrawler.util;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;

import com.sun.jna.platform.win32.Crypt32Util;

import net.jcraron.pixivcrawler.util.exception.DecryptException;

public class CookieLoader {
	public final static String WINDOWS_CHROME_COOKIE_PATH = System.getProperty("user.home") + "/AppData/Local/Google/Chrome/User Data/Default/Network/Cookies";
	public final static String WINDOWS_CHROME_MASTER_KEY_PATH = System.getProperty("user.home") + "/AppData/Local/Google/Chrome/User Data/Local State";

	public static CookieStore readChromeCookies(File cookiesFile, File masterKeyFile, @Nullable String sqlWhereAppend) throws IOException, DecryptException {
		CookieStore cookiestore = new BasicCookieStore();
		String url = "jdbc:sqlite:" + cookiesFile.getAbsolutePath();
		try (Connection connection = DriverManager.getConnection(url)) {
			PreparedStatement ps = connection
					.prepareStatement("SELECT %s , %s , %s FROM %s %s".formatted("host_key", "name", "encrypted_value", "cookies", sqlWhereAppend != null ? "WHERE " + sqlWhereAppend : "").toString());
			try (ResultSet result = ps.executeQuery()) {
				byte[] masterKey = getChromeMasterKey(masterKeyFile);
				while (result.next()) {
					String key = result.getString("name");
					String value = decryptChromeCookie(result.getBytes("encrypted_value"), masterKey);
					BasicClientCookie cookie = new BasicClientCookie(key, value);
					cookie.setDomain(result.getString("host_key"));
					cookiestore.addCookie(cookie);
				}
			}
		} catch (

		SQLException e) {
			e.printStackTrace();
		}
		return cookiestore;
	}

	/**
	 * https://stackoverflow.com/questions/65939796/java-how-do-i-decrypt-chrome-cookies
	 * 
	 * @throws DecryptException
	 */
	private static String decryptChromeCookie(byte[] encryptedData, byte[] masterKey) throws DecryptException {
		// Separate praefix (v10), nonce and ciphertext/tag
		byte[] nonce = Arrays.copyOfRange(encryptedData, 3, 3 + 12);
		byte[] ciphertextTag = Arrays.copyOfRange(encryptedData, 3 + 12, encryptedData.length);
		try {
			// Decrypt
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, nonce);
			SecretKeySpec keySpec = new SecretKeySpec(masterKey, "AES");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
			byte[] cookie = cipher.doFinal(ciphertextTag);
			return new String(cookie);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new DecryptException(e.getClass().getName() + " : " + e.getMessage());
		}
	}

	private static byte[] getChromeMasterKey(File masterKeyFile) throws IOException {
		String encryptedMasterKeyWithPrefixB64 = IOUtil.readText(masterKeyFile);
		encryptedMasterKeyWithPrefixB64 = new JSONObject(encryptedMasterKeyWithPrefixB64).getJSONObject("os_crypt").getString("encrypted_key");

		// Remove praefix (DPAPI)
		byte[] encryptedMasterKeyWithPrefix = Base64.getDecoder().decode(encryptedMasterKeyWithPrefixB64);
		byte[] encryptedMasterKey = Arrays.copyOfRange(encryptedMasterKeyWithPrefix, 5, encryptedMasterKeyWithPrefix.length);
		// Decrypt
		byte[] masterKey = Crypt32Util.cryptUnprotectData(encryptedMasterKey);

		return masterKey;
	}
}
