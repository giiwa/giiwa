/*
 * Copyright 2015 Giiwa, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.framework.web;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

// TODO: Auto-generated Javadoc
/**
 * the {@code GClassLoader} Class lets loading modules/[module]/model/[*.jars]
 * 
 * @author yjiang
 *
 */
public class GClassLoader extends ClassLoader {

    private static final int BUFFER_SIZE = 8192;

    ArrayList<JarFile> jarFiles = new ArrayList<JarFile>();

    HashMap<String, Object> resources = new HashMap<String, Object>();

    private ClassLoader parent;

    /**
     * Instantiates a new g class loader.
     * 
     * @param cl
     *            the cl
     */
    public GClassLoader(ClassLoader cl) {
        parent = cl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.ClassLoader#getResource(java.lang.String)
     */
    @Override
    public URL getResource(String name) {

        log("getResource:" + name);
        for (JarFile j : jarFiles) {
            ZipEntry z = j.getEntry(name);
            if (z != null) {
                try {
                    return new URL("file://" + j.getName() + "!/" + name);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

        return parent.getResource(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
     */
    @Override
    public InputStream getResourceAsStream(String name) {

        log("getResourceAsStream:" + name);

        for (JarFile j : jarFiles) {
            ZipEntry z = j.getEntry(name);
            if (z != null) {
                try {
                    return j.getInputStream(z);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return parent.getResourceAsStream(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.ClassLoader#getResources(java.lang.String)
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {

        // log("getResources: " + name);

        log("getResources:" + name);

        Vector<URL> list = new Vector<URL>();
        for (JarFile f : jarFiles) {
            ZipEntry z = f.getEntry(name);
            if (z != null) {
                URL u = new URL("jar:" + new File(f.getName()).toURI().toURL() + "!/" + name);
                list.add(u);
            }
        }
        if (list.isEmpty()) {
            return parent.getResources(name);
        } else {
            return list.elements();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.ClassLoader#loadClass(java.lang.String)
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    /**
     * Adds the jar.
     * 
     * @param name
     *            the name
     */
    public void addJar(String name) {
        try {
            if (name.endsWith(".jar")) {
                // log(name);
                JarFile f = new JarFile(name);
                jarFiles.add(f);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InputStream getInputStream(String name) throws IOException {
        log("getInputStream:" + name);

        for (JarFile f : jarFiles) {
            ZipEntry z = f.getEntry(name);
            if (z != null) {
                return f.getInputStream(z);
            }
        }

        log("getInputStream:" + name + ": NULL");
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
     */
    protected synchronized Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {

        log("loadClass:" + className);

        // 1. is this class already loaded?
        Class<?> cls = findLoadedClass(className);
        if (cls != null) {
            return cls;
        }

        // 2. get class file name from class name
        String clsFile = className.replace('.', '/') + ".class";

        // 3. get bytes for class
        byte[] classBytes = null;
        try {
            InputStream in = getInputStream(clsFile);
            if (in != null) {
                byte[] buffer = new byte[BUFFER_SIZE];
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int n = -1;
                while ((n = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    out.write(buffer, 0, n);
                }
                classBytes = out.toByteArray();

                cls = defineClass(className, classBytes, 0, classBytes.length);
                if (resolve) {
                    resolveClass(cls);
                }

                return cls;
            }
        } catch (IOException e) {
            // log("ERROR loading class file: " + e);
        }

        return parent.loadClass(className);
    }

    /**
     * Log.
     * 
     * @param s
     *            the s
     */
    static void log(String s) {
        // System.out.println(s);
    }

}
