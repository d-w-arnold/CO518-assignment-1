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

    public Compressor(Image image)
    {
        this.image = image;
        cursor = new Coordinate(0,0);
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

        while (true) {
            System.out.println(findNeighboursLength(Direction.RIGHT));
            break;
        }

        return drawing;
    }

    private int findNeighboursLength(Direction d)
    {
        // if not stuck
        int initialColor = image.get(cursor.x + 1, cursor.y);
        int i = -1;
        while (true) {
            i++;
            int newColor;
            int newX = cursor.x + i + 1;
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
