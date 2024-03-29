/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.tomitribe.delorean.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @version $Revision$ $Date$
 */
public class IO {

    private IO() {
        // no-op
    }

    public static Properties readProperties(final URL resource) throws IOException {
        return readProperties(resource, new Properties());
    }

    public static Properties readProperties(final URL resource, final Properties properties) throws IOException {
        return readProperties(read(resource), properties);
    }

    public static Properties readProperties(final File resource) throws IOException {
        return readProperties(resource, new Properties());
    }

    public static Properties readProperties(final File resource, final Properties properties) throws IOException {
        return readProperties(read(resource), properties);
    }

    /**
     * Reads and closes the input stream
     *
     * @param in         InputStream
     * @param properties Properties
     * @return Properties
     * @throws java.io.IOException
     */
    public static Properties readProperties(final InputStream in, final Properties properties) throws IOException {
        if (in == null) {
            throw new NullPointerException("InputStream is null");
        }
        if (properties == null) {
            throw new NullPointerException("Properties is null");
        }
        try {
            properties.load(in);
        } finally {
            close(in);
        }
        return properties;
    }

    public static void writeProperties(final Properties properties, final File dest) throws IOException {
        if (properties == null) {
            throw new NullPointerException("Properties is null");
        }

        final OutputStream outputStream = write(dest);

        try {
            properties.store(outputStream, "");
        } finally {
            close(outputStream);
        }
    }

