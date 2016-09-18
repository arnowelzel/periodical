package de.arnowelzel.android.periodical;

import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Helper for file operations
 */
public class FileUtils {
    /**
     * Creates the specified destination file as a byte for byte copy of the
     * source file. If the destination file already exists, then it will be
     * replaced with a copy of the source file.
     * <br>
     * <br><i>Note: The file streams will be closed by this function.</i>
     *
     * @param fromFile
     * The input stream for the file to copy from.
     *
     * @param toFile
     * The output stream for the file to copy to.
     */
    public static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {
        FileChannel fromChannel = null;
        FileChannel toChannel = null;
        try {
            fromChannel = fromFile.getChannel();
            toChannel = toFile.getChannel();
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        } finally {
            try {
                if (fromChannel != null) {
                    fromChannel.close();
                }
            } finally {
                if (toChannel != null) {
                    toChannel.close();
                }
            }
        }
    }
}