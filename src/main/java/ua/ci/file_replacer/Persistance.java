package ua.ci.file_replacer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

public class Persistance {

	private final ArrayList<Properties> dbFilterPropsList = new ArrayList<Properties>();
	private Properties dbProperties = new Properties();;

	private static ArrayList<String> resultPaths = new ArrayList<String>();

	public Persistance() {

		{
			try {
				loadDbFilters();
				loadDbConfig();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// log.warning(e.getMessage());
			}
		}
	}

	private ArrayList<Properties> getDBPropsList() {
		return dbFilterPropsList;
	}

	public void showDBPropsList() {
		if (getDBPropsList() != null) {
			for (Properties prop : getDBPropsList()) {
				log.info(String.format("%s", prop.toString()));
			}
		}
	}

	// singleton
	public ArrayList<String> getResultPaths() {
		ArrayList<String> result = null;
		if (resultPaths == null || resultPaths.size() == 0) {
			this.showDBPropsList();
			try {
				this.connectDB();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			result = resultPaths;
		} else {
			result = resultPaths;
		}
		log.info(String.format("Returned %s elements", result.size()));
		return result;
	}

	private void loadDbFilters() throws IOException {
		Path dbFiltersDir = Paths.get("SQL filters");
		if (!Files.exists(dbFiltersDir)) {
			Files.createDirectories(dbFiltersDir);
		}

		{

			DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
				@Override
				public boolean accept(Path file) throws IOException {
					return (Files.isRegularFile(file));
				}
			};

			DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dbFiltersDir, filter);

			for (Path path : directoryStream) {
				Properties prop = new Properties();

				String strPath = path.toString();
				log.info("Loadings dbFilterProp: " + strPath);

				prop.load(new FileInputStream(strPath));
				dbFilterPropsList.add(prop);
			}
		}
	}

	private void loadDbConfig() throws IOException, NullPointerException {
		Path dbConfFile = Paths.get("db.conf");
		if (Files.exists(dbConfFile)) {
			dbProperties.load(Files.newInputStream(dbConfFile));
		} else {
			throw new NullPointerException(String.format("File \"%s\" not exist!", dbConfFile.toAbsolutePath()));
		}

	}

	private void connectDB() throws SQLException {
		System.out.println("connectDB");

		// connection properties
		String url = dbProperties.getProperty("db.url");
		String name = dbProperties.getProperty("db.login");
		String password = dbProperties.getProperty("db.password");

		// test
		log.info(String.format("url %s, name %s, password %s", url, name, password));

		// add Driver
		String portgresDriverName = "org.postgresql.Driver";
		try {
			Class.forName(portgresDriverName);
		} catch (ClassNotFoundException e) {
			log.warning(String.format("Can't found class \"%s\"", portgresDriverName));
		}
		// Start connection
		try (Connection conn = DriverManager.getConnection(url, name, password)) {
			conn.setAutoCommit(false);
			try (Statement st = conn.createStatement()) {

				st.setFetchSize(50);
				for (Properties prop : getDBPropsList()) {
					getData(st, prop);
				}
			}

		}

	}

	private void getData(Statement st, Properties prop) throws SQLException {

		System.out.println("conn");
		String sql = prop.getProperty("sql");

		// Turn use of the cursor on.
		log.info(sql);
		try (ResultSet rs = st.executeQuery(sql)) {
			// System.out.println("while");
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			while (rs.next()) {
				for (int i = 1; i <= columnsNumber; i++) {
					resultPaths.add(rs.getString(i));
				}

			}
		}
	}

	public void showResultPaths() {
		Random rnd = new Random();
		System.out.println(String.format("count: %s, random el:\n%s\n", resultPaths.size(),
				resultPaths.get(rnd.nextInt(resultPaths.size()))));
	}

	Logger log = Logger.getLogger(this.getClass().getName());
}
