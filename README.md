# lwdebugger

 This is a launching debugger, which means it launches a new instance of the Java Virtual Machine and connects to it for debugging. Do not forget to generate the reference information of the debugee with the `-g` flag. `javac -g Filename.java`. The provided examples are already compiled with it.

 Functions of the console application:

 ~~~text
Press h for help
Press e for exit
Press r for run
Press b for setting a breakpoint
Write db to delete a breakpoint
Write sb to show the breakpoints
Write sf to print the current stack frame
WWrite st to print the current stack trace
Press p for printing a object value or an array element
~~~

The main application is the `LWDebugger.java`.
