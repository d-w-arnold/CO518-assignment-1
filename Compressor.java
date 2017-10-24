import java.util.*;
import java.util.stream.Collectors;

/**
 * Compressor Class.
 */
public class Compressor
{
    private Image image;
    private Drawing drawing;
    private Coordinate cursor;
    private List<Coordinate> drawnCoordinates;
    private List<Integer> colors;
    private List<Coordinate> allCoordinates;
    private int colorIndexToTest = 0;
    private List<Integer> colorsDrawn;
    private ArrayList<Coordinate> allCoordinatesExceptBackground;

    /**
     * Reads in the image to be compressed.
     *
     * @param image The image to be compressed.
     */
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

    /**
     * The method used to compress an image into Drawing commands.
     *
     * @return The compressed drawing object
     */
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

    /**
     * Clears the drawnCoordinates list of any coordinates containing colours
     * which aren't suppose to have been drawn yet.
     */
    private void clearDrawnCoordinates()
    {
        drawnCoordinates.removeIf(coordinate -> !colorsDrawn.contains(image.getColor(coordinate)));
    }

    /**
     * Get the colour we will be painting with as we draw colours on top of the background.
     *
     * @return The colour we will be painting.
     */
    private int getColorToTest()
    {
        return colors.get(colorIndexToTest);
    }

    /**
     * If no coordinate adjacent to the cursor is drawable, I look for the next drawable coordinate
     * with the smallest number of commands to move there, but with the
     * greatest number of pixels in a straight line (more pixels drawn per command).
     */
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

    /**
     * Creates an arraylist of potential lines which can be drawn, so that the
     * resolveStuckCase method can decide which one is best to draw next.
     *
     * @return The potential lines to draw in a stuck case scenario.
     */
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

    /**
     * Finds the best coordinate to move to to draw a line in a stuck case scenario.
     *
     * @param l The line we know we are going to draw.
     * @return A ListTargetCost object containing detail of the line, the target
     * coordinate and the cost of getting to that coordinate.
     */
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

    /**
     * Calculates the cost of moving to a specific coordinate from where the cursor is currently positioned.
     *
     * @param c The coordinate to test.
     * @return The number of moves it would take to move there.
     */
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

    /**
     * Add a command to the string of Drawing commands.
     *
     * @param d The direction.
     * @param l The length.
     * @param paint True to draw as the cursor moves, false to not draw.
     * @param color The color to draw, if paint is set to true.
     */
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

    /**
     * Finds the best way to move out of the adjacent pixels from the cursor.
     *
     * @return A map entry set
     */
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

    /**
     * Test how far can we move in one direction from where the cursor is currently located.
     *
     * @param d The Direction enum to test.
     * @return The number of pixels we can move in a straight line,
     * if we can'ty move in that direction, the method returns 0.
     */
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

/**
 * LineType Enum.
 */
enum LineType
{
    VERTICAL, HORIZONTAL, SINGLE;
}

/**
 * Line Class.
 */
class Line
{
    protected Coordinate start;
    protected Coordinate end;

    /**
     * Create a line, with a start and end point, so we can try to draw an entire line in one command.
     *
     * @param s The start coordinate.
     * @param e The end coordinate.
     */
    public Line(Coordinate s, Coordinate e)
    {
        start = s;
        end = e;
    }

    /**
     * Finds out whether the line is a single coordinate, a vertical line or a horizontal line.
     *
     * @return A LineType object, which is one of three Enums: SINGLE, VERTICAL and HORIZONTAL.
     */
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

    /**
     * Gets the direction of a line by comparing the x and y values of the start and end points.
     *
     * @return A Direction object defining the direction of travel.
     */
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

    /**
     * Calculates the length of a line.
     *
     * @return The length of the line.
     */
    protected int getLength()
    {
        return Math.abs((start.x - end.x) + (start.y - end.y)) + 1;
    }
}

/**
 * LineTargetCost Class.
 */
class LineTargetCost
{
    protected Line line;
    protected Coordinate target;
    protected int cost;

    /**
     * Defining an object containing the target to start drawing a line and
     * the cost of moving the cursor to that point.
     *
     * @param line The line we want to draw.
     * @param target The coordinate we move to to draw this line.
     * @param cost The cost of moving to that coordinate.
     */
    public LineTargetCost(Line line, Coordinate target, int cost)
    {
        this.line = line;
        this.target = target;
        this.cost = cost;
    }
}

/**
 * Coordinate Class.
 */
class Coordinate
{
    int x;
    int y;

    /**
     * Provide x and y values for the Coordinate object.
     *
     * @param x
     * @param y
     */
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
