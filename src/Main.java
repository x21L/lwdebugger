import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.VMStartException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IllegalConnectorArgumentsException, VMStartException, IOException, InterruptedException {
        // String debuggeeClassName = args[0];
        // System.out.println(debuggeeClassName);
        LwDebugger<Calc> debugger = new LwDebugger<>(Calc.class);
      //  debugger.addBreakpoint(5);
      //  debugger.addBreakpoint(6);
        debugger.debug();
    }
}
