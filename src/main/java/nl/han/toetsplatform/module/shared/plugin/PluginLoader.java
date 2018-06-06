package nl.han.toetsplatform.module.shared.plugin;

import com.google.common.reflect.ClassPath;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginLoader {

    private final static Logger LOGGER = Logger.getLogger(PluginLoader.class.getName());

    private static String pluginFolder = "./plugins";




    /** Because some libraries (like graphviz) don't like to be loaded twice caches the loaded classes
     */
    private static HashMap<String, Class> loadedClasses = new HashMap<>();
    private static List<File> readFiles = new ArrayList<>();

    /**
     * Finds all classes that impliment the Plugin interface.
     *
     * @return a list of classes
     */
    public static List<Class> getPlugins() {
        List<Class> classes = new ArrayList<>();

        for (Map.Entry<String, Class> entry : loadedClasses.entrySet()) {
            classes.add(entry.getValue());
        }


        List<File> jarFiles = getJars(pluginFolder);
        for (File jar : jarFiles) {
            if(isFileRead(jar))continue;

            try {
                URLClassLoader cl = URLClassLoader.newInstance(new URL[]{jar.toURI().toURL()});
                ClassLoader[] loaders = new ClassLoader[]{cl};
                readFiles.add(jar);

                for (final ClassPath.ClassInfo info : ClassPath.from(cl).getTopLevelClasses()) {
                    if (info.getName().startsWith("nl")) {
                        final Class<?> clazz = info.load();
                        for (Class i : clazz.getInterfaces()) {
                            if (i == Plugin.class) {
                                loadedClasses.put(clazz.getName(), clazz);
                                classes.add(clazz);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Something wen't wrong get the plugins: " + e.getMessage());
            }
        }
        return classes;
    }

    public static boolean isFileRead(File file){
        for(File files : readFiles){
            if(files.toURI().equals(file.toURI()))
                return true;
        }

        return false;
    }


    /**
     * returns a class bases on the class name
     *
     * @param className the name of the classes
     * @return A class object
     * @throws ClassNotFoundException when it can't find the given class
     */
    public static Class getClass(String className) throws ClassNotFoundException {
        if (loadedClasses.containsKey(className)) {
            return loadedClasses.get(className);
        }


        List<File> jarFiles = getJars(pluginFolder);
        for (File jar : jarFiles) {
            if(isFileRead(jar))continue;

            try {
                URLClassLoader cl = URLClassLoader.newInstance(new URL[]{jar.toURI().toURL()});
                readFiles.add(jar);
                ClassLoader[] loaders = new ClassLoader[]{cl};

                Class questionClass = cl.loadClass(className);
                loadedClasses.put(className, questionClass);

                return questionClass;
            } catch (ClassNotFoundException e) {
                //Doesn't matter if it can't find it now

            } catch (MalformedURLException e) {
                LOGGER.log(Level.WARNING, "Something went wrong with loading plugin: " + e.getMessage());
            }
        }

        //If it still hasn't found it it doesn't exist
        throw new ClassNotFoundException();
    }

    /**
     * returns all the jar files that are in the given folder
     *
     * @param folder the folder path
     * @return all the jar files
     */
    private static List<File> getJars(String folder) {
        File[] files = new File(folder).listFiles();

        if (files == null) {
            LOGGER.log(Level.WARNING, "Could not find plugin folder");
            return new ArrayList<>();
        }

        List<File> jarFiles = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                int index = files[i].getName().lastIndexOf('.');
                if (index > 0) {
                    String extension = files[i].getName().substring(index + 1);
                    if (extension.toLowerCase().equals("jar")) {
                        jarFiles.add(files[i]);
                    }
                }
            }
        }

        return jarFiles;
    }

    /**
     * creates a plugin instance of the given vraagType
     *
     * @param vraagType the vraag type of the plugin (the full class path)
     * @param vraagData the data that the plugin needs
     * @return an instance of the plugin
     * @throws ClassNotFoundException when it can't find the given vraagtype
     */
    public static Plugin getPlugin(String vraagType, String vraagData) throws ClassNotFoundException {
        try {
            Plugin pl = (Plugin) getClass(vraagType).newInstance();
            pl.initialize(vraagData);
            return pl;
        } catch (InstantiationException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }

        throw new ClassNotFoundException();
    }
}

