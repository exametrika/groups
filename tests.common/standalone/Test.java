import com.exametrika.common.shell.IShell;
import com.exametrika.common.shell.impl.ShellBuilder;

public class Test
{
    public static void main(String[] args) throws Throwable
    {
        IShell shell = new ShellBuilder().title("Test application.").build();
        shell.run();
    }
}
