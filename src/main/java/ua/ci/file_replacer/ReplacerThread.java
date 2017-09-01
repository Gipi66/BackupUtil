package ua.ci.file_replacer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.SocketException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import net.anotheria.moskito.aop.annotation.Monitor;

@Monitor
public class ReplacerThread {
	Logger log = Logger.getLogger(this.getClass().getSimpleName());

	@SuppressWarnings("unused")
	private final Path inputDir;
	private final Path outputDir;

	@SuppressWarnings("unused")
	private boolean hasRecursive;
	@SuppressWarnings("unused")
	private boolean hasReplace;
	@SuppressWarnings("unused")
	private boolean isFilter;

	private String filter;

	private final FTPClient ftp;

	private String mediaName;
	private boolean isMedia;

	private String archive_path;
	private String extract_to_path;
	private Set<String> files;
	private Properties props;

	private String media_path;

	private ArrayList<String> sendingFilesPath;
	{
		sendingFilesPath = new ArrayList<String>();
		Path externalFilesDir = Paths.get("send files");
		for (int attempt = 0; attempt < 2; attempt++) {
			if (Files.notExists(externalFilesDir)) {
				try {
					Files.createDirectory(externalFilesDir);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try (DirectoryStream<Path> externalFilesDirChildrens = Files.newDirectoryStream(externalFilesDir)) {
					Properties prop;
					for (Path externalFilesChildren : externalFilesDirChildrens) {
						log.info(externalFilesChildren.toAbsolutePath().toString());
						prop = new Properties();

						try (InputStream is = Files.newInputStream(externalFilesChildren);
								Reader reader = new InputStreamReader(is)) {
							prop.load(reader);
						}

						String path = prop.getProperty("path");
						sendingFilesPath.add(path);
					}
					log.info(sendingFilesPath.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}

				break;
			}
		}

	}

	public ReplacerThread(String oldDirPath, String newDirPath, Properties props) {
		this.inputDir = Paths.get(oldDirPath);
		this.outputDir = Paths.get(props.getProperty("outfile_path"));
		sendingFilesPath.add(outputDir.toAbsolutePath().toString());
		this.media_path = props.getProperty("directory_path");

		this.props = props;

		// create dirs
		if (!Files.exists(outputDir.getParent())) {
			try {
				Files.createDirectories(outputDir.getParent());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		ftp = new FTPClient();

		files = new HashSet<String>();
	}

	public ReplacerThread(String oldDirPath, String newDirPath, boolean hasRecursive, boolean hasReplace, String filter,
			String mediaName, Properties props) {

		this(props.getProperty("directory_path"), props.getProperty("outfile_path"), props);

		this.hasRecursive = hasRecursive;

		this.hasReplace = hasReplace;

		this.filter = props.getProperty("filter_name");
		this.isFilter = (this.filter.equals(null) || this.filter.equals("")) ? false : true;

		this.mediaName = props.getProperty("main_dir_name");
		this.isMedia = (this.mediaName.equals(null) || this.mediaName.equals("")) ? false : true;

		this.archive_path = props.getProperty("archive_path");
		this.extract_to_path = props.getProperty("extract_to_path");

	}

	public void compress() throws IOException {
		Persistance pers = new Persistance();
		files = new HashSet<String>(pers.getResultPaths());
		pers = null;
		createTarGZ();
		sendFile();
	}

	public void sendFile() throws SocketException, IOException {
		{
			String ftpUrl = props.getProperty("ftp_server");
			String ftpLogin = props.getProperty("ftp_login");
			String ftpPassword = props.getProperty("ftp_password");

			ftp.connect(ftpUrl);
			ftp.login(ftpLogin, ftpPassword);
		}
		File file = null;
		for (int i = 0; i < sendingFilesPath.size(); file = new File(sendingFilesPath.get(i))) {
			String ftp_dump = "//backup_ciua//" + file.getName();

			int reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				System.err.println("FTP server refused connection.");
			}

			ftp.deleteFile(ftp_dump);

			ftp.setFileType(FTP.BINARY_FILE_TYPE);

			DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

			ftp.storeFile(ftp_dump, dis);

			// after all
		}
		ftp.disconnect();
	}

	public void decompress() {
		decompressTarGz();
	}

	private void decompressTarGz() {
		try (FileInputStream fin = new FileInputStream(archive_path);
				BufferedInputStream bis = new BufferedInputStream(fin);
				GzipCompressorInputStream gzIn = new GzipCompressorInputStream(bis);
				TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn);) {

			ArchiveEntry entry = null;

			while ((entry = tarIn.getNextTarEntry()) != null) {
				File curfile = new File(extract_to_path, entry.getName());
				log.info(curfile.toString());
				File parent = curfile.getParentFile();
				if (!parent.exists()) {
					parent.mkdirs();
				}
				OutputStream out = new FileOutputStream(curfile);
				IOUtils.copy(tarIn, out);
				out.close();
			}

			fin.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
	}

	public void createTarGZ() throws FileNotFoundException, IOException {
		System.out.println("createTarGZ");
		try (final FileOutputStream fOut = new FileOutputStream(outputDir.toFile());
				final BufferedOutputStream bOut = new BufferedOutputStream(fOut);
				final GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(bOut);
				final TarArchiveOutputStream tOut = new TarArchiveOutputStream(gzOut);) {

			tOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			files.stream().forEach(i -> {
				try {
					addFileToTarGz2(tOut, media_path + "//" + i, "");
				} catch (IOException e) {
					e.printStackTrace();

				}
			});
			tOut.finish();
		}
	}

	private void addFileToTarGz2(TarArchiveOutputStream tOut, String filePathString, String base) throws IOException {

		String filePostfix = "";
		{

			filePostfix = filePathString;
			if (isMedia) {
				String[] filePostfixSplit = filePostfix.split(mediaName);
				filePostfix = filePostfixSplit[filePostfixSplit.length - 1];
			}
		}

		Path filePath = Paths.get(filePathString);
		File file = filePath.toFile();

		if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
			TarArchiveEntry tarEntry = new TarArchiveEntry(file, filePostfix);

			tOut.putArchiveEntry(tarEntry);
			try (InputStream is = Files.newInputStream(filePath)) {
				IOUtils.copy(is, tOut);
			}

			tOut.closeArchiveEntry();

			log.info("Added entity: " + file.getAbsolutePath());
		} else {
			log.warning(" ELSE: " + file.getAbsolutePath());
		}
	}

}
