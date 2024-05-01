package net.jcraron.pixivcrawler.controller;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import net.jcraron.pixivcrawler.util.exception.DecryptException;

public class MainController {
	public static void main(File mainDir) throws InterruptedException, SQLException, IOException, DecryptException {
		try (PixivDownloadController con = new PixivDownloadController(new File(mainDir, "images"), new File(mainDir, "database.sqlite"), new File(mainDir, "cookies"))) {
			synchronized (MainController.class) {
				MainController.class.wait();
			}
		}
	}
}
