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
 * @author pc2
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Properties props = loadProps();
		long startTime = new Date().getTime();
		ReplacerThread repl = new ReplacerThread("/home/pc2/realt/realtyboard/media", "/home/pc2/del/media", true, true,
				"", "media", props);

		// repl.run();
		try {
			repl.compress();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		repl.decompress();
		System.out.println("total time: " + new Date(new Date().getTime() - startTime).getTime() / 1000);
	}

	public static Properties loadProps() {
		Properties props = new Properties();
		Path propsPath = Paths.get("conf.ini");
		if (Files.notExists(propsPath)) {
			createProps();
		}
		try {
			props.load(new FileInputStream("conf.ini"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String[] reqKeys = new String[] { "directory_path", "outfile_path", "filter_name", "main_dir_name",
				"archive_path", "extract_to_path" };
		for (String key : reqKeys) {
			if (!props.containsKey(key)) {
				System.out.printf(
						"Error: Please add a \"%s\" to the configuration file and try run again.\nTo set the default configuration, delete \"%s\" file.",
						key, propsPath.toAbsolutePath().toString());

				System.exit(1);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Properties compressProps = new Properties();
		Properties deCompressProps = new Properties();

		compressProps.put("directory_path", "//home//pc2//realt//realtyboard//media");
		compressProps.put("outfile_path", "//home//pc2//del//arch//media.tar.gz");
		compressProps.put("filter_name", "");
		compressProps.put("main_dir_name", "media");

		deCompressProps.put("archive_path", "//home//pc2//del//arch//media.tar.gz");
		deCompressProps.put("extract_to_path", "//home//pc2//del//arch//media");

		try {
			compressProps.store(out, "Configuration for creat archive");
			deCompressProps.store(out, "Configuration for creat archive");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
