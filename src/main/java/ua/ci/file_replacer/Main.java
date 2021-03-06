/**
 * 
 */
package ua.ci.file_replacer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;

/**
 * @author Sergey
 *
 */
public class Main {
	public static void main(String[] args) {
		Properties props = loadProps();
		if (props == null) {
			System.exit(0);
		}
		long startTime = new Date().getTime();
		ReplacerThread repl = new ReplacerThread("/home/pc2/realt/realtyboard/media", "/home/pc2/del/media", true, true,
				"", "media", props);

		try {
			repl.compress();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("total time: " + new Date(new Date().getTime() - startTime).getTime() / 1000);
	}

	public static Properties loadProps() {
		Properties props = new Properties();
		Path propsPath = Paths.get("conf.ini");
		if (Files.notExists(propsPath)) {
			createProps();
			return null;
		}
		try {
			props.load(new FileInputStream("conf.ini"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] reqKeys = new String[] { "directory_path", "outfile_path", "filter_name", "main_dir_name",
				"archive_path", "extract_to_path", "ftp_server", "ftp_password", "ftp_login" };
		for (String key : reqKeys) {
			if (!props.containsKey(key)) {
				System.out.printf(
						"Error: Please add a \"%s\" to the configuration file and try run again.\nTo set the default configuration, delete \"%s\" file.",
						key, propsPath.toAbsolutePath().toString());

				return null;
			}
		}
		return props;
	}

	public static void createProps() {
		File file = new File("conf.ini");

		RandomAccessFile raf;
		OutputStream out = null;
		try {
			raf = new RandomAccessFile(file, "rws");
			out = Channels.newOutputStream(raf.getChannel());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Properties compressProps = new Properties();
		Properties deCompressProps = new Properties();

		compressProps.put("directory_path", "//home//pc2//realt//realtyboard//media");
		compressProps.put("outfile_path", "//home//pc2//del//arch//media.tar.gz");
		compressProps.put("filter_name", "");

		compressProps.put("ftp_password", "");
		compressProps.put("ftp_login", "");
		compressProps.put("ftp_server", "");
		compressProps.put("ftp_dir", "//backup_ciua//");
		compressProps.put("main_dir_name", "media");

		deCompressProps.put("archive_path", "//home//pc2//del//arch//media.tar.gz");
		deCompressProps.put("extract_to_path", "//home//pc2//del//arch//media");

		try {
			compressProps.store(out, "Configuration for creat archive");
			deCompressProps.store(out, "Configuration for creat archive");
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.printf("Default config file has generated (%s).\n", file.getAbsolutePath());

	}
}