    public static String readString(final URL url) throws IOException {
        final InputStream in = url.openStream();
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            return reader.readLine();
        } finally {
            close(in);
        }
    }

    public static String readString(final File file) throws IOException {
        final FileReader in = new FileReader(file);
        try {
            final BufferedReader reader = new BufferedReader(in);
            return reader.readLine();
        } finally {
            close(in);
        }
    }

    public static byte[] readBytes(final File file) throws IOException {
        final InputStream in = read(file);
        try {
            return readBytes(in);
        } finally {
            close(in);
        }
    }

    public static byte[] readBytes(final URL url) throws IOException {
        final InputStream in = read(url);
        try {
            return readBytes(in);
        } finally {
            close(in);
        }
    }

    public static byte[] readBytes(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    public static String slurp(final File file) throws IOException {
        return slurp(read(file));
    }


    public static String slurp(final URL url) throws IOException {
        return slurp(url.openStream());
    }

    public static String slurp(final InputStream in) throws IOException {
        return new String(readBytes(in));
    }

    public static void writeString(final File file, final String string) throws IOException {
        final FileWriter out = new FileWriter(file);
        try {
            final BufferedWriter bufferedWriter = new BufferedWriter(out);
            try {
                bufferedWriter.write(string);
                bufferedWriter.newLine();
            } finally {
                close(bufferedWriter);
            }
        } finally {
            close(out);
        }
    }

    public static void copy(final File from, final File to) throws IOException {
        if (!from.isDirectory()) {
            final FileOutputStream fos = new FileOutputStream(to);
            try {
                copy(from, fos);
            } finally {
                close(fos);
            }
        } else {
            copyDirectory(from, to);
        }
    }

    public static void copyDirectory(final File srcDir, final File destDir) throws IOException {
        if (srcDir == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (!srcDir.exists()) {
            throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
        }
        if (!srcDir.isDirectory()) {
            throw new IOException("Source '" + srcDir + "' exists but is not a directory");
        }
        if (srcDir.getCanonicalPath().equals(destDir.getCanonicalPath())) {
            throw new IOException("Source '" + srcDir + "' and destination '" + destDir + "' are the same");
        }

        // Cater for destination being directory within the source directory (see IO-141)
        List<String> exclusionList = null;
        if (destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath())) {
            final File[] srcFiles = srcDir.listFiles();
            if (srcFiles != null && srcFiles.length > 0) {
                exclusionList = new ArrayList<String>(srcFiles.length);
                for (final File srcFile : srcFiles) {
                    final File copiedFile = new File(destDir, srcFile.getName());
                    exclusionList.add(copiedFile.getCanonicalPath());
                }
            }
        }
        doCopyDirectory(srcDir, destDir, exclusionList);
    }

    private static void doCopyDirectory(final File srcDir, final File destDir, final List<String> exclusionList) throws IOException {
        final File[] files = srcDir.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + srcDir);
        }
        if (destDir.exists()) {
            if (!destDir.isDirectory()) {
                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
            }
        } else {
            if (!destDir.mkdirs()) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
        }
        if (!destDir.canWrite()) {
            throw new IOException("Destination '" + destDir + "' cannot be written to");
        }
        for (final File file : files) {
            final File copiedFile = new File(destDir, file.getName());
            if (exclusionList == null || !exclusionList.contains(file.getCanonicalPath())) {
                if (file.isDirectory()) {
                    doCopyDirectory(file, copiedFile, exclusionList);
                } else {
                    copy(file, copiedFile);
                }
            }
        }
    }

    public static void copy(final File from, final OutputStream to) throws IOException {
        final InputStream read = read(from);
        try {
            copy(read, to);
        } finally {
            close(read);
        }
    }

    public static void copy(final URL from, final OutputStream to) throws IOException {
        final InputStream read = read(from);
        try {
            copy(read, to);
        } finally {
            close(read);
        }
    }

    public static void copy(final InputStream from, final File to) throws IOException {
        final OutputStream write = write(to);
        try {
            copy(from, write);
        } finally {
            close(write);
        }
    }

    public static void copy(final InputStream from, final File to, final boolean append) throws IOException {
        final OutputStream write = write(to, append);
        try {
            copy(from, write);
        } finally {
            close(write);
        }
    }

    public static void copy(final InputStream from, final OutputStream to) throws IOException {
        final byte[] buffer = new byte[1024];
        int length;
        while ((length = from.read(buffer)) != -1) {
            to.write(buffer, 0, length);
        }
        to.flush();
    }

    public static void copy(final byte[] from, final File to) throws IOException {
        copy(new ByteArrayInputStream(from), to);
    }

    public static void copy(final byte[] from, final OutputStream to) throws IOException {
        copy(new ByteArrayInputStream(from), to);
    }

    public static ZipOutputStream zip(final File file) throws IOException {
        final OutputStream write = write(file);
        return new ZipOutputStream(write);
    }

    public static ZipInputStream unzip(final File file) throws IOException {
        final InputStream read = read(file);
        return new ZipInputStream(read);
    }

    public static void close(final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            if (Flushable.class.isInstance(closeable)) {
                ((Flushable) closeable).flush();
            }
        } catch (final Throwable e) {
            //Ignore
        }
        try {
            closeable.close();
        } catch (final Throwable e) {
            //Ignore
        }
    }

    public static boolean delete(final File file) {
        if (file == null) {
            return false;
        }
        if (!file.delete()) {
            Logger.getLogger(IO.class.getName()).log(Level.WARNING, "Delete failed on: " + file.getAbsolutePath());
            return false;
        }

        return true;
    }

    public static OutputStream write(final File destination) throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(destination), 32768);
    }

    public static OutputStream write(final File destination, final boolean append) throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(destination, append), 32768);
    }

    public static PrintStream print(final File destination, final boolean append) throws FileNotFoundException {
        return print(write(destination, append));
    }

    public static PrintStream print(final File destination) throws FileNotFoundException {
        return print(write(destination));
    }

    public static PrintStream print(final OutputStream out) {
        return new PrintStream(out);
    }

    public static InputStream read(final File source) throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(source), 32768);
    }

    public static InputStream read(final String content) {
        return read(content.getBytes());
    }

    public static InputStream read(final String content, final String encoding) throws UnsupportedEncodingException {
        return read(content.getBytes(encoding));
    }

    public static InputStream read(final byte[] content) {
        return new ByteArrayInputStream(content);
    }

    public static InputStream read(final URL url) throws IOException {
        return url.openStream();
    }

    public static Iterable<String> readLines(final File file) throws FileNotFoundException {
        return readLines(read(file));
    }

    public static Iterable<String> readLines(final InputStream inputStream) {
        return readLines(new BufferedReader(new InputStreamReader(inputStream)));
    }

    public static Iterable<String> readLines(final BufferedReader reader) {
        return new BufferedReaderIterable(reader);
    }

    private static class BufferedReaderIterable implements Iterable<String> {
        private final BufferedReader reader;

        private BufferedReaderIterable(final BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public Iterator<String> iterator() {
            return new BufferedReaderIterator();
        }

        private class BufferedReaderIterator implements Iterator<String> {

            private String line;

            @Override
            public boolean hasNext() {
                try {
                    final boolean hasNext = (line = reader.readLine()) != null;
                    if (!hasNext) {
                        close(reader);
                    }
                    return hasNext;
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public String next() {
                return line;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove not supported");
            }
        }
    }
}
