/**
 * 
 */
package ua.ci.file_replacer;

import java.io.IOException;
import java.util.Date;

/**
 * @author pc2
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = new Date().getTime();
		ReplacerThread repl = new ReplacerThread("/home/pc2/realt/realtyboard/media", "/home/pc2/del/media", true, true,
				true);
		// repl.run();
		try {
			repl.compress();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("total time: " + new Date(new Date().getTime() - startTime).getTime() / 1000);
	}

}
