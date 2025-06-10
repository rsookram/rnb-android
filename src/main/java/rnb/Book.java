package rnb;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Book {

    private final FileInputStream is;

    public final String name;
    public final ContentBlock[] contentBlocks;
    private final byte[] imageMeta;
    private final int baseImageOffset;

    public Book(File file) throws IOException {
        this.name = file.getName();

        int baseImageOffset = 0;
        is = new FileInputStream(file);

        BufferedInputStream bis = new BufferedInputStream(is, 512 * 1024);
        byte[] buf = new byte[16 * 1024];

        bis.readNBytes(buf, 0, 3);
        baseImageOffset += 3;
        int numContentBlocks = ((buf[1] & 0xFF) << 8) | (buf[0] & 0xFF);
        int numImages = buf[2] & 0xFF;

        imageMeta = new byte[numImages * 8];
        bis.readNBytes(imageMeta, 0, imageMeta.length);
        baseImageOffset += imageMeta.length;

        ContentBlock[] contentBlocks = new ContentBlock[numContentBlocks];

        char[] emptyCharArray = {};
        Furigana[] emptyFuriganaArray = {};
        for (int contentBlockIndex = 0; contentBlockIndex < numContentBlocks; contentBlockIndex++) {
            bis.readNBytes(buf, 0, 2);
            baseImageOffset += 2;

            ContentBlock contentBlock = new ContentBlock();

            int lengthPrefix = ((buf[1] & 0xFF) << 8) | (buf[0] & 0xFF);
            contentBlock.flagsOrImageIndex = lengthPrefix;

            if (isImage(lengthPrefix)) {
                contentBlock.readings = emptyFuriganaArray;
                contentBlocks[contentBlockIndex] = contentBlock;
                continue;
            }

            lengthPrefix &= 0b0001_1111_1111_1111;

            if (lengthPrefix == 0) {
                contentBlock.data = emptyCharArray;
                contentBlock.readings = emptyFuriganaArray;
                contentBlocks[contentBlockIndex] = contentBlock;
                continue;
            }

            bis.readNBytes(buf, 0, lengthPrefix);
            baseImageOffset += lengthPrefix;
            char[] text = decodeString(buf, lengthPrefix);

            int numFuriganaSpans = bis.read() & 0xFF;
            baseImageOffset += 1;

            Furigana[] spans = new Furigana[numFuriganaSpans];
            for (int i = 0; i < numFuriganaSpans; i++) {
                bis.readNBytes(buf, 0, 4);
                baseImageOffset += 4;
                int location = readFuriganaLocation(buf);
                int readingLength = buf[3] & 0xFF;

                bis.readNBytes(buf, 0, readingLength);
                baseImageOffset += readingLength;
                char[] reading = decodeString(buf, readingLength);

                Furigana furigana = new Furigana();
                furigana.location = location;
                furigana.reading = reading;

                spans[i] = furigana;
            }

            contentBlock.data = text;
            contentBlock.readings = spans;
            contentBlocks[contentBlockIndex] = contentBlock;
        }

        this.contentBlocks = contentBlocks;
        this.baseImageOffset = baseImageOffset;
    }

    public byte[] getImageBytes(int index) throws IOException {
        int rowOffset = index * 8;

        int offset = (imageMeta[rowOffset + 3] & 0xFF << 24)
                | ((imageMeta[rowOffset + 2] & 0xFF) << 16)
                | ((imageMeta[rowOffset + 1] & 0xFF) << 8)
                | (imageMeta[rowOffset] & 0xFF);
        offset += baseImageOffset;

        int length = (imageMeta[rowOffset + 7] & 0xFF << 24)
                | ((imageMeta[rowOffset + 6] & 0xFF) << 16)
                | ((imageMeta[rowOffset + 5] & 0xFF) << 8)
                | (imageMeta[rowOffset + 4] & 0xFF);

        is.getChannel().position(offset);

        byte[] imageBytes = new byte[length];
        is.readNBytes(imageBytes, 0, imageBytes.length);

        return imageBytes;
    }

    private boolean isImage(int lengthPrefix) {
        // lengthPrefix is a u16, so this checks if the highest bit in the u16 is set
        return lengthPrefix >= (1 << 15);
    }

    private char[] decodeString(byte[] buffer, int numBytes) {
        char[] chars = new char[numBytes / Character.BYTES];

        for (int i = 0; i < chars.length; i++) {
            int j = i * 2;
            char ch = (char) ((buffer[j + 1]) << 8 | (buffer[j] & 0xFF));
            chars[i] = ch;
        }

        return chars;
    }

    private static int readFuriganaLocation(byte[] bytes) {
        return ((bytes[2] & 0xFF) << 16) | ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF);
    }

    public void close() throws IOException {
        is.close();
    }

    public static class Furigana {
        /**
         * The lowest two bytes are the start offset into the text block.
         * The third lowest byte is the number of characters in the text block that the reading
         * applies to.
         */
        public int location;
        public char[] reading;
    }
}
