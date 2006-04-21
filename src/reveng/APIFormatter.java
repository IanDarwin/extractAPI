package reveng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * <p>
 * APIFormatter is an abstract class that reads one or more Zip files, gets all entries from each
 * and, for each entry that ends in ".class", loads it with Class.forName()
 * and hands it off to a Template Method doClass(Class c) declared in a subclass.
 * <br/>TODO<br/>
 * Use GETOPT to control doingStandardClasses, verbosity level, etc.
 * @author	Ian Darwin, Ian@DarwinSys.com
 * @version	$Id$
 */
public abstract class APIFormatter {

	/** True if we are doing classpath, so only do java. and javax. */
	protected static boolean doingStandardClasses = true;
	/** True to skip names that begin "sun." or "com.sun." */
	private boolean skipSunInternal = true;	// change to false if you make it an option.
	
	protected int doArgs(String[] argv) throws IOException {
		/** Counter of fields/methods printed. */
		int n = 0;

		// TODO: option
		// -s - skipSunInternal = true;
		
		if (argv.length == 1 && "-b".equals(argv[0])) {
			String s = System.getProperty("sun.boot.class.path");
			doClassPath(s);
		} else if (argv.length == 0) {
			// No arguments, look in CLASSPATH
			String classPath = System.getProperty("java.class.path");
			doClassPath(classPath);
		} else {
			// We have arguments, process them as zip/jar files
			// doingStandardClasses = false;
			for (int i=0; i<argv.length; i++)
				processOneFile(argv[i]);
		}

		return n;
	}

	/**
	 * Break a "ClassPath"-like String into individual components
	 * @param classPath
	 * @throws IOException
	 */
	private void doClassPath(String classPath) throws IOException {
		//  break apart with path sep.
		String pathSep = System.getProperty("path.separator");
		StringTokenizer st = new StringTokenizer(classPath, pathSep);
		// Process each zip in classpath
		while (st.hasMoreTokens()) {
			String thisFile = st.nextToken();
			System.err.println("Trying path " + thisFile);
			processOneFile(thisFile);
		}
	}

	/** For each Zip file, for each entry, xref it */
	public void processOneFile(String fileName) throws IOException {
		if (fileName.endsWith(".class")) {
			doClass(fileName);
		}
		if (!(fileName.endsWith(".jar") || fileName.endsWith(".zip"))) {
			System.err.printf("pocessOneFile: Do not understand file %s%n", fileName);
		}
			List<ZipEntry> entries = new ArrayList<ZipEntry>();
			ZipFile zipFile = null;

			try {
				zipFile = new ZipFile(new File(fileName));
			} catch (ZipException zz) {
				throw new FileNotFoundException(zz.toString() + fileName);
			}
			Enumeration all = zipFile.entries();

			// Put the entries into the List for sorting...
			while (all.hasMoreElements()) {
				ZipEntry zipEntry = (ZipEntry)all.nextElement();
				entries.add(zipEntry);
			}

			// Sort the entries (by class name)
			// Collections.sort(entries);

			// Process all the entries in this zip.
			Iterator it = entries.iterator();
			while (it.hasNext()) {
				ZipEntry zipEntry = (ZipEntry)it.next();
				String zipName = zipEntry.getName();

				// Ignore package/directory, other odd-ball stuff.
				if (zipEntry.isDirectory()) {
					continue;
				}

				// Ignore META-INF stuff
				if (zipName.startsWith("META-INF/")) {
					continue;
				}

				// Ignore images, HTML, whatever else we find.
				if (!zipName.endsWith(".class")) {
					continue;
				}

				// If doing CLASSPATH, Ignore com.* which are "internal API".
				// 	if (doingStandardClasses && !zipName.startsWith("java")){
				// 		continue;
				// 	}
			
				// Convert the zip file entry name, like
				//	java/lang/Math.class
				// to a class name like
				//	java.lang.Math
				String className = zipName.replace('/', '.').
					substring(0, zipName.length() - 6);	// 6 for ".class"

				// Now process the class.
				doClass(className);
				
			}
	}
	
	/**
	 * This has the same name and argument as the Template Method but is only used here.
	 * @param className
	 * @throws IOException
	 */
	private void doClass(String className) throws IOException {
		if (skipSunInternal && (className.startsWith("sun.") || className.startsWith("com.sun."))) {
			return;
		}
		try {
			Class c = Class.forName(className);
			// Hand it off to the subclass...
			doClass(c);
		} catch (ClassNotFoundException e) {
			System.err.println("Error: " + e);
		}
	}

	/** Template method to format the fields and methods of one class, given its name.
	 */
	protected abstract void doClass(Class c) throws IOException;
}
