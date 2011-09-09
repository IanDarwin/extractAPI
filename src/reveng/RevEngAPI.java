package reveng;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

/** Make up a compilable version of a given Sun or other API, 
 * so developers can compile against it without a licensed copy. In Sun's case,
 * all public API info is public on Sun's web site, so this does not disclose
 * anything that is Sun Confidential.
 * <p>This is a clean-room implementation: I did not look at the code
 * for Sun's javap or any similar tool in preparing this program.
 * XXX TODO:<ul>
 * <li>Collapse common code in printing Constructors and Methods
 * <li>Method printing: add exceptions
 * <li>Arguments: Handle arrays (names begin [L)
 * <li>Use default return types consistently: in return statements
 *		and in assigment to protected final variables.
 * </ul>
 * @author Ian Darwin, http://www.darwinsys.com/
 * @version $Id$
 */
public class RevEngAPI extends APIFormatter {

	public static void main(String[] argv) throws Exception {
		new RevEngAPI().doArgs(argv);
	}

	private final static String PREFIX_ARG = "arg";
	
	/** Make up names like "arg0" "arg1", etc. */
	private String mkName(String name, int number) {
		return new StringBuffer(name).append(number).toString();
	}

	/** Generate a .java file for the outline of the given class. */
	public void doClass(Class<?> c) throws IOException {
		String className = c.getName();
		
		// Inner class in Zip or Jar file
		if (className.indexOf('$') != -1)
			return;

		// get name, as String, with . changed to /
		String slashName = className.replace('.','/');
		String fileName = slashName + ".java";

		System.out.println(className + " --> " + fileName);

		String dirName = slashName.substring(0, slashName.lastIndexOf("/"));
		new File(dirName).mkdirs();
		PrintWriter out = null;
		try {
			// create the file.
			out = new PrintWriter(new FileWriter(fileName));

			out.println("// Generated by RevEngAPI for class " + className);
			doClass(c, out, false);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	private void doClass(Class<?> c, PrintWriter out, boolean isInner) {

		// If in a package, say so.
		Package packaje;
		String packageName = null;
		if ((packaje = c.getPackage()) != null) {
			packageName = packaje.getName();
			if (!isInner) {
				out.println("package " + packageName + ';');
				out.println();
			}
		}
		// print class header
		int cMods = c.getModifiers();
		printModifiers(cMods, out);
		if (c.isAnnotation()) {
			out.print("@interface");
		} else if (c.isEnum()) {
			out.print("enum");
		} else if (c.isInterface()) {
			out.print("interface ");
		} else {
			out.print("class ");
		}
		final String className = c.getName();
		String sb = formatClassName(packageName, className);
		out.print(sb);

		final Class<?> superclass = c.getSuperclass();
		if (superclass != null) {
			out.print(" extends ");
			out.print(superclass.getName().replace('$', '.'));
		}
		boolean doneThisCategory = false;
		
		Class<?>[] interfaces = c.getInterfaces();
		for (Class<?> interfaze : interfaces) {
			if (!doneThisCategory) {
				out.print(c.isInterface() ? " extends" : " implements");
				doneThisCategory = true;
			} else {
				out.print(",");
			}
			out.print(' ');
			out.print(interfaze.getName());
		}
		out.println(" {");		
		
		// print inner classes
		Class<?>[] inners = c.getDeclaredClasses();
		for (Class<?> inner : inners) {
			if (!doneThisCategory) {
				out.println();
				out.println("\t// Inner classes");
				doneThisCategory = true;
			}
			doClass(inner, out, true);	// recurse
		}
		
		// print fields
		doneThisCategory = false;
		Field[] flds = c.getDeclaredFields();
		sortMembersArray(flds);
		for (Field f : flds) {
			if (!doneThisCategory) {
				out.println();
				out.println("\t// Fields");
				doneThisCategory = true;
			}
			f.setAccessible(true);  // bye-bye "private"
			int mods = f.getModifiers();
			out.print('\t');
			printModifiers(mods, out);
			final Class<?> type = f.getType();
			printType(out, type);
			out.print(' ');
			out.print(f.getName());
			if (Modifier.isFinal(mods)) {
				out.print(" = ");
				boolean isString = type == String.class;
				if (isString)
					out.print('"');
				Exception exc = null;
				try {
					out.print(f.get(null));
				} catch (Exception ex) {
					out.print(defaultValue(type));
					exc = ex;
				}
				if (isString)
					out.print('"');
				out.println(";");
				if (exc != null) {
					out.print("\t// XXX RevEng got " + exc + " fetching value for above");
				}
			}
			out.println(';');
		}

		// print constructors
		doneThisCategory = false;
		Constructor<?>[] constructors = c.getDeclaredConstructors();
		sortMembersArray(constructors);
		for (Constructor<?> constructor : constructors) {
			if (!doneThisCategory) {
				out.println();
				out.println("\t// Constructors");
				doneThisCategory = true;
			}
			constructor.setAccessible(true);
			try {
				int mods = constructor.getModifiers();
				out.print('\t');
				printModifiers(mods, out);
			} catch (ClassFormatError e) {
				System.err.println(e);
			}
			out.print(formatClassName(packageName, constructor.getName()));
			printArguments(out, constructor.getParameterTypes());
			// XXX print thrown Exceptions
			out.println(" {");
			out.println("\t}");
		}

		// print methods
		doneThisCategory = false;
		Method[] methods = c.getDeclaredMethods();
		sortMembersArray(methods);
		for (Method method : methods) {
			if (!doneThisCategory) {
				out.println();
				out.println("\t// Methods");
				doneThisCategory = true;
			}
			if (method.getName().startsWith("access$"))
				continue;
			method.setAccessible(true);  // bye-bye "private"	
			int mods = method.getModifiers();
			out.print('\t');
			printModifiers(mods, out);
			final Class<?> returnType = method.getReturnType();
			printType(out, returnType);
			out.print(' ');
			out.print(method.getName());
			printArguments(out, method.getParameterTypes());
			//	XXX print thrown Exceptions
			// Now the body, or ';' if abstract
			if (Modifier.isAbstract(mods) || Modifier.isNative(mods)) {
				out.println(';');
			} else {
				out.println(" {");
				if (!returnType.equals(void.class)) {
					out.println("\treturn " + defaultValue(method.getReturnType()) + ';');
				}
				out.println("\t}");
			}
		}


		// End of this class
		out.println("}");
		out.flush();
	}

	/**
	 * @param className
	 * @return
	 */
	static String formatClassName(final String pkg, final String className) {
		boolean debug = false;
		StringBuilder sb = new StringBuilder(className);
		int off;
		if ((off = sb.indexOf("$")) != -1) {
			sb.setCharAt(off, '.');
		}
		if (className.startsWith(pkg)) {
			sb.delete(0, pkg.length() + 1);
		}
		if (debug) System.out.println(sb);
		int lastDot = sb.lastIndexOf(".");
		if (lastDot != -1) {
			sb.delete(0, lastDot + 1);
		}
		if (debug) System.out.println(sb);
		return sb.toString();
	}

	/**
	 * @param out
	 * @param classes
	 */
	private void printArguments(PrintWriter out, Class[] classes) {
		out.print('(');
		for (int j = 0; j<classes.length; j++) {
			if (j > 0) out.print(", ");
			printType(out, classes[j]);
			out.print(' ');
			out.print(mkName(PREFIX_ARG, j));
		}
		out.print(')');
	}

	/**
	 * @param out
	 * @param returnType
	 */
	private void printType(PrintWriter out, final Class<?> returnType) {
		if (returnType == null) {
			out.println("void /*XXX*/");
		} else if (returnType.isArray()) {
			out.print(returnType.getCanonicalName());
		} else {
			out.print(returnType.getName().replace('$','.'));
		}
	}

	private class ModInfo {
		int val;
		String name;
		ModInfo(int v, String n) {
			val = v;
			name = n;
		}
	}

	private ModInfo[] modInfo = {
		new ModInfo(16, "final"),
		new ModInfo(2, "private"),
		new ModInfo(1, "public"),
		new ModInfo(4, "protected"),
		new ModInfo(1024, "abstract"),
		new ModInfo(8, "static"),
		new ModInfo(32, "synchronized"),
		new ModInfo(256, "native"),
		new ModInfo(128, "transient"),
		new ModInfo(64, "volatile"),
		new ModInfo(2048, "strict"),
	};

	private void printModifiers(int mods, PrintWriter out) {
		for (int i=0; i < modInfo.length; i++) {
			if ((mods & modInfo[i].val) == modInfo[i].val) {
				out.print(modInfo[i].name);
				out.print(' ');
			}
		}
	}

	static String defaultValue(Class c) {
		if (c.getName().equals("boolean"))
			return "false";
		if (c.isPrimitive()) {
			return "0";
		}
		return "null";
	}

	public void startFile() {
		// maybe save filename as project name?
	}

	public void endFile() {
		// maybe generate a trivial "build.xml" for Ant to create the jar file?
	}
	
	/** Sort the entries by name */
	static void sortMembersArray(Member[] data) {
		Arrays.sort(data, new Comparator<Member>() {
			public int compare(Member n1, Member n2) {
				return n1.getName().compareToIgnoreCase(n2.getName());
			}
		});
	}
}
