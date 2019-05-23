package ru.ifmo.rain.tynyanov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * The class implements {@link JarImpler} interface.
 */
public class Implementor implements JarImpler {

    /**
     * String equivalent to 4 spaces
     */
    String TAB = "    ";

    /**
     * Creates new instance of {@link Implementor}
     */
    public Implementor() {
    }

    /**
     * Generates class implementing <code>token</code> as interface (if it is).
     * Name of generated class should be the same as <code>token</code>, but with <code>Impl</code> suffix
     *
     * @throws ImplerException if the given class cannot be generated because some arguments are null or
     *                         {@link IOException} happened while implementation
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkArguments(token, root);
        try (BufferedWriter writer = Files.newBufferedWriter(getImplementationFile(token, root, "Impl.java"))) {
            writer.write(String.format("%s %n %s {%n %s %n}%n",
                    getPackage(token),
                    getClassHeader(token),
                    getMethods(token)));
        } catch (IOException e) {
            throw new ImplerException("Error while writing code to file");
        }
    }

    /**
     * Produces <code>.jar</code> file implementing interface provided by <code>token</code>.
     * Name of generated class should be the same as <code>token</code>, but with <code>Impl</code> suffix
     * During implementation creates temporary folder to store temporary <code>.java</code> and <code>.class</code> files.
     *
     * @param token   {@link Class} interface to implement
     * @param jarFile {@link Path} to store <code>.jar</code> file
     * @throws ImplerException if the given class cannot be generated for one of such reasons:
     *                         <ul>
     *                         <li> Some arguments are <code>null</code></li>
     *                         <li> Error occurs during implementation via {@link #implement(Class, Path)} </li>
     *                         <li> The process is not allowed to create files or directories. </li>
     *                         <li> {@link JavaCompiler} failed to compile implemented class </li>
     *                         <li> The problems with I/O occurred during implementation. </li>
     *                         </ul>
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        checkArguments(token, jarFile);
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "temp");
        } catch (IOException e) {
            throw new ImplerException("Unable to create temp directory");
        }
        try {
            implement(token, tempDir);
            String implClassPath = getImplementationFile(token, tempDir, "Impl.java").toString();
            System.out.println(implClassPath);
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            String tokenPath = token.getProtectionDomain().getCodeSource().getLocation().getPath();
            String[] args = new String[]{
                    "-cp",
                    tempDir.toString() + File.pathSeparator + System.getProperty("java.class.path")
                            + ":" + tokenPath, "-encoding", "UTF8",
                    implClassPath};
            if (compiler == null || compiler.run(null, null, null, args) != 0) {
                throw new ImplerException("Unable to compile implemented file");
            }
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0.0");
            manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "Sergey Tynyanov");
            try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                JarEntry je = new JarEntry(token.getName().replace(".", "/") + "Impl.class");
                jarOutputStream.putNextEntry(je);
                Files.copy(getImplementationFile(token, tempDir, "Impl.class"), jarOutputStream);
            } catch (IOException e) {
                throw new ImplerException("Error while writing to .jar file");
            }
        } catch (IOException e) {
            throw new ImplerException("Error while implementing class");
        } finally {
            try {
                clean(tempDir);
            } catch (IOException e) {
                System.err.println("Unable to delete temp directory: " + e.getMessage());
            }
        }
    }

    /**
     * Clean all including files and directories in <code>path</code>
     *
     * @param path input path
     * @throws IOException if occurs error while walking
     */
    private void clean(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult postVisitDirectory(Path path, IOException exception) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Returns {@link Path} to directory from <code>curRoot</code> to <code>token</code>'s package
     *
     * @param token   {@link Class} to get package name from
     * @param curRoot {@link Path} to use as root path
     * @return {@link Path} to directory
     * @throws IOException if error happened while creating directories
     */
    public Path getImplementationDir(Class<?> token, Path curRoot) throws IOException {
        if (token.getPackage() != null) {
            curRoot = curRoot.resolve(token.getPackage().getName().replace(".", File.separator));
        }
        Files.createDirectories(curRoot);
        return curRoot;
    }

    /**
     * Return {@link Path} to file, containing implementation of given class, with given suffix. Creates directories if they don't exist
     *
     * @param token   {@link Class} to get package name and filename from
     * @param curRoot {@link Path} to use as root path
     * @param suffix  {@link String} to add at the end of filename
     * @return {@link Path} to file
     * @throws IOException if error happened while creating directories
     */
    public Path getImplementationFile(Class<?> token, Path curRoot, String suffix) throws IOException {
        return getImplementationDir(token, curRoot).resolve(token.getSimpleName() + suffix);
    }

    /**
     * Check if any of arguments is null and if <code>token</code> is interface
     *
     * @param token {@link Class} to check for not being null and being interface
     * @param path  {@link Path} to check for not being null
     * @throws ImplerException in case <code>token</code> or <code>path</code> is null or <code>token</code> is not an interface
     */
    private void checkArguments(Class<?> token, Path path) throws ImplerException {
        if (token == null || path == null) {
            throw new ImplerException("Required non null arguments");
        }
        if (!token.isInterface()) {
            throw new ImplerException("First argument requires interface");
        }
    }

    /**
     * Returns {@link String} containing all methods declarations of <code>token</code> with return statements
     *
     * @param token {@link Class} to get methods from
     * @return {@link String} with all methods
     */
    private String getMethods(Class<?> token) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Method method : token.getMethods()) {
            stringBuilder.append(String.format("%s%s {%n%s%s%s%n%s} %n",
                    TAB, getMethodHeader(method), TAB,
                    TAB, getMethodReturn(method), TAB));
        }
        return stringBuilder.toString();
    }

    /**
     * Returns {@link String} with return statement of <code>method</code>
     *
     * @param method {@link Method} to get return statement for
     * @return {@link String} with return statement
     */
    private String getMethodReturn(Method method) {
        return "return" + getDefaultReturnValue(method.getReturnType()) + ";";
    }

    /**
     * Returns {@link String} with default value of <code>returnType</code>
     *
     * @param returnType {@link Class} to get default value of
     * @return {@link String} with default value
     */
    private String getDefaultReturnValue(Class<?> returnType) {
        if (returnType.equals(boolean.class)) {
            return " true";
        } else if (returnType.equals(void.class)) {
            return "";
        } else if (returnType.isPrimitive()) {
            return " 0";
        }
        return " null";
    }

