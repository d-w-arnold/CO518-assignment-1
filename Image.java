import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

/**
 * Image Class.
 *
 * This class represents a simple rectangular image, where each pixel can be
 * one of 16 colours.
 */
public class Image
{

    // This is the standard 4-bit EGA colour scheme, where the numbers represent
    // 24-bit RGB colours.
    static int[] colours =
            {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
                    0xAA0000, 0xAA00AA, 0xAA5500, 0xAAAAAA,
                    0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
                    0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    // Store a 2 dimensional image with "colours" as numbers between 0 and 15
    public int pixels[][];

    /**
     * Read in an image from a file. Each line of the file must be the same
     * length, and only contain single digit hex numbers 0-9 and a-f.
     *
     * @param filename The file to be read.
     */
    public Image(String filename)
    {
        // Read the whole file into lines
        ArrayList<String> lines = new ArrayList<String>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            for (String s = in.readLine(); s != null; s = in.readLine())
                lines.add(s);
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filename);
            System.exit(1);
        } catch (IOException e) {
            System.exit(2);
        }

        if (lines.size() == 0) {
            System.out.println("Empty file: " + filename);
            System.exit(1);
        }

        // Initialise the array based on the number of lines and the length of the
        // first one.
        int length = lines.get(0).length();
        pixels = new int[lines.size()][length];

        for (int i = 0; i < lines.size(); i++) {
            // Check that all of the lines have the same length as the first one.
            if (length != lines.get(i).length()) {
                System.out.println("Inconsistent line lengths: " + length + " and " + lines.get(i).length() + " on lines 1 and " + (i + 1));
                System.exit(1);
            }

            // Copy each line into the array
            for (int j = 0; j < length; j++) {
                pixels[i][j] = Character.getNumericValue(lines.get(i).charAt(j));
                if (pixels[i][j] < 0 || pixels[i][j] > 15) {
                    System.out.println("Invalid contents: " + lines.get(i).charAt(j) + " on line " + (i + 1));
                    System.exit(1);
                }
            }
        }
    }

    /**
     * Create a solid image with given dimensions and colour.
     *
     * @param height The height.
     * @param width The width.
     * @param colour The colour of the background.
     */
    public Image(int height, int width, int colour)
    {
        pixels = new int[height][width];
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++)
                pixels[i][j] = colour;
    }

    /**
     * Main method.
     */
    public static void main(String[] args)
    {
        // A simple test to read in an image and print it out.
        Image i = new Image(args[0]);
        System.out.print(i.compress().toString());
        i.toPNG(args[0]);
    }

    /**
     * Custom toString to get back the original text-based representation.
     *
     * @return A string with the original text-based representation.
     */
    public String toString()
    {
        StringBuilder s = new StringBuilder(pixels.length * pixels[0].length);
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[i].length; j++)
                s.append(Integer.toHexString(pixels[i][j]));
            s.append("\n");
        }
        return s.toString();
    }

    /**
     * TASK 2: Implement the compress method to create and return a list of
     * drawing commands that will draw this image.
     * 6 marks for correctness -- does the command list exactly produce the
     * input image.
     * 5 marks for being able to shrink test-image1 and test-image2 into no more
     * than 7 commands each. You can work out these commands by hand, but the
     * marks here are for your implemented algorithm (HINT: think Run-length
     * Encoding) being able to create the commands.
     * 4 marks for shrinking the other, more difficult, test images. We'll run
     * this as a competition and give all 4 to the best 20% of the class, 3 to
     * the next best 20%, and so on.
     *
     * @return Test commands for a drawing.
     */
    public Drawing compress()
    {
        Compressor c = new Compressor(this);
        return c.compress();
    }

    /**
     * Render the image into a PNG with the given filename.
     *
     * @param filename The file to be rendered into a PNG.
     */
    public void toPNG(String filename)
    {

        BufferedImage im = new BufferedImage(pixels[0].length, pixels.length, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < pixels.length; i++)
            for (int j = 0; j < pixels[i].length; j++) {
                im.setRGB(j, i, colours[pixels[i][j]]);
            }

        File f = new File(filename + ".png");
        try {
            ImageIO.write(im, "PNG", f);
        } catch (IOException e) {
            System.out.println("Unable to write image");
            System.exit(1);
        }
    }

    /**
     * As part of Task 1, this method sets the colour of a specific pixel.
     *
     * @param x The x value.
     * @param y The y value.
     * @param color The colour to be painted at a specific pixel.
     */
    public void set(int x, int y, int color) throws BadCommand {
        try {
            pixels[y][x] = color;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new BadCommand(x, y);
        }
    }

    /**
     * Get colour at a specific pixel, using x and y values.
     *
     * @param x Value of x.
     * @param y Value of y.
     * @return The colour at the specified coordinate.
     */
    public int getColor(int x, int y)
    {
        return pixels[y][x];
    }

    /**
     * Get colour at a specific pixel, using a Coordinate object.
     *
     * @param c The Coordinate object to refer to.
     * @return The colour at the specified coordinate.
     */
    public int getColor(Coordinate c)
    {
        return getColor(c.x, c.y);
    }
}
