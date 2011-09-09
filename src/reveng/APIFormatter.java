package reveng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	/** True to skip names that begin "sun." or "com." */
	private boolean skipInternal = true;	// change to false if you make it an option.
	
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
		System.out.printf("APIFormatter.processOneFile: %s.%n", fileName);
		if (fileName.endsWith(".class")) {
			try {
				doClass(fileName);
			} catch (Exception e) {
				System.err.printf("File %s failed (%s)%n", fileName, e);
			}
			return;
		}
		File f = new File(fileName);
		if (!f.canRead()) {
			System.err.printf("processOneFile: Can not read file %s%n", fileName);
			return;
		}
		if (!f.isFile()) {
			System.err.printf("processOneFile: %s not a regular file.%n", fileName);
			return;
		}
		if (!(fileName.endsWith(".jar") || fileName.endsWith(".zip"))) {
			System.err.printf("processOneFile: Do not understand file %s%n", fileName);
			return;
		}
		List<ZipEntry> zipEntries = new ArrayList<ZipEntry>();
		ZipFile zipFile = null;
		
		try {
			zipFile = new ZipFile(f);
		} catch (ZipException ze) {
			throw new FileNotFoundException(ze.toString() + ' ' + fileName);
		}
		@SuppressWarnings("unchecked")
		Enumeration<ZipEntry> all = (Enumeration<ZipEntry>) zipFile.entries();
		
		// Put the entries into the List for sorting...
		while (all.hasMoreElements()) {
			ZipEntry zipEntry = all.nextElement();
			zipEntries.add(zipEntry);
		}
		
		// Sort the entries (by class name)
		Collections.sort(zipEntries, new Comparator<ZipEntry>() {
			public int compare(ZipEntry o1, ZipEntry o2) {
				return o1.getName().compareToIgnoreCase(o2.getName());
			}				
		});
		
		System.err.printf("We have %d entries to try in %s.%n", zipEntries.size(), fileName);
		
		// Process all the entries in this zip.
		int tries = 0, successes = 0;
		Iterator<ZipEntry> it = zipEntries.iterator();
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
			
			// Convert the zip file entry name, like
			//	java/lang/Math.class
			// to a class name like
			//	java.lang.Math
			String className = zipName.replace('/', '.').
				substring(0, zipName.length() - 6);	// 6 for ".class"
			
			if (skipInternal && (className.startsWith("sun.") || className.startsWith("com."))) {
				continue;
			}

			// Now process the class.
			try {
				++tries;
				doClass(className);
				++successes;
			} catch (ClassNotFoundException e) {
				System.err.println("Error: " + e);
			} catch (ClassFormatError e) {
				System.err.println("Error! " +e);
			} catch (NoClassDefFoundError e) {
				System.err.println("Error! " +e);
			} catch (Exception e) {
				System.err.println("Error! " +e);
			}
		}
		System.err.printf("Succeeded in %d classes out of %d attempted.%n", successes, tries);
	}
	
	/**
	 * This has the same name and argument as the Template Method but is only used here.
	 * @param className
	 * @throws IOException
	 */
	private void doClass(String className) throws Exception {
	
		Class<?> c = Class.forName(className);
		// Hand it off to the subclass...
		doClass(c);
	}

	/** Template method to format the fields and methods of one class, given its name.
	 */
	protected abstract void doClass(Class<?> c) throws IOException;
}
