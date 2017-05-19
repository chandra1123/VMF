package eu.mihosoft.vmf;

import eu.mihosoft.vmf.core.CodeGenerator;
import eu.mihosoft.vmf.core.FileResourceSet;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by miho on 03.01.2017.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class VMF {

    /**
     * Generates code for the specified model definition.
     *
     * @param outputDir output directory for the generated code, e.g., '<em>main/java/src-gen</em>'
     * @param interfaces model interfaces (all interfaces must be in the same package,
     *                   package must end with '.vmfmodel')
     * @throws IOException if the code generation fails due to I/O related problems
     * @throws IllegalArgumentException if the specified model is empty
     */
    public static void generate(File outputDir, Class<?>... interfaces) throws IOException {
        new CodeGenerator().generate(new FileResourceSet(outputDir),interfaces);
    }

    /**
     * Generates code for the specified model definition. The context classloader of the current thread is used to
     * locate the model interfaces.
     *
     * @param outputDir output directory for the generated code, e.g., '<em>main/java/src-gen</em>'
     * @param packageName package that contains the model interfaces
     *                    (all interfaces must be in the same package, package must end with '.vmfmodel')
     * @throws IOException if the code generation fails due to I/O related problems
     * @throws IllegalArgumentException if the specified model is empty
     */
    public static void generate(File outputDir, String packageName) throws IOException {
        Collection<Class<?>> interfaces = listClassesInPackage(
                Thread.currentThread().getContextClassLoader(), packageName);
        generate(outputDir,interfaces.toArray(new Class[interfaces.size()]));
    }

    /**
     * Generates code for the specified model definition.
     *
     * @param outputDir output directory for the generated code, e.g., '<em>main/java/src-gen</em>'
     * @param classLoader the classloader that shall be used to locate the model interfaces in the specified package
     * @param packageName package that contains the model interfaces
     *                    (all interfaces must be in the same package, package must end with '.vmfmodel')
     * @throws IOException if the code generation fails due to I/O related problems
     * @throws IllegalArgumentException if the specified model is empty
     */
    public static void generate(File outputDir, ClassLoader classLoader, String packageName) throws IOException {
        Collection<Class<?>> interfaces = listClassesInPackage(classLoader, packageName);
        generate(outputDir,interfaces.toArray(new Class[interfaces.size()]));
    }

    /**
     * Lists all classes in the specified package.
     * @param classLoader classloader used for searching the package content
     * @param packageName package name
     * @return all classes in the specified package
     *
     * @throws IllegalArgumentException if the specified model is empty
     */
    private static Collection<Class<?>> listClassesInPackage(ClassLoader classLoader, String packageName) {
        List<String> clsNames = new FastClasspathScanner(packageName).overrideClassLoaders(classLoader)
                .scan()
                .getNamesOfAllClasses();

        return clsNames.stream().map(clsName->loadClass(classLoader, clsName)).
                filter(cls->cls!=null).filter(cls->{
                    // annotations from other packages are allowed
                    // we remove annotations from other packages from this list
                    // other classes from other packages are not removed since we generate
                    // an error message in this case
                    return Objects.equals(cls.getPackage().getName(),packageName)||!cls.isAnnotation();
                }).
                collect(Collectors.toList());
    }

    /**
     * Loads a class from a specified classloader.
     * @param classLoader classloader that shall be used for classloading
     * @param clsName the name of the class to load
     * @return the reuested class or {@code null} if the requested class cannot be found
     *
     * @throws IllegalArgumentException if the specified model is empty
     */
    private static Class<?> loadClass(ClassLoader classLoader,String clsName) {
        try {
            return classLoader.loadClass(clsName);
        } catch (ClassNotFoundException e) {
            //
        }

        return null;
    }

//
//    /**
//     * Lists all classes in the specified package.
//     * @param classLoader classloader used for searching the package content
//     * @param packageName package name
//     * @return all classes in the specified package
//     */
//    private static Collection<Class<?>> listClassesInPackage(ClassLoader classLoader, String packageName) {
//        List<Class<?>> classes = new ArrayList<>();
//        // Get a File object for the package
//        File directory;
//        String fullPath;
//        String relPath = packageName.replace('.', '/');
//        System.out.println("ClassDiscovery: Package: " + packageName + " becomes Path:" + relPath);
//        URL resource = classLoader.getResource(relPath);
//        System.out.println("ClassDiscovery: Resource = " + resource);
//        if (resource == null) {
//            throw new RuntimeException("No resource for " + relPath);
//        }
//        fullPath = resource.getFile();
//        System.out.println("ClassDiscovery: FullPath = " + resource);
//
//        try {
//            directory = new File(resource.toURI());
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(packageName + " (" + resource + ") does not appear to be a valid URL / URI.  Strange, since we got it from the system...", e);
//        } catch (IllegalArgumentException e) {
//            directory = null;
//        }
//        System.out.println("ClassDiscovery: Directory = " + directory);
//
//        if (directory != null && directory.exists()) {
//            // Get the list of the files contained in the package
//            String[] files = directory.list();
//            for (int i = 0; i < files.length; i++) {
//                // we are only interested in .class files
//                if (files[i].endsWith(".class")) {
//                    // removes the .class extension
//                    String className = packageName + '.' + files[i].substring(0, files[i].length() - 6);
//                    System.out.println("ClassDiscovery: className = " + className);
//                    try {
//                        classes.add(classLoader.loadClass(className));
//                    }
//                    catch (ClassNotFoundException e) {
//                        throw new RuntimeException("ClassNotFoundException loading " + className);
//                    }
//                }
//            }
//        }
//        else {
//            try {
//                String jarPath = fullPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
//                JarFile jarFile = new JarFile(jarPath);
//                Enumeration<JarEntry> entries = jarFile.entries();
//                while(entries.hasMoreElements()) {
//                    JarEntry entry = entries.nextElement();
//                    String entryName = entry.getName();
//                    if(entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length())) {
//                        System.out.println("ClassDiscovery: JarEntry: " + entryName);
//                        String className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
//                        System.out.println("ClassDiscovery: className = " + className);
//                        try {
//                            classes.add(classLoader.loadClass(className));
//                        }
//                        catch (ClassNotFoundException e) {
//                            throw new RuntimeException("ClassNotFoundException loading " + className);
//                        }
//                    }
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(packageName + " (" + directory + ") does not appear to be a valid package", e);
//            }
//        }
//        return classes;
//    }

}
