package org.nsu.syspro.parprog.examples;

import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.ExecutionEngine;
import org.nsu.syspro.parprog.external.ExecutionResult;
import org.nsu.syspro.parprog.external.MethodID;
import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.solution.BalancerState;

import java.util.concurrent.ExecutorService;

/**
 * Straightforward solution that always interprets given method.
 */
public final class Interpreter extends UserThread {

    public Interpreter(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r, ExecutorService e, BalancerState b) {
        super(compilationThreadBound, exec, compiler, r, e, b);
    }

    @Override
    public ExecutionResult executeMethod(MethodID id) {
        return exec.interpret(id);
    }
}
