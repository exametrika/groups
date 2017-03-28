import org.jline.utils.AttributedStringBuilder;

public class Test
{
    public static void main(String[] args) throws Throwable
    {
        AttributedStringBuilder s = new AttributedStringBuilder();
        s.appendAnsi("abc def");
        String s2 = s.append(" ttt").toAnsi();
        System.out.println(s2);
    }

}
