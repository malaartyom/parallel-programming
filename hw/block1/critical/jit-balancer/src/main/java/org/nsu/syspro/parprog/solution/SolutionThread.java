package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.CompilationEngine;
import org.nsu.syspro.parprog.external.ExecutionEngine;
import org.nsu.syspro.parprog.external.ExecutionResult;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.nsu.syspro.parprog.solution.BalancerState.L1;
import static org.nsu.syspro.parprog.solution.BalancerState.L2;

public class SolutionThread extends UserThread {


    /**
     * A thread-safe pool of threads for compiling methods asynchronously.
     * <p>
     */
    /*
    Сделал BalancerState не статическим. Внутри все поля статические, кроме usages.
    Вроде так все работает.

    Можно попробовать сделать одно поле статическим, другое нет. Но тогда возникает вопрос:
    А зачем нужен будет статическое поле? Да, мы будем его обновлять раз в k вызовов state.incrementUsages(id), но когда
    мы будем использовать информацию оттуда? Ведь во всех местах нам хватает информации и из локального счетчика использований
     */

    private static final int MAX_LOCAL_COUNTER = 100;
    private static final int F = 100;

    private int localCounter = 0;
    private final HashMap<MethodID, Future<?>> futures = new HashMap<>();



    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r, ExecutorService e, BalancerState b) {
        super(compilationThreadBound, exec, compiler, r, e, b);
    }

    @Override
    public ExecutionResult executeMethod(MethodID id) {
//        state.incrementUsages(id);
        increment(id);
        check(id);
        if (state.checkTimeOfCompilation(id)) {
            // TODO: Work with future (DONE)

            if (futures.containsKey(id)) {
                try {
                    futures.get(id).wait();
                } catch (InterruptedException e) {
                    System.out.println("Future was interrupted!");
                }
            }

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
        if (state.getUsages(id) + localCounter + F >= L1) {
            if (state.getCompiled(id).isEmpty()) {
                state.addCompiled(id, compiler.compile_l1(id));
            }
            if (state.getUsages(id) + localCounter + F >= L2 && !state.isCompiledL2(id)) {
                state.addCompiledL2(id);
                Future<?> f = executor.submit(() ->
                {
                    state.updateCompiling(id);
                    state.addCompiled(id, compiler.compile_l2(id));
                    state.deleteCompiling(id);
                    futures.remove(id);
                });
                futures.put(id, f);
            }
        }
    }

    private void increment(MethodID id) {
        localCounter++;
        if (localCounter >= MAX_LOCAL_COUNTER) {
            state.updateUsages(id, localCounter);
            localCounter = 0;
        }

    }
}