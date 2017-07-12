package ua.ci.file_replacer;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.logging.Logger;

import static java.lang.System.out;

public class ReplacerThread {
	private final Path inputDir;
	private final Path outputDir;

	private boolean hasRecursive;
	private boolean hasReplace;

	private ArrayList<Path> files;

	public ReplacerThread(String oldDirPath, String newDirPath) {
		this.inputDir = Paths.get(oldDirPath);
		this.outputDir = Paths.get(newDirPath);

		files = new ArrayList<Path>();
	}

	public ReplacerThread(String oldDirPath, String newDirPath, boolean hasRecursive, boolean hasReplace) {
		this(oldDirPath, newDirPath);
		this.hasRecursive = hasRecursive;
		this.hasReplace = hasReplace;
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
			files.add(inputDir);
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
						files.add(entry);
					}

				}
			}
		}
	}

	Logger log = Logger.getLogger(this.getClass().getName());
}
