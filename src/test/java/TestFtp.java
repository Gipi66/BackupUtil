import java.io.IOException;
import java.net.SocketException;

import org.junit.Test;

import ua.ci.file_replacer.Main;
import ua.ci.file_replacer.ReplacerThread;

public class TestFtp {
	@Test
	public void checkFtp() {

		ReplacerThread rt = new ReplacerThread("", "//home//pc2//dump.gz", Main.loadProps());
		try {
			rt.sendFile();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("END");
	}
}
