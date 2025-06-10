package rnb;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

// File format:
// Entry position:
// - content block index u16
// - offsetInContentBlock u16
// Name of last book opened (UTF-16LE)
public class Storage {

    private static final String FILE_NAME = "a";

    public static void save(Context context, String name, int contentBlockIndex, int integerOffsetInContentBlock) {
        byte[] encodedName = name.getBytes(StandardCharsets.UTF_16LE);
        byte[] newData = new byte[4 + encodedName.length];

        newData[0] = (byte) contentBlockIndex;
        newData[1] = (byte) (contentBlockIndex >> 8);

        newData[2] = (byte) integerOffsetInContentBlock;
        newData[3] = (byte) (integerOffsetInContentBlock >> 8);

        System.arraycopy(encodedName, 0, newData, 4, encodedName.length);

        try (OutputStream os = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            os.write(newData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // This is for the initial load
    public static LoadResult load(Context context) {
        try (InputStream is = context.openFileInput(FILE_NAME)) {
            byte[] bytes = is.readAllBytes();

            int contentBlockIndex = ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF);
            float offsetInContentBlock = (((bytes[3] & 0xFF) << 8) | (bytes[2] & 0xFF)) / 65535f;
            String name = new String(bytes, 4, bytes.length - 4, StandardCharsets.UTF_16LE);

            LoadResult result = new LoadResult();
            result.fileName = name;
            result.contentBlockIndex = contentBlockIndex;
            result.offsetInContentBlock = offsetInContentBlock;

            return result;
        } catch (FileNotFoundException e) {
            // Nothing saved yet
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class LoadResult {
        public String fileName;
        public int contentBlockIndex;
        public float offsetInContentBlock;
    }
}
