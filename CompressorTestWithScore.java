import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Enclosed.class)
public class CompressorTestWithScore
{
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][]{
                {"./test-image1", 14, 14},
                {"./test-image2", 30, 29},
                {"./test-image3", 200, 200},
                {"./test-image4", 23, 22},
                {"./test-image5", 92, 26},
                {"./pixel-art1", 230, 215},
                {"./pixel-art2", 197, 181},
                {"./pixel-art3", 44, 43},
                {"./pixel-art4", 58, 55},
                {"./pixel-art5", 201, 176},
                {"./pixel-art6", 128, 113},
        });
    }

    @RunWith(Parameterized.class)
    public static class ParameterizeTests
    {
        @Parameter
        public String filename;

        @Parameter(1)
        public int maxScore;

        @Parameter(2)
        public int compScore;

        @Parameters(name = "{0}")
        public static Collection<Object[]> data()
        {
            return CompressorTestWithScore.data();
        }

        @Test
        public void testWithFile()
        {
            Image i = new Image(filename);

            Drawing d = i.compress();

            String name = filename.substring(2);

            //assertEquals(maxScore, d.commands.size());
            System.out.println(name + " Commands: " + d.commands.size() + " - Competitor score beats me by: " + (d.commands.size() - compScore));

            try {
                assertEquals(i.toString(), d.draw().toString());
            } catch (BadCommand e) {
                fail(e.toString());
            }
        }
    }

    public static class SingleTests
    {
        @Test
        public void score()
        {
            int score = 0;
            int diff = 0;

            for (Object[] f : CompressorTestWithScore.data()) {
                Image i = new Image((String) f[0]);
                Drawing d = i.compress();
                score += d.commands.size();
                diff += d.commands.size() - (int) f[2];
            }

            System.out.println("Total Commands: " + score + " - Competitor total beats me by: " +  diff);
            System.out.println();
        }
    }
}
