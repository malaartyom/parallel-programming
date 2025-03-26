package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.ExecutionEngine;
import org.nsu.syspro.parprog.external.ExecutionResult;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.nsu.syspro.parprog.solution.BalancerState.L1;
import static org.nsu.syspro.parprog.solution.BalancerState.L2;

public class SolutionThread extends UserThread {


    /**
     * A thread-safe pool of threads for compiling methods asynchronously.
     * <p>
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final BalancerState state = new BalancerState();

    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
    }

    @Override
    public ExecutionResult executeMethod(MethodID id) {
        state.incrementUsages(id);
        check(id);
        if (state.checkTimeOfCompilation(id)) {
            executor.shutdown();
        }
        var compiled = state.getCompiled(id);
        if (compiled.isPresent()) return exec.execute(compiled.get());
        else return exec.interpret(id);
    }

    /**
     * Checks if method should be compiled and what level of compilation should be used
     * <p>
     * This method uses information in {@link #state} to check if specified method
     * needs compilation. If usages of method with {@link #id} more than some constant L1
     * then method checks if higher level of compilation is needed. If so compiler
     * compiles method with given {@link #id} by l2 compiler. Otherwise, checks if
     * method is already compiled. If not, method with given {@link #id} is compiled by l1
     * compiler
     *
     * </p>
     *
     * @param id - the {@link MethodID} which we want to check
     */
    private void check(MethodID id) {
        if (state.getUsages(id) >= L1) {
            if (state.getCompiled(id).isEmpty()) {
                state.addCompiled(id, compiler.compile_l1(id));
            }
            if (state.getUsages(id) >= L2 && !state.isCompiledL2(id)) {
                state.addCompiledL2(id);
                executor.submit(() ->
                {
                    state.updateCompiling(id);
                    state.addCompiled(id, compiler.compile_l2(id));
                    state.deleteCompiling(id);
                });
            }
        }
    }

}