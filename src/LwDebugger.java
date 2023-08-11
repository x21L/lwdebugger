import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

public class LwDebugger<T> {
  private final Class<T> debuggee;
  private final VirtualMachine virtualMachine;
  private final Scanner scanner;

  public LwDebugger(Class<T> debuggee) throws IllegalConnectorArgumentsException, VMStartException, IOException {
    this.debuggee = debuggee;
    virtualMachine = connectAndLaunchVirtualMachine();
    scanner = new Scanner(System.in);
  }

  public void debug() throws IllegalConnectorArgumentsException, VMStartException, IOException {
    System.out.println("Welcome to the debugger");
    printHelp();
    // EventRequestManager reqManager = virtualMachine.eventRequestManager();
    // EventQueue q = virtualMachine.eventQueue();
    startDebugger();
  }

  private void startDebugger() throws IllegalConnectorArgumentsException, VMStartException, IOException {
    LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
    Map<String, Connector.Argument> env = launchingConnector.defaultArguments();
    env.get("main").setValue(debuggee.getName());
    env.get("options").setValue("-cp Documents/University/System\\ Software/lwdebugger ");
    listenForDebugEvents();
  }

  public void listenForDebugEvents() throws IllegalConnectorArgumentsException, VMStartException, IOException {
    // MethodEntryRequest req = reqManager.createMethodEntryRequest();
    // req.addClassFilter(debuggee.getName());
    // req.enable();
    EventRequestManager reqManager = virtualMachine.eventRequestManager();
    EventQueue q = virtualMachine.eventQueue();

    ClassPrepareEvent classPrepareEvent = null;
    prepareRequest(reqManager);

    try {
      while (true) {

        EventSet events = q.remove();
        for (Event event : events) {
          if (event instanceof ClassPrepareEvent eventClassPrepareEvent) {
            classPrepareEvent = eventClassPrepareEvent;
            debuggerActions(classPrepareEvent);
          }

          if (event instanceof BreakpointEvent breakpointEvent) {
            System.out.println("breakpoint at " + breakpointEvent.location().lineNumber() + " in " +
                breakpointEvent.location().method().toString());
            printVars(breakpointEvent.thread().frame(0));
            debuggerActions(classPrepareEvent);
          }

          if (event instanceof StepEvent stepEvent) {
            System.out.print("step halted in " + stepEvent.location().method().name() + " at ");
            printLocation(stepEvent.location());
            printVars(stepEvent.thread().frame(0));
            reqManager.deleteEventRequest(stepEvent.request());
            debuggerActions(classPrepareEvent);
          }

          if (event instanceof VMDisconnectEvent) {
            System.out.println("VM is now disconnected.");
            return;
          }
          virtualMachine.resume();
        }

      }
    } catch (VMDisconnectedException e) {
      System.out.println("VM unexpectedly disconnected.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void debuggerActions(ClassPrepareEvent event)
      throws AbsentInformationException, IncompatibleThreadStateException {
    String currentLine;
    loop: for (; (currentLine = scanner.nextLine()) != "e";) {
      switch (currentLine) {
        case "r":
          virtualMachine.resume();
          break loop;

        case "b":
          setBreakpoint(event);
          break;

        case "n":
          stepOver(event);
          break loop;

        case "ni":
          stepInto(event);
          break loop;

        case "sb":
          showBreakpoints();
          break;

        case "h":
          printHelp();
          break;

        case "p":
          printElement(event);
          break;

        case "db":
          deleteBreakpoint();
          break;

        case "sf":
          System.out.println("====current stack frame====");
          printVars(event.thread().frames().get(0));
          System.out.println("===========================");
          break;

        case "st":
          System.out.println("====current stack trace====");
          printStackTrace(event.thread());
          System.out.println();
          System.out.println("===========================");
          break;

        default:
          break;
      }
    }
  }

  private void showBreakpoints() {
    for (BreakpointRequest breq : virtualMachine.eventRequestManager().breakpointRequests()) {
      System.out.println(breq.location() + " in method " + breq.location().method());
    }
  }

  private void setBreakpoint(ClassPrepareEvent event) throws AbsentInformationException {
    System.out.println("Enter the line for the breakpoint: ");
    int line = Integer.parseInt(scanner.nextLine());
    ClassType classType = (ClassType) event.referenceType();
    Location location = classType.locationsOfLine(line).get(0);
    BreakpointRequest breakpointRequest = virtualMachine.eventRequestManager().createBreakpointRequest(location);
    breakpointRequest.enable();
    System.out.println("Breakpoint set at line " + line);
  }

  private void deleteBreakpoint() {
    System.out.println("Enter the line for the breakpoint: ");
    int line = Integer.parseInt(scanner.nextLine());

    for (BreakpointRequest breakpointRequest : virtualMachine.eventRequestManager().breakpointRequests()) {
      if (breakpointRequest.location().lineNumber() == line) {
        virtualMachine.eventRequestManager().deleteEventRequest(breakpointRequest);
        System.out.println("Breakpoint at line " + line + " deleted");
        return;
      }
    }
  }

  private void stepOver(ClassPrepareEvent event) {
    try {
      StepRequest req = virtualMachine.eventRequestManager().createStepRequest(event.thread(), StepRequest.STEP_LINE,
          StepRequest.STEP_OVER);
      req.addClassFilter(debuggee.getName());
      req.addCountFilter(1); // create step event after 1 step
      req.enable();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void stepInto(ClassPrepareEvent event) {
    try {
      System.out.println("inside step into");
      StepRequest req = virtualMachine.eventRequestManager().createStepRequest(event.thread(), StepRequest.STEP_LINE,
          StepRequest.STEP_INTO);
      req.addClassFilter(debuggee.getName());
      req.addCountFilter(1); // create step event after 1 step
      req.enable();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void printElement(ClassPrepareEvent event)
      throws AbsentInformationException, IncompatibleThreadStateException {
    System.out.println("Insert variable name to print");
    String name = scanner.nextLine();

    Value variableValue = null;
    for (LocalVariable localVariable : event.thread().frame(0).visibleVariables()) {
      if (name.equals(localVariable.name())) {
        variableValue = event.thread().frame(0).getValue(localVariable);
        break;
      }
    }

    if (variableValue == null) {
      System.out.println("variable: " + name + " not found. Probably a wrong input.");
      return;
    }

    if (variableValue instanceof ArrayReference arrayReference) {
      System.out.println("Which index of the array should be printed?");
      int index = Integer.parseInt(scanner.nextLine());

      if (index < 0 || index >= arrayReference.length()) {
        System.out.println("Index out of bounds");
        return;
      }

      System.out.println(name + "[" + index + "]: " + arrayReference.getValue(index));
    }

    if (variableValue instanceof ObjectReference objectReference) {
      System.out.println("Which field should be printed?");
      String fieldName = scanner.nextLine();
      ClassType classType = (ClassType) objectReference.type();
      Field field = classType.fieldByName(fieldName);

      if (field == null) {
        System.out.println("Field " + fieldName + " cannot be found, probably a wrong input.");
        return;
      }

      System.out.print(name + "." + fieldName + ": ");
      printValue(objectReference.getValue(field));
      System.out.println();
    }
  }

  private void prepareRequest(EventRequestManager reqManager) {
    ClassPrepareRequest classPrepareRequest = virtualMachine.eventRequestManager().createClassPrepareRequest();
    classPrepareRequest.addClassFilter(debuggee.getName());
    classPrepareRequest.enable();
  }

  private VirtualMachine connectAndLaunchVirtualMachine()
      throws IllegalConnectorArgumentsException, VMStartException, IOException {
    LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
    Map<String, Connector.Argument> env = launchingConnector.defaultArguments();
    env.get("main").setValue(debuggee.getName());

    VirtualMachine vm = launchingConnector.launch(env);
    Process proc = vm.process();
    new Redirection(proc.getErrorStream(), System.err).start();
    new Redirection(proc.getInputStream(), System.out).start();
    return vm;
  }

  private void printStackTrace(ThreadReference threadReference) throws IncompatibleThreadStateException {
    for (StackFrame stackFrame : threadReference.frames()) {
      System.out.print(stackFrame.location().method().name() + ": " + stackFrame.location().lineNumber());
    }
  }

  private static void printLocation(Location loc) {
    System.out.println(loc.lineNumber() + ", " + loc.codeIndex());
  }

  private static void printVars(StackFrame frame) {
    try {
      for (LocalVariable v : frame.visibleVariables()) {
        System.out.print(v.name() + ": " + v.type().name() + " = ");
        printValue(frame.getValue(v));
        System.out.println();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void printValue(Value val) {
    if (val instanceof IntegerValue integerValue) {
      System.out.print(integerValue.value() + " ");
    } else if (val instanceof StringReference stringReference) {
      System.out.print(stringReference.value() + " ");
    } else if (val instanceof ArrayReference arrayReference) {
      for (Value v : arrayReference.getValues()) {
        printValue(v);
        System.out.print(" ");
      }
    } else if (val instanceof ObjectReference objectReference) {
      System.out.print(objectReference.toString() + " ");
    } else if (val instanceof DoubleValue doubleValue) {
      System.out.print(doubleValue.value() + " ");
    } else if (val instanceof FloatValue floatValue) {
      System.out.print(floatValue.value() + " ");
    } else if (val instanceof ByteValue byteValue) {
      System.out.print(byteValue.value() + " ");
    } else if (val instanceof ShortValue shortValue) {
      System.out.print(shortValue.value() + " ");
    } else if (val instanceof LongValue longValue) {
      System.out.print(longValue.value() + " ");
    } else if (val instanceof CharValue charValue) {
      System.out.print(charValue.value() + " ");
    }
  }

  private void printHelp() {
    System.out.println("""
        Press h for help
        Press e for exit
        Press r for run
        Press b for setting a breakpoint
        Write db to delete a breakpoint
        Write sb to show the breakpoints
        Write sf to print the current stack frame
        Write st to print the current stack trace
        Press p for printing a object value or an array element
        """);
  }
}
