package rnb;

public class ContentBlock {

    public char[] data;
    public int flagsOrImageIndex;
    public Book.Furigana[] readings;

    public boolean isBold() {
        return (flagsOrImageIndex & 0x0100_0000) != 0;
    }

    public boolean isTextLarge() {
        // flagsOrImageIndex is a u16, and this should only be called when the block isn't an image
        // (isImage returns false). So the highest bit of the u16 isn't set, and checking for the
        // second-highest bit can be simplified.
        return flagsOrImageIndex >= (1 << 14);
    }

    public boolean isImage() {
        // flagsOrImageIndex is a u16, so this checks if the highest bit in the u16 is set
        return flagsOrImageIndex >= (1 << 15);
    }

    public int getImageIndex() {
        return flagsOrImageIndex & 0xFF;
    }
}
