import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Direction Enum Class.
 */
enum Direction
{
    UP {
        public String toString()
        {
            return "up";
        }
    },
    DOWN {
        public String toString()
        {
            return "down";
        }
    },
    LEFT {
        public String toString()
        {
            return "left";
        }
    },
    RIGHT {
        public String toString()
        {
            return "right";
        }
    }
}

/**
 * DrawingCommand Class.
 *
 * A single drawing command. Which direction to go in, how far to move, and
 * whether to paint all of the spaces in-between, or leave them as-is. Also
 * indicate which colour if painting.
 */
class DrawingCommand
{
    public Direction dir;
    public int distance;
    public boolean paint;
    public int colour;

    // Reads in a Drawing commands from a string
    // The format should be "direction distance colour" or "direction distance"
    // if moving without painting, for example
    // left 10 3
    // up 1
    // up 2 c

    /**
     * Reads in Drawing commands from a string.
     * The format should be "direction distance colour" or "direction distance"
     * if moving without painting for example.
     *
     * @param s The string of Drawing commands to be read.
     */
    public DrawingCommand(String s)
    {
        // Split the string by whitespace
        String[] elems = s.split("\\s");

        if (elems.length != 3 && elems.length != 2) {
            System.out.println("Bad command (should have 2 or 3 parts): " + s);
            System.exit(1);
        }

        if (elems[0].equals("up"))
            dir = Direction.UP;
        else if (elems[0].equals("down"))
            dir = Direction.DOWN;
        else if (elems[0].equals("left"))
            dir = Direction.LEFT;
        else if (elems[0].equals("right"))
            dir = Direction.RIGHT;
        else {
            System.out.println("Bad direction (should be up, down, left, or right): " + elems[0]);
            System.exit(1);
        }

        try {
            distance = Integer.parseInt(elems[1]);
        } catch (NumberFormatException e) {
            System.out.println("Bad distance (should be a number): " + elems[1]);
            System.exit(1);
        }
        // Check for the optional colour
        if (elems.length == 2)
            paint = false;
        else {
            paint = true;
            try {
                colour = Integer.parseInt(elems[2], 16);
                if (colour < 0 || colour > 15)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.out.println("Bad colour (should be a hex number betweeen 0 and f): " + elems[2]);
                System.exit(1);
            }
        }
    }

    /**
     * Custom toString method.
     *
     * @return A string for a single Drawing command.
     */
    public String toString()
    {
        return (dir.toString() + " " + distance + " " + (paint ? Integer.toHexString(colour) : ""));
    }
}

/**
 * BadCommand Class.
 */
class BadCommand extends Exception
{
    /**
     * Returns an error message.
     *
     * @param x The value of x.
     * @param y The value of y.
     */
    public BadCommand(int x, int y)
    {
        super("Cannot be drawn at coordinate: (" + x + "," + y + ")");
    }
}

// Represent a picture as the height and width, and a sequence of drawing
// commands to build it. Also has an initial background colour. Each command
// starts at the current location and draws a certain number of spaces to getColor
// to the next location. If a colour is given, it paints over the spaces
// in-between, including the final destination space, but not the initial
// space. Otherwise, it just updates the position. The starting position is at
// (0,0), the top, left corner of the drawing.
//
// For example, starting with a 4x2 picture of all 0's
//
// 00
// 00
// 00
// 00
//
// the command 'down 2 1' changes the image as follows and leaves the current
// position at (2,0) -- down 2 and over none
//
// 00
// 10
// 10
// 00
//
// Then 'right 1 2' changes the position to (2,1) and updates the image
//
// 00
// 10
// 12
// 00
//
// then 'up 1' leaves the image unchanged, but updated the current position to (1,1)
// finally 'up 1 9` makes the position (0,1) -- the top right -- and the image
//
// 09
// 10
// 12
// 00

/**
 * Drawing Class.
 */
public class Drawing
{

    public int height;
    public int width;
    public int background;
    ArrayList<DrawingCommand> commands;

