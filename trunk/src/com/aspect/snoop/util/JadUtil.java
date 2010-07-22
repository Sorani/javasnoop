/*
 * Copyright, Aspect Security, Inc.
 *
 * This file is part of JavaSnoop.
 *
 * JavaSnoop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JavaSnoop is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaSnoop.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aspect.snoop.util;

import com.aspect.snoop.JavaSnoop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author adabirsiaghi
 */
public class JadUtil {

    private static Map<String,String> codeCache;

    private static File getExecutableFromPath(String executableName) {
        String systemPath = System.getenv("PATH");
        String[] pathDirs = systemPath.split(File.pathSeparator);

        File fullyQualifiedExecutable = null;
        for (String pathDir : pathDirs) {
            File file = new File(pathDir, executableName);
            if (file.isFile()) {
                fullyQualifiedExecutable = file;
                break;
            }
        }
        return fullyQualifiedExecutable;
    }

    public static String getJadLocation() {

        try {

            boolean isWin = System.getProperty("os.name").contains("Windows");

            File f = getExecutableFromPath("jad" + (isWin ? ".exe":"") );

            if ( f != null ) {
                return f.getAbsolutePath();
            }

            /*
             * Check the Jad path.
             */
            String jadPath = JavaSnoop.getProperty(JavaSnoop.JAD_PATH);
            if ( jadPath == null || jadPath.length() == 0 ) {
                return null;
            }

            if ( new File(jadPath).exists() ) {
                return jadPath;
            }

        } catch (Exception e) { }

        return null;
    }

    public static String getDecompiledJava(String className, byte[] clazz) throws IOException {

        String jadPath = getJadLocation();

        if ( jadPath == null ) {
            throw new IOException("Couldn't find jad");
        }

        if ( codeCache == null ) {
            codeCache = new HashMap<String,String>();
        } else {
            String code = codeCache.get(className);
            if ( code != null ) {
                return code;
            }
        }

        String prefix = RandomUtil.randomString(8);
        File classFile = File.createTempFile(prefix, ".class");
        
        FileOutputStream fos = new FileOutputStream(classFile);
        fos.write(clazz);
        fos.close();

        String tmpDir = System.getProperty("java.io.tmpdir");

        String cmd[] = {
            jadPath,
            "-o",
            "-d",
            tmpDir,
            "-s",
            ".java",
            classFile.getAbsolutePath()
        };

        Process p = Runtime.getRuntime().exec(cmd);

        try {
            p.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(JadUtil.class.getName()).log(Level.SEVERE, "Failure with Jad", ex);
        }

        String simpleClassName = getSimpleClassName(className);
        String javaFile = tmpDir + File.separatorChar + simpleClassName + ".java";
        BufferedReader reader = new BufferedReader(new FileReader(javaFile));

        StringBuffer sb = new StringBuffer(5000);
        String buff = null;
        String nl = System.getProperty("line.separator");

        while ( (buff = reader.readLine()) != null ) {
            if ( !buff.startsWith("//") ) {
                sb.append(buff + nl);
            }
        }
        
        String finalCode =
                "/* Decompiled " + className + " */" + nl +
                sb.toString().trim();

        codeCache.put(className, finalCode);

        return finalCode;
    }

    private static String getSimpleClassName(String className) {
        String fqClassName = new String(className);
        int firstChar;
        firstChar = fqClassName.lastIndexOf('.') + 1;
        if (firstChar > 0) {
            className = fqClassName.substring(firstChar);
        }
        return className;
    }

}
