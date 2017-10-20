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
    public static Collection<Object> data()
    {
        return Arrays.asList(new Object[]{
                "./test-image1",
                "./test-image2",
                "./test-image3",
                "./test-image4",
                "./test-image5",
                "./pixel-art1",
                "./pixel-art2",
                "./pixel-art3",
                "./pixel-art4",
                "./pixel-art5",
                "./pixel-art6",
        });
    }

    @RunWith(Parameterized.class)
    public static class ParameterizeTests
    {
        @Parameter
        public String filename;

        @Parameters(name = "{0}")
        public static Collection<Object> data()
        {
            return CompressorTestWithScore.data();
        }

        @Test
        public void testWithFile()
        {
            Image i = new Image(filename);

            Drawing d = i.compress();

            String name = filename.substring(2);

            System.out.println(name + " Commands: " + d.commands.size());

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

            for (Object f : CompressorTestWithScore.data()) {
                Image i = new Image((String) f);
                Drawing d = i.compress();
                score += d.commands.size();
            }

            System.out.println("Total Commands: " + score);
            System.out.println();
        }
    }
}
