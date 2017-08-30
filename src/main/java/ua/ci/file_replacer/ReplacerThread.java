package ua.ci.file_replacer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
	private final Path inputDir;
	private final Path outputDir;

	@SuppressWarnings("unused")
	private boolean hasRecursive;
	@SuppressWarnings("unused")
	private boolean hasReplace;
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

	public ReplacerThread(String oldDirPath, String newDirPath, Properties props) {
		this.inputDir = Paths.get(oldDirPath);
		this.outputDir = Paths.get(props.getProperty("outfile_path"));

		this.media_path = props.getProperty("directory_path");

		this.props = props;

		// create dirs
		if (!Files.exists(outputDir.getParent())) {
			try {
				Files.createDirectories(outputDir.getParent());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		ftp = new FTPClient();

		files = new HashSet<String>();
	}

	public ReplacerThread(String oldDirPath, String newDirPath, boolean hasRecursive, boolean hasReplace, String filter,
			String mediaName, Properties props) {

		this(props.getProperty("directory_path"), props.getProperty("outfile_path"), props);
		// TODO
		this.hasRecursive = hasRecursive;
		// TODO
		this.hasReplace = hasReplace;

		this.filter = props.getProperty("filter_name");
		this.isFilter = (this.filter.equals(null) || this.filter.equals("")) ? false : true;

		this.mediaName = props.getProperty("main_dir_name");
		this.isMedia = (this.mediaName.equals(null) || this.mediaName.equals("")) ? false : true;

		this.archive_path = props.getProperty("archive_path");
		this.extract_to_path = props.getProperty("extract_to_path");

	}

	public void compress() throws IOException {
		// runListFiles();
		Persistance pers = new Persistance();
		files = new HashSet<String>(pers.getResultPaths());
		pers = null;
		createTarGZ();
		sendFile();
	}

	public void sendFile() throws SocketException, IOException {

		String ftp_dump = "//backup_ciua//" + outputDir.getFileName();

		{
			String ftpUrl = props.getProperty("ftp_server");
			String ftpLogin = props.getProperty("ftp_login");
			String ftpPassword = props.getProperty("ftp_password");

			ftp.connect(ftpUrl);
			ftp.login(ftpLogin, ftpPassword);
		}
		int reply = ftp.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			ftp.disconnect();
			System.err.println("FTP server refused connection.");
		}

		ftp.deleteFile(ftp_dump);

		ftp.setFileType(FTP.BINARY_FILE_TYPE);
		// log.info(String.format("%s\n %s", Arrays.toString(files),
		// files[1].getName()));
		// ArrayList<FTPFile> fileList = new ArrayList<FTPFile>(Arrays.asList(files));
		// fileList.parallelStream().forEach(i-> log.info(i.getName()));
		InputStream is = new FileInputStream(outputDir.toFile());

		ftp.storeFile(ftp_dump, is);

		// after all
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
					addFileToTarGz2(tOut, new File(media_path +"//"+ i), "");
				} catch (IOException e) {
					e.printStackTrace();

				}
			});
			tOut.finish();
		} finally {

			// tOut.close();
			// gzOut.close();
			// bOut.close();
			// fOut.close();
		}
	}

	private void addFileToTarGz2(TarArchiveOutputStream tOut, File filePath, String base) throws IOException {
		// log.info("Started: %s" + filePath.getAbsolutePath());
		String filePostfix = "";
		{

			filePostfix = filePath.toString();
			if (isMedia) {
				String[] filePostfixSplit = filePostfix.split(mediaName);
				filePostfix = filePostfixSplit[filePostfixSplit.length - 1];
			}
		}

		if (filePath.exists() && filePath.isFile()) {
			TarArchiveEntry tarEntry = new TarArchiveEntry(filePath, filePostfix);

			tOut.putArchiveEntry(tarEntry);

			IOUtils.copy(new FileInputStream(filePath), tOut);
			tOut.closeArchiveEntry();

			log.info("Added entity: " + filePath.getAbsolutePath());
		} else {
			log.warning(" ELSE: " + filePath.getAbsolutePath());
		}
	}

	Logger log = Logger.getLogger(this.getClass().getName());
}
