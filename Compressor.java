import java.util.*;
import java.util.stream.Collectors;

public class Compressor
{
    // TODO make four fields private
    private Image image;
    private Drawing drawing;
    private Coordinate cursor;
    private List<Coordinate> drawnCoordinates;
    private List<Integer> colors;
    private List<Coordinate> allCoordinates;
    private int colorIndexToTest = 0;
    private List<Integer> colorsDrawn;
    private ArrayList<Coordinate> allCoordinatesExceptBackground;

    public Compressor(Image image)
    {
        this.image = image;
        cursor = new Coordinate(0, 0);
        allCoordinates = new ArrayList<Coordinate>();
        drawnCoordinates = new ArrayList<Coordinate>();
        colorsDrawn = new ArrayList<Integer>();

        HashMap<Integer, Integer> mapOfColors = new HashMap<Integer, Integer>();
        int height = image.pixels.length;
        int width = image.pixels[0].length;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int count = 0;
                int color = image.getColor(x, y);
                if (mapOfColors.containsKey(color)) {
                    count = mapOfColors.get(color);
                }
                count++;
                mapOfColors.put(color, count);
            }
        }

        colors = mapOfColors.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        int backgroundColor = getColorToTest();
        colorsDrawn.add(backgroundColor);
        colorIndexToTest++;
        drawing = new Drawing(height, width, backgroundColor);
    }

    public Drawing compress()
    {
        for (int x = 0; x < image.pixels[0].length; x++) {
            for (int y = 0; y < image.pixels.length; y++) {
                allCoordinates.add(new Coordinate(x, y));
            }
        }
        allCoordinatesExceptBackground = new ArrayList<Coordinate>(allCoordinates);
        allCoordinatesExceptBackground.removeIf(coordinate -> image.getColor(coordinate.x, coordinate.y) == drawing.background);

        int i = 0;
        int spotInfiniteLoop = 1000;
        while (!drawnCoordinates.containsAll(allCoordinatesExceptBackground) && i < spotInfiniteLoop) {
            Map.Entry<Direction, Integer> pairDirectionLength = findBestNeighbourDirection();
            if (pairDirectionLength == null) {
                resolveStuckCase();
            } else {
                Direction d = pairDirectionLength.getKey();
                int l = pairDirectionLength.getValue();
                addCommand(d, l, true, getColorToTest());
            }
            ArrayList<Coordinate> currentColorDrawnCoordinates = new ArrayList<Coordinate>(drawnCoordinates);
            currentColorDrawnCoordinates.removeIf(coordinate -> image.getColor(coordinate) != getColorToTest());
            ArrayList<Coordinate> currentColorAllCoordinatesExceptBackground = new ArrayList<Coordinate>(allCoordinatesExceptBackground);
            currentColorAllCoordinatesExceptBackground.removeIf(coordinate -> image.getColor(coordinate) != getColorToTest());
            if (currentColorDrawnCoordinates.containsAll(currentColorAllCoordinatesExceptBackground)) {
                colorsDrawn.add(getColorToTest());
                colorIndexToTest++;
                clearDrawnCoordinates();
            }
            System.out.print("");
            spotInfiniteLoop++;
        }

        return drawing;
    }

    private void clearDrawnCoordinates()
    {
        drawnCoordinates.removeIf(coordinate -> !colorsDrawn.contains(image.getColor(coordinate)));
    }

    private int getColorToTest()
    {
        return colors.get(colorIndexToTest);
    }

    private void resolveStuckCase()
    {
        ArrayList<Coordinate> notDrawn = new ArrayList<Coordinate>(allCoordinates);
        notDrawn.removeAll(drawnCoordinates);
        notDrawn.removeIf(coordinate -> image.getColor(coordinate.x, coordinate.y) != getColorToTest());

        ArrayList<Line> lines = createLines();

        List<LineTargetCost> lineTargetCosts = new ArrayList<>();

        for (Line line : lines) {
            lineTargetCosts.addAll(findBestDrawingCoordinate(line));
        }

        int minCost = lineTargetCosts.stream().min(Comparator.comparingInt(o -> o.cost)).get().cost;

        Coordinate target = lineTargetCosts.stream()
                .filter(lineTargetCost -> lineTargetCost.cost == minCost)
                .max(Comparator.comparingInt((LineTargetCost ltc) -> ltc.line.getLength()))
                .get().target;

        if (target == null) {
            System.err.println("No coordinate, next to target coordinate, has been selected to resolve this stuck case.");
            return;
        }

        if (target.x < cursor.x) {
            addCommand(Direction.LEFT, Math.abs(cursor.x - target.x), false, 0);
        }
        if (target.x > cursor.x) {
            addCommand(Direction.RIGHT, Math.abs(target.x - cursor.x), false, 0);
        }
        if (target.y < cursor.y) {
            addCommand(Direction.UP, Math.abs(cursor.y - target.y), false, 0);
        }
        if (target.y > cursor.y) {
            addCommand(Direction.DOWN, Math.abs(target.y - cursor.y), false, 0);
        }
    }

    private ArrayList<Line> createLines()
    {
        ArrayList<Line> lines = new ArrayList<Line>();
        ArrayList<Coordinate> notDrawn = new ArrayList<>(allCoordinatesExceptBackground);
        notDrawn.removeIf(coordinate -> drawnCoordinates.contains(coordinate));
        notDrawn.removeIf(coordinate -> getColorToTest() != image.getColor(coordinate));
        for (Coordinate c : notDrawn) {
            int left = findNeighboursLength(Direction.LEFT);
            int right = findNeighboursLength(Direction.RIGHT);
            int up = findNeighboursLength(Direction.UP);
            int down = findNeighboursLength(Direction.DOWN);
            int horizontal = left + right + 1;
            int vertical = up + down + 1;
            if (horizontal == 1 && vertical == 1) {
                lines.add(new Line(c, c));
                continue;
            }
            if (vertical > 1) {
                lines.add(new Line(new Coordinate(c.x, c.y - up), new Coordinate(c.x, c.y + down)));
            }
            if (horizontal > 1) {
                lines.add(new Line(new Coordinate(c.x - left, c.y), new Coordinate(c.x + right, c.y)));
            }
        }

        return lines;
    }

    private List<LineTargetCost> findBestDrawingCoordinate(Line l)
    {
        ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
        Direction direction = l.getDirecton();
        LineType lineType = l.findLineType();
        if (lineType.equals(LineType.VERTICAL)) {

            if (direction.equals(Direction.DOWN)) {
                coordinates.add(new Coordinate(l.start.x, l.start.y - 1));
                coordinates.add(new Coordinate(l.end.x, l.end.y + 1));
            } else {
                // up
                coordinates.add(new Coordinate(l.start.x, l.start.y + 1));
                coordinates.add(new Coordinate(l.end.x, l.end.y - 1));
            }

        } else if (lineType.equals(LineType.HORIZONTAL)) {

            if (direction.equals(Direction.RIGHT)) {
                // right
                coordinates.add(new Coordinate(l.start.x - 1, l.start.y));
                coordinates.add(new Coordinate(l.end.x + 1, l.end.y));
            } else {
                // left
                coordinates.add(new Coordinate(l.start.x + 1, l.start.y));
                coordinates.add(new Coordinate(l.end.x - 1, l.end.y));
            }

        } else if (lineType.equals(LineType.SINGLE)) {
            coordinates.add(new Coordinate(l.start.x - 1, l.start.y));
            coordinates.add(new Coordinate(l.start.x + 1, l.start.y));
            coordinates.add(new Coordinate(l.start.x, l.start.y - 1));
            coordinates.add(new Coordinate(l.start.x, l.start.y + 1));
        }

        return coordinates.stream()
                .map(coordinate -> new LineTargetCost(l, coordinate, calculateCost(coordinate)))
                .collect(Collectors.toList());
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

    // TODO make addCommand method private
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
        boolean ifTheLineContainsColor = false;
        boolean ifTheLineHasNewThingsToBeDrawn = false;
        int i = -1;
        int reverseDirection = 1;
        if (d == Direction.LEFT || d == Direction.RIGHT) {

            if (d == Direction.LEFT) {
                reverseDirection = -1;
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
                if (colorsDrawn.contains(newColor)) {
                    break;
                }
                if (newColor == getColorToTest()) {
                    ifTheLineContainsColor = true;
                }
                if (!drawnCoordinates.contains(new Coordinate(newX, y))) {
                    ifTheLineHasNewThingsToBeDrawn = true;
                }
            }
            if (!ifTheLineContainsColor || !ifTheLineHasNewThingsToBeDrawn) {
                return 0;
            }
        } else {
            if (d == Direction.UP) {
                reverseDirection = -1;
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
                if (colorsDrawn.contains(newColor)) {
                    break;
                }
                if (newColor == getColorToTest()) {
                    ifTheLineContainsColor = true;
                }
                if (!drawnCoordinates.contains(new Coordinate(x, newY))) {
                    ifTheLineHasNewThingsToBeDrawn = true;
                }
            }
            if (!ifTheLineContainsColor || !ifTheLineHasNewThingsToBeDrawn) {
                return 0;
            }
        }

        return i;
    }
}

enum LineType
{
    VERTICAL, HORIZONTAL, SINGLE;
}

class Line
{
    protected Coordinate start;
    protected Coordinate end;

    public Line(Coordinate s, Coordinate e)
    {
        start = s;
        end = e;
    }

    protected LineType findLineType()
    {
        if (start.equals(end)) {
            return LineType.SINGLE;
        }
        if (start.x == end.x) {
            return LineType.VERTICAL;
        }
        if (start.y == end.y) {
            return LineType.HORIZONTAL;
        }

        return null;
    }

    protected Direction getDirecton()
    {
        if (start.x < end.x) {
            return Direction.RIGHT;
        }
        if (start.x > end.x) {
            return Direction.LEFT;
        }
        if (start.y < end.y) {
            return Direction.DOWN;
        }
        if (start.y > end.y) {
            return Direction.UP;
        }

        return null;
    }

    protected int getLength()
    {
        return Math.abs((start.x - end.x) + (start.y - end.y)) + 1;
    }
}

class LineTargetCost
{
    protected Line line;
    protected Coordinate target;
    protected int cost;

    public LineTargetCost(Line line, Coordinate target, int cost)
    {
        this.line = line;
        this.target = target;
        this.cost = cost;
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
