/**
 * 
 */
package ua.ci.file_replacer;

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
		long startTime = new Date().getSeconds();
		ReplacerThread repl = new ReplacerThread("/home/pc2/realt/realtyboard/media", "/home/pc2/del/media", true,
				true);
		repl.run();
		System.out.println("total time: " + new Date(new Date().getSeconds() - startTime).getSeconds());
	}

}
