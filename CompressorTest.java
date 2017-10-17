import org.junit.Test;
import java.util.ArrayList;
import java.util.Objects;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class CompressorTest
{
    @Test
    public void image1()
    {
        testWithFile("./test-image1");
    }

    @Test
    public void image2()
    {
        testWithFile("./test-image2");
    }

    @Test
    public void image3()
    {
        testWithFile("./test-image3");
    }

    @Test
    public void image4()
    {
        testWithFile("./test-image4");
    }

    @Test
    public void image5()
    {
        testWithFile("./test-image5");
    }

    @Test
    public void pixel1()
    {
        testWithFile("./pixel-art1");
    }

    @Test
    public void pixel2()
    {
        testWithFile("./pixel-art2");
    }

    @Test
    public void pixel3()
    {
        testWithFile("./pixel-art3");
    }

    @Test
    public void pixel4()
    {
        testWithFile("./pixel-art4");
    }

    @Test
    public void pixel5()
    {
        testWithFile("./pixel-art5");
    }

    @Test
    public void pixel6()
    {
        testWithFile("./pixel-art6");
    }

    private void testWithFile(String filename)
    {
        Image i = new Image(filename);

        String s = filename.substring(2);

        Drawing d = i.compress();

        System.out.println(s + " commands: " + d.commands.size());

        try {
            assertEquals(i.toString(), d.draw().toString());
        } catch(BadCommand e) {
            fail(e.toString());
        }
    }
}
