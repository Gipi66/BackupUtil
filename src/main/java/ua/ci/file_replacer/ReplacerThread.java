package ua.ci.file_replacer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

public class ReplacerThread {
	private final Path inputDir;
	private final Path outputDir;

	private boolean hasRecursive;
	private boolean hasReplace;
	private boolean isFilter;

	private String filter;

	private String mediaName;
	private boolean isMedia;

	private String archive_path;
	private String extract_to_path;
	private ArrayList<Path> files;

	public ReplacerThread(String oldDirPath, String newDirPath) {
		this.inputDir = Paths.get(oldDirPath);
		this.outputDir = Paths.get(newDirPath);

		files = new ArrayList<Path>();
	}

	public ReplacerThread(String oldDirPath, String newDirPath, boolean hasRecursive, boolean hasReplace, String filter,
			String mediaName, Properties props) {

		this(props.getProperty("directory_path"), props.getProperty("outfile_path"));
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
		runListFiles();
		createTarGZ(Paths.get("//home//pc2//del//arch//media.tar.gz"));
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

	public void createTarGZ(Path file) throws FileNotFoundException, IOException {

		try (final FileOutputStream fOut = new FileOutputStream(new File(archive_path));
				final BufferedOutputStream bOut = new BufferedOutputStream(fOut);
				final GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(bOut);
				final TarArchiveOutputStream tOut = new TarArchiveOutputStream(gzOut);) {

			tOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			files.stream().forEach(i -> {
				try {
					addFileToTarGz2(tOut, i, "");
				} catch (IOException e) {
					// TODO Auto-generated catch block
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

	private void addFileToTarGz2(TarArchiveOutputStream tOut, Path filePath, String base) throws IOException {

		String filePostfix = "";
		{

			filePostfix = filePath.toString();
			if (isMedia) {
				String[] filePostfixSplit = filePostfix.split(mediaName);
				filePostfix = filePostfixSplit[filePostfixSplit.length - 1];
			}
		}

		TarArchiveEntry tarEntry = new TarArchiveEntry(filePath.toFile(), filePostfix);

		tOut.putArchiveEntry(tarEntry);

		if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
			IOUtils.copy(new FileInputStream(filePath.toFile()), tOut);
			tOut.closeArchiveEntry();

		}
	}

	public void run() {
		runListFiles();
		replaceFiles();
	}

	private void replaceFiles() {
		files.parallelStream().forEach(filePath -> {
			Path newFilePath = null;

			{
				int fileSubCount = filePath.getNameCount();
				int subStart = fileSubCount - 5;
				if (!filePath.toString().contains(filter)) {
					subStart++;
				}
				String filePostfix = filePath.subpath(subStart, fileSubCount).toString();
				newFilePath = outputDir.resolve(filePostfix);
			}
			if (Files.exists(filePath) && newFilePath != null && newFilePath != null) {
				Path parentDir = newFilePath.getParent();
				if (!Files.exists(parentDir)) {
					try {
						Files.createDirectories(parentDir);
					} catch (IOException e) {
						// fail to create directory
						e.printStackTrace();
					}
				}
				try {
					log.info(String.format("%s %s", filePath, newFilePath));
					Files.move(filePath, newFilePath);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	private void runListFiles() {
		if (Files.isDirectory(inputDir)) {
			try {
				listFiles(inputDir);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (Files.isRegularFile(inputDir)) {
			if (isFilter) {
				if (inputDir.toString().contains("thumbs")) {
					files.add(inputDir);
				}
			} else {
				files.add(inputDir);
			}
		}
	}

	private void listFiles(Path path) throws IOException {

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entry : stream) {
				if (Files.exists(entry)) {
					if (Files.isDirectory(entry)) {
						try {
							listFiles(entry);
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else if (!Files.isDirectory(entry)) {

						if (isFilter) {
							if (entry.toString().contains(filter)) {
								files.add(entry);
							}
						} else {
							files.add(entry);
						}
					}

				}
			}
		}
	}

	Logger log = Logger.getLogger(this.getClass().getName());
}
