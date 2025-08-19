package com.sagecraft.chatbubble.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 工具类，用于压缩文件和目录。
 * 提供将整个目录压缩为zip文件的方法。
 */
public class ZipUtils {

    /**
     * 私有构造函数，防止工具类被实例化。
     */
    private ZipUtils() {}

    /**
     * 将目录内容压缩为zip文件。
     * 此方法递归遍历目录并将每个文件添加到zip存档中。
     *
     * @param folderPath 要压缩的文件夹路径。
     * @param zipFilePath 输出zip文件的路径。
     * @throws IOException 如果在读取目录或写入zip文件时发生I/O错误。
     */
    public static void zipDirectory(Path folderPath, Path zipFilePath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            try (Stream<Path> paths = Files.walk(folderPath)) {
                for (Path path : (Iterable<Path>) paths::iterator) {
                    if (Files.isDirectory(path)) {
                        continue;
                    }
                    String zipEntryName = folderPath.relativize(path).toString().replace("\\", "/");
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    try (InputStream is = Files.newInputStream(path)) {
                        addToZip(zipEntry, is, zos);
                    }
                }
            }
        }
    }

    /**
     * 将文件内容添加到zip存档中。
     *
     * @param zipEntry 要添加到zip存档的zip条目（文件）。
     * @param is 从中读取文件内容的输入流。
     * @param zos 文件将被写入的zip输出流。
     * @throws IOException 如果在将文件写入zip存档时发生I/O错误。
     */
    public static void addToZip(ZipEntry zipEntry, InputStream is, ZipOutputStream zos) throws IOException {
        zos.putNextEntry(zipEntry);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            zos.write(buffer, 0, bytesRead);
        }
        zos.closeEntry();
    }
}
