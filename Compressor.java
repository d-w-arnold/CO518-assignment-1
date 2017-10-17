import java.util.*;
import java.util.stream.Collectors;

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
    }

    public Drawing compress()
    {
        for (int x = 0; x < image.pixels[0].length; x++) {
            for (int y = 0; y < image.pixels.length; y++) {
                allCoordinates.add(new Coordinate(x,y));
            }
        }

        ArrayList<Coordinate> allCoordinatesExceptBackground = new ArrayList<Coordinate>(allCoordinates);
        allCoordinatesExceptBackground.removeIf(coordinate -> image.getColor(coordinate.x, coordinate.y) == drawing.background);

        while (!drawnCoordinates.containsAll(allCoordinatesExceptBackground)) {
            Map.Entry<Direction, Integer> pairDirectionLength = findBestNeighbourDirection();
            if(pairDirectionLength == null) {
                resolveStuckCase();
            } else {
                Direction d = pairDirectionLength.getKey();
                int l = pairDirectionLength.getValue();
                addCommand(d, l, true, getColorDirection(d));
            }
            System.out.print("");
        }

        return drawing;
    }

    private int getColorDirection(Direction d)
    {
        if (d == Direction.LEFT) {
            return image.getColor(cursor.x - 1, cursor.y);
        }
        if (d == Direction.RIGHT) {
            return image.getColor(cursor.x + 1, cursor.y);
        }
        if (d == Direction.UP) {
            return image.getColor(cursor.x, cursor.y - 1);
        }
        if (d == Direction.DOWN) {
            return image.getColor(cursor.x, cursor.y + 1);
        }

        return 0;
    }

    private void resolveStuckCase()
    {
        ArrayList<Coordinate> notDrawn = new ArrayList<Coordinate>(allCoordinates);
        notDrawn.removeAll(drawnCoordinates);
        notDrawn.removeIf(coordinate -> image.getColor(coordinate.x, coordinate.y) == drawing.background);

        Coordinate coordinateSelected = null;
        int numOfMovesRequired = -1;
        for (Coordinate coordinate : notDrawn) {
            int costCalculated = calculateCost(coordinate);
            if (numOfMovesRequired == -1 || costCalculated < numOfMovesRequired) {
                numOfMovesRequired = costCalculated;
                coordinateSelected = coordinate;
            }
        }

        if (coordinateSelected == null) {
            System.err.println("No coordinate has been selected to resolve this stuck case.");
            return;
        }

        ArrayList<Coordinate> spotsAroundCoordinateSelected = new ArrayList<Coordinate>();
        spotsAroundCoordinateSelected.add(new Coordinate(coordinateSelected.x + 1, coordinateSelected.y));
        spotsAroundCoordinateSelected.add(new Coordinate(coordinateSelected.x - 1, coordinateSelected.y));
        spotsAroundCoordinateSelected.add(new Coordinate(coordinateSelected.x, coordinateSelected.y + 1));
        spotsAroundCoordinateSelected.add(new Coordinate(coordinateSelected.x, coordinateSelected.y - 1));

        Coordinate nextToCoordinateSelected = null;
        int smallestCost = -1;
        for (Coordinate coordinate : spotsAroundCoordinateSelected) {
            int costCalculated = calculateCost(coordinate);
            if (smallestCost == -1 || costCalculated < smallestCost) {
                smallestCost = costCalculated;
                nextToCoordinateSelected = coordinate;
            }
        }

        if (nextToCoordinateSelected == null) {
            System.err.println("No coordinate, next to target coordinate, has been selected to resolve this stuck case.");
            return;
        }

        if (nextToCoordinateSelected.x < cursor.x) {
            addCommand(Direction.LEFT, Math.abs(cursor.x - nextToCoordinateSelected.x), false, 0);
        }
        if (nextToCoordinateSelected.x > cursor.x) {
            addCommand(Direction.RIGHT, Math.abs(nextToCoordinateSelected.x - cursor.x), false, 0);
        }
        if (nextToCoordinateSelected.y < cursor.y) {
            addCommand(Direction.UP, Math.abs(cursor.y - nextToCoordinateSelected.y), false, 0);
        }
        if (nextToCoordinateSelected.y > cursor.y) {
            addCommand(Direction.DOWN, Math.abs(nextToCoordinateSelected.y - cursor.y), false, 0);
        }
    }

    private int calculateCost(Coordinate c)
    {
        int cost = 0;
        if (c.x != cursor.x) {
            cost++;
        }
        if (c.y != cursor.y) {
            cost++;
        }

        return cost;
    }

    //make private
    private void addCommand(Direction d, int l, boolean paint, int color)
    {
        String newHexColor = Integer.toString(color, 16);
        if (paint) {
            drawing.addCommand(new DrawingCommand(d + " " + l + " " + newHexColor));
        } else {
            drawing.addCommand(new DrawingCommand(d + " " + l));
        }

        if (paint) {
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
        Map<Direction, Integer> map = new HashMap<Direction, Integer>();
        map.put(Direction.LEFT, findNeighboursLength(Direction.LEFT));
        map.put(Direction.RIGHT, findNeighboursLength(Direction.RIGHT));
        map.put(Direction.UP, findNeighboursLength(Direction.UP));
        map.put(Direction.DOWN, findNeighboursLength(Direction.DOWN));

        map = map.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (map.size() == 0) {
            return null;
        }

        return Collections.max(map.entrySet(), Comparator.comparingInt(Map.Entry::getValue));
    }

    private int findNeighboursLength(Direction d)
    {
        int i = -1;
        int reverseDirection = 1;
        if (d == Direction.LEFT || d == Direction.RIGHT) {
            int initialColor;
            if (d == Direction.LEFT) {
                reverseDirection = -1;
            }
            try {
                initialColor = image.getColor(cursor.x + reverseDirection, cursor.y);
            } catch (ArrayIndexOutOfBoundsException e) {
                return 0;
            }
            while (true) {
                i++;
                int newColor;
                int newX = cursor.x + ((i + 1) * reverseDirection);
                int y = cursor.y;
                try {
                    newColor = image.getColor(newX, y);
                } catch (ArrayIndexOutOfBoundsException e) {
                    break;
                }
                if (drawnCoordinates.contains(new Coordinate(newX, y)) || image.getColor(newX, y) == drawing.background) {
                    break;
                }
                if (newColor != initialColor) {
                    break;
                }
            }
        } else {
            int initialColor;
            if (d == Direction.UP) {
                reverseDirection = -1;
            }
            try {
                initialColor = image.getColor(cursor.x, cursor.y + reverseDirection);
            } catch (ArrayIndexOutOfBoundsException e) {
                return 0;
            }
            while (true) {
                i++;
                int newColor;
                int newY = cursor.y + ((i + 1) * reverseDirection);
                int x = cursor.x;
                try {
                    newColor = image.getColor(x, newY);
                } catch (ArrayIndexOutOfBoundsException e) {
                    break;
                }
                if (drawnCoordinates.contains(new Coordinate(x, newY)) || image.getColor(x, newY) == drawing.background) {
                    break;
                }
                if (newColor != initialColor) {
                    break;
                }
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