//    /**
//     * Returns {@link String} with package of <code>token</code> if exists
//     *
//     * @param token {@link Class} for which to generate package string
//     * @return {@link String} representing package of interface
//     */
//    public String getImport(Class<?> token) {
//        if (token.getPackage() != null) {
//            return String.format("import %s;", token.getPackageName() + "." + token.getSimpleName());
//        } else {
//            return "";
//        }
//    }

    /**
     * Returns {@link String} with header of <code>method</code>, containing modifiers, return type, name, arguments, possible exceptions
     *
     * @param method {@link Method} to generate header for
     * @return {@link String} representing header of <code>method</code>
     */
    private String getMethodHeader(Method method) {
        return String.format("%s %s %s (%s) %s",
                getMethodModifiers(method),
                method.getReturnType().getCanonicalName(),
                method.getName(),
                getMethodArguments(method),
                getMethodExceptions(method));
    }

    /**
     * Returns {@link String} with arguments that <code>method</code> has, split by comma
     *
     * @param method {@link Method} to generate arguments for
     * @return {@link String} representing arguments of <code>method</code> with default names
     */
    private String getMethodArguments(Method method) {
        return Arrays.stream(method.getParameters())
                .map(s -> s.getType().getCanonicalName() + " " + s.getName())
                .collect(Collectors.joining(","));
    }

    /**
     * Returns {@link String} with exceptions that <code>method</code> may throw
     *
     * @param method {@link Method} to generate exceptions for
     * @return {@link String} representing exceptions of <code>method</code>
     */
    private String getMethodExceptions(Method method) {
        if (method.getExceptionTypes().length == 0) {
            return "";
        }
        return "throws " + Arrays.stream(method.getExceptionTypes())
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Returns {@link String} containing modifiers of <code>method</code>
     *
     * @param method {@link Method} to generate modifiers for
     * @return {@link String} representing modifiers of <code>method</code>
     */
    private String getMethodModifiers(Method method) {
        return Modifier.toString(method.getModifiers() & (Modifier.methodModifiers() ^ Modifier.ABSTRACT));
    }

    /**
     * Returns {@link String} with package of <code>token</code> if exists
     *
     * @param token {@link Class} for which to generate package string
     * @return {@link String} representing package of interface
     */
    public String getPackage(Class<?> token) {
        if (token.getPackage() != null) {
            return String.format("package %s;", token.getPackageName());
        } else {
            return "";
        }
    }

    /**
     * Returns declaration {@link String} of the implemented class implementing given interface
     *
     * @param token interface to implement
     * @return {@link String} declaring implemented class
     */
    public String getClassHeader(Class<?> token) {
        return String.format("public class %s implements %s", token.getSimpleName() + "Impl", token.getCanonicalName());
    }


    /**
     * Function to choose way of {@link Implementor} to run:
     * 2 arguments: <code>className rootPath</code> - runs {@link #implement(Class, Path)} with given arguments
     * 3 arguments: <code>-jar className jarPath</code> - runs {@link #implementJar(Class, Path)} with two second arguments
     * If arguments are wrong or error happens while running, tells user about it
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 3 && args.length != 2)) {
            System.err.println("Two or three arguments expected");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.err.println("All arguments should be not null");
            }
        }
        JarImpler implementor = new Implementor();
        try {
            if (args.length == 3 && args[0].equals("-jar")) {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                System.err.println("Format expected: [-jar](optional) [className] [rootPath]");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class can not be found: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Invalid path: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Error while implementing class: " + e.getMessage());
        }
    }

}
