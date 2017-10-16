import java.util.*;

/**
 * @author David W. Arnold
 * @version 14/10/2017
 */
public class Compressor
{
    private Image image;
    private Drawing drawing;
    private Coordinate cursor;
    private ArrayList<Coordinate> allCoordinates;
    ArrayList<Coordinate> drawnCoordinates;

    public Compressor(Image image)
    {
        this.image = image;
        cursor = new Coordinate(0,0);
        allCoordinates = new ArrayList<Coordinate>();
        drawnCoordinates = new ArrayList<Coordinate>();
    }

    public Drawing compress()
    {
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        int height = image.pixels.length;
        int width = image.pixels[0].length;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int tmp = 0;
                int n = image.pixels[y][x];
                if (map.containsKey(n)) {
                    tmp = map.get(n);
                }
                tmp++;
                map.put(n, tmp);
            }
        }
        int color = Collections.max(map.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
        drawing = new Drawing(height, width, color);
        for (int x = 0; x < image.pixels[0].length; x++) {
            for (int y = 0; y < image.pixels.length; y++) {
                allCoordinates.add(new Coordinate(x,y));
            }
        }
        while (!drawnCoordinates.containsAll(allCoordinates)) {
            Map.Entry<Direction, Integer> pairDirectionLength = findBestNeighbourDirection();
            Direction d = pairDirectionLength.getKey();
            int l = pairDirectionLength.getValue();
            addCommand(d, l, true, 0);

            // System.out.println(findNeighboursLength(Direction.LEFT));
            break;
        }

        return drawing;
    }

    private void addCommand(Direction d, int l, boolean paint, int color)
    {
        if (paint) {
            drawing.addCommand(new DrawingCommand(d + " " + l + " " + color));
        } else {
            drawing.addCommand(new DrawingCommand(d + " " + l));
        }
        if (d == Direction.LEFT) {
            for (int i = 1; i <= l; i++) {
                drawnCoordinates.add(new Coordinate(cursor.x - i, cursor.y));
            }
        }
        if (d == Direction.RIGHT) {
            for (int i = 1; i <= l; i++) {
                drawnCoordinates.add(new Coordinate(cursor.x + i, cursor.y));
            }
        }
        if (d == Direction.UP) {
            for (int i = 1; i <= l; i++) {
                drawnCoordinates.add(new Coordinate(cursor.x, cursor.y - i));
            }
        }
        if (d == Direction.DOWN) {
            for (int i = 1; i <= l; i++) {
                drawnCoordinates.add(new Coordinate(cursor.x, cursor.y + i));
            }
        }
        if (d == Direction.LEFT) {
            cursor = new Coordinate(cursor.x - l, cursor.y);
        }
        if (d == Direction.RIGHT) {
            cursor = new Coordinate(cursor.x + l, cursor.y);
        }
        if (d == Direction.UP) {
            cursor = new Coordinate(cursor.x, cursor.y - l);
        }
        if (d == Direction.DOWN) {
            cursor = new Coordinate(cursor.x, cursor.y + l);
        }
    }

    private Map.Entry<Direction, Integer> findBestNeighbourDirection()
    {
        HashMap<Direction, Integer> map = new HashMap<Direction, Integer>();
        map.put(Direction.LEFT, findNeighboursLength(Direction.LEFT));
        map.put(Direction.RIGHT, findNeighboursLength(Direction.RIGHT));
        map.put(Direction.UP, findNeighboursLength(Direction.UP));
        map.put(Direction.DOWN, findNeighboursLength(Direction.DOWN));

        return Collections.max(map.entrySet(), Comparator.comparingInt(Map.Entry::getValue));
    }

    private int findNeighboursLength(Direction d)
    {
        // if not stuck
        int i = -1;
        int reverseDirection = 1;
        if (d == Direction.LEFT || d == Direction.RIGHT) {
            int initialColor;
            if (d == Direction.LEFT) {
                reverseDirection = -1;
            }
            try {
                initialColor = image.get(cursor.x + reverseDirection, cursor.y);
            } catch (ArrayIndexOutOfBoundsException e) {
                return 0;
            }
            while (true) {
                i++;
                int newColor;
                int newX = cursor.x + ((i + 1) * reverseDirection);
                int y = cursor.y;
                try {
                    newColor = image.get(newX, y);
                } catch (ArrayIndexOutOfBoundsException e) {
                    break;
                }
                if (newColor == initialColor) {
                    continue;
                }
                break;
            }
        } else {
            int initialColor;
            if (d == Direction.UP) {
                reverseDirection = -1;
            }
            try {
                initialColor = image.get(cursor.x, cursor.y + reverseDirection);
            } catch (ArrayIndexOutOfBoundsException e) {
                return 0;
            }
            while (true) {
                i++;
                int newColor;
                int newY = cursor.y + ((i + 1) * reverseDirection);
                int x = cursor.x;
                try {
                    newColor = image.get(x, newY);
                } catch (ArrayIndexOutOfBoundsException e) {
                    break;
                }
                if (newColor == initialColor) {
                    continue;
                }
                break;
            }
        }
        return i;
    }
}

class Coordinate
{
    int x;
    int y;

    public Coordinate(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode()
    {
        return x * y + x;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Coordinate) {
            Coordinate c = (Coordinate) obj;
            return c.x == this.x && c.y == this.y;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return x + "," + y;
    }
}
