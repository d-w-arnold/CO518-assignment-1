import java.util.*;

/**
 * @author David W. Arnold
 * @version 14/10/2017
 */
public class Compressor
{
    private Image image;


    public Compressor(Image image)
    {
        this.image = image;
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
        Drawing drawing = new Drawing(height, width, color);
        int rowNum = 0;
        for (int[] row : image.pixels) {
            int pixelIndex;
            int pCount = 0;
            if ((rowNum%2) == 0) {
                pixelIndex = 0;
                while (pixelIndex < row.length && pixelIndex >= 0) {
                    int pColor = row[pixelIndex];
                    if (pixelIndex == row.length - 1 || pColor != row[pixelIndex + 1]) {
                        drawing.addCommand(new DrawingCommand("right " + pCount + " " + Integer.toString(pColor, 16)));
                    } else {
                        pCount++;
                    }
                    pixelIndex++;
                }
                pixelIndex--;
            } else {
                pixelIndex = row.length - 1;
                while (pixelIndex < row.length && pixelIndex >= 0) {
                    int pColor = row[pixelIndex];
                    if (pixelIndex == 0 || pColor != row[pixelIndex - 1]) {
                        drawing.addCommand(new DrawingCommand("left " + pCount + " " + Integer.toString(pColor, 16)));
                    } else {
                        pCount++;
                    }
                    pixelIndex--;
                }
                pixelIndex++;
            }

            if (rowNum < image.pixels.length - 1) {
                int pCol = image.pixels[rowNum+1][pixelIndex];
                drawing.addCommand(new DrawingCommand("down 1 " + Integer.toString(pCol, 16)));
            }
            rowNum++;
        }
        return drawing;
    }
}
