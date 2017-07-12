package ua.ci.file_replacer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import static java.lang.System.out;

public class ReplacerThread {
	private final Path inputDir;
	private final Path outputDir;

	private boolean hasRecursive;
	private boolean hasReplace;
	private boolean onlyThumbs;

	private ArrayList<Path> files;

	public ReplacerThread(String oldDirPath, String newDirPath) {
		this.inputDir = Paths.get(oldDirPath);
		this.outputDir = Paths.get(newDirPath);

		files = new ArrayList<Path>();
	}

	public ReplacerThread(String oldDirPath, String newDirPath, boolean hasRecursive, boolean hasReplace,
			boolean onlyThumbs) {
		this(oldDirPath, newDirPath);
		this.hasRecursive = hasRecursive;
		this.hasReplace = hasReplace;
		this.onlyThumbs = onlyThumbs;
	}

	public void compress() throws IOException {
		runListFiles();

		createTarGZ(Paths.get("//home//pc2//del//arch//media.tar.gz"));

	}

	public void createTarGZ(Path file) throws FileNotFoundException, IOException {
		// FileOutputStream fOut = null;
		// BufferedOutputStream bOut = null;
		// GzipCompressorOutputStream gzOut = null;
		// TarArchiveOutputStream tOut = null;fsdfsdfsdf

		System.out.println(new File(".").getAbsolutePath());
		System.out.println(new File(".").getAbsolutePath());
		String tarGzPath = "//home//pc2//del//arch//media.tar.gz";
		try (final FileOutputStream fOut = new FileOutputStream(new File(tarGzPath));
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

	private void addFileToTarGz(TarArchiveOutputStream tOut, String path, String base) throws IOException {
		File f = new File(path);
		System.out.println(f.exists());
		String entryName = base + f.getName();
		TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
		tOut.putArchiveEntry(tarEntry);

		if (f.isFile()) {
			IOUtils.copy(new FileInputStream(f), tOut);
			tOut.closeArchiveEntry();
		} else {
			tOut.closeArchiveEntry();
			File[] children = f.listFiles();
			if (children != null) {
				for (File child : children) {
					System.out.println(child.getName());
					addFileToTarGz(tOut, child.getAbsolutePath(), entryName + "/");
				}
			}
		}
	}

	private void addFileToTarGz2(TarArchiveOutputStream tOut, Path filePath, String base) throws IOException {

		String entryName = base + filePath.getFileName();
		TarArchiveEntry tarEntry = new TarArchiveEntry(filePath.toFile(), entryName);
		String filePostfix = "";
		{
			int fileSubCount = filePath.getNameCount();
			int subStart = fileSubCount - 5;
			if (!filePath.toString().contains("thumb")) {
				subStart++;
			}
			filePostfix = filePath.subpath(subStart, fileSubCount).toString();
		}

		tarEntry.setName(filePostfix);
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
				if (!filePath.toString().contains("thumb")) {
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
			if (onlyThumbs) {
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

						if (onlyThumbs) {
							if (entry.toString().contains("thumbs")) {
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
