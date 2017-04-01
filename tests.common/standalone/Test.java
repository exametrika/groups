import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.exametrika.common.shell.IShell;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandExecutor;
import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.shell.impl.ShellBuilder;
import com.exametrika.common.shell.impl.ShellCommandBuilder;

public class Test
{
    public static void main(String[] args) throws Throwable
    {
        List<IShellCommand> commands = new ShellCommandBuilder()
            .names("l1:l2").description("L2 description").namespace().addCommand()
            .names("l1:l2:command1").description("Command1 description")
                .addNamedParameter("p1", Arrays.asList("-p1", "--p1-long"), "-p1, --p1-long", 
                    "P1 description", "P1 short", false)
                .addPositionalParameter("p2", "<p2>", "P2 description", "P2 short", null, null, null)
                .defaultParameter("p3", "<p3>", "P3 description", "P3 short", true, true, null, null, null, null).executor(new TestShellCommand()).addCommand()
            .build();
        IShell shell = new ShellBuilder().title("Test application.").commands(commands).build();
        shell.run();
    }
    private static class TestShellCommand implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellContext context, Map<String, Object> parameters)
        {
            return null;
        }
    }
}