    /**
     * Read in an ArrayList of drawing commands from a file. There should be
     * exactly 1 command per line. The first two lines should be 2 numbers for
     * the height and width rather than commands. The third line is the
     * background colour.
     *
     * @param filename The file name containing the Drawing commands.
     */
    public Drawing(String filename)
    {
        commands = new ArrayList<DrawingCommand>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String s = in.readLine();
            try {
                height = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Expected the height on the first line: " + s);
                System.exit(1);
            }

            s = in.readLine();
            try {
                width = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Expected the width on the second line: " + s);
                System.exit(1);
            }

            s = in.readLine();
            try {
                background = Integer.parseInt(s, 16);
            } catch (NumberFormatException e) {
                System.out.println("Expected the background colour on the third line: " + s);
                System.exit(1);
            }

            for (s = in.readLine(); s != null; s = in.readLine()) {
                commands.add(new DrawingCommand(s));
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filename);
            System.exit(1);
        } catch (IOException e) {
            System.exit(2);
        }
    }

    /**
     * Create an empty drawing of the given dimensions.
     *
     * @param h The height of the drawing.
     * @param w The width of the drawing.
     * @param b The initial background colour.
     */
    public Drawing(int h, int w, int b)
    {
        height = h;
        width = w;
        assert (b >= 0 && b <= 15);
        background = b;
        commands = new ArrayList<DrawingCommand>();
    }

    /**
     * Main method.
     */
    public static void main(String[] args)
    {
        // A simple test to read in a file of drawing commands and print it out.
        Drawing d = new Drawing(args[0]);
        try {
            System.out.print(d.draw().toString());
        } catch (BadCommand e){
            System.out.println(e);
        }
    }

    /**
     * Add a command to the list of commands.
     *
     * @param c The command to add to the list.
     */
    public void addCommand(DrawingCommand c)
    {
        commands.add(c);
    }

    /**
     * Custom toString method for a list of Drawing commands.
     *
     * @return The list of drawing commands.
     */
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        s.append(height + "\n");
        s.append(width + "\n");
        s.append(Integer.toHexString(background) + "\n");
        for (DrawingCommand command : commands) {
            s.append(command.toString() + "\n");
        }
        return s.toString();
    }

    /**
     * Task 1: Implement the draw method to create and return an image by
     * executing all of the drawing commands in the commands field.
     * Throw a BadCommand exception if any command tries to paint outside of the
     * picture's dimensions, as given by the height and width field.
     * It is ok for the position to leave the dimensions, as long as no attempt
     * is made to paint outside of the picture.
     * (5 marks)
     *
     * @return An Image object.
     */
    public Image draw() throws BadCommand
    {
        Coordinate cursor = new Coordinate(0,0);
        Coordinate newCursor = new Coordinate(0,0);
        Image newImage = new Image(height, width, background);
        for (DrawingCommand command : commands) {
            if (command.distance == 0) {
                if (command.paint) {
                    newImage.set(cursor.x, cursor.y, command.colour);
                }
            } else if (command.dir == Direction.UP) {
                for (int i = 0; i < command.distance; i++) {
                    newCursor.y--;
                    if (command.paint) {
                        newImage.set(cursor.x, newCursor.y, command.colour);
                    }
                }
                cursor.y = newCursor.y;
            } else if (command.dir == Direction.DOWN) {
                for (int i = 0; i < command.distance; i++) {
                    newCursor.y++;
                    if (command.paint) {
                        newImage.set(cursor.x, newCursor.y, command.colour);
                    }
                }
                cursor.y = newCursor.y;
            } else if (command.dir == Direction.LEFT) {
                for (int i = 0; i < command.distance; i++) {
                    newCursor.x--;
                    if (command.paint) {
                        newImage.set(newCursor.x, cursor.y, command.colour);
                    }
                }
                cursor.x = newCursor.x;
            } else if (command.dir == Direction.RIGHT) {
                for (int i = 0; i < command.distance; i++) {
                    newCursor.x++;
                    if (command.paint) {
                        newImage.set(newCursor.x, cursor.y, command.colour);
                    }
                }
                cursor.x = newCursor.x;
            }
        }
        return newImage;
    }
}
