package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SolutionThread extends UserThread {

    /**
     * Stores the usage count of methods, where the key is the method's {@code id}
     * and the value is the number of times it has been used.
     */
    private static final HashMap<Long, Integer> usages = new HashMap<>();

    /**
     * Stores compiled methods, where the key is the method's {@code id}
     * and the value is a {@link CompiledMethod} object representing the compiled version of the method.
     */
    private static final HashMap<Long, CompiledMethod> methods = new HashMap<>();

    /**
     * Contains the {@code id} of methods that have been compiled at level 2 (L2).
     */
    private static final HashSet<Long> compiledL2 = new HashSet<>();

    /**
     * A thread-safe pool of threads for compiling methods asynchronously.
     *
     * BTW it is not yet used :)
     */
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * An object used for synchronization to ensure thread safety when working with shared resources.
     */
    private final static Object lock = new Object();

    private static final int L1 = 20;
    private static final int L2 = 900;


    /**
     * Increments the usage count for the specified {@link MethodID}.
     * <p>
     * This method updates the {@link #usages} HashMap in a thread-safe manner
     * using synchronization on lock. If the method ID is not already in the map,
     * it is initialized with a count of zero before being incremented.
     * </p>
     *
     * @param id the {@link MethodID} whose usage count should be incremented
     */
    private void incUsages(MethodID id) {
        synchronized (lock) {
            if (!usages.containsKey(id.id())) {
                usages.put(id.id(), 0);
            }
            usages.put(id.id(), usages.get(id.id()) + 1);
        }
    }

    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
    }

    @Override
    public ExecutionResult executeMethod(MethodID id) {
        incUsages(id);
        check(id);
        if (isCompiled(id)) return exec.execute(getCompiled(id));
        else return exec.interpret(id);
    }

    /**
     * Checks if method should be compiled and what level of compilation should be used
     * <p>
     *     This method uses information in {@link #usages} map to check if specified method
     *     needs compilation. If usages of method with {@link #id} more than some constant L1
     *     then method checks if higher level of compilation is needed. If so compiler
     *     compiles method with given {@link #id} by l2 compiler. Otherwise, checks if
     *     method is already compiled. If not, method with given {@link #id} is compiled by l1
     *     compiler
     *
     * </p>
     * <p>
     *   This method is thread-safe as it synchronizes on {@link #lock} to ensure
     *   atomic updates to the {@link #usages} map and compilation process.
     * </p>
     * @param id - the {@link MethodID} which we want to check
     */
    private void check(MethodID id) {
        synchronized (lock) {
            if (usages.get(id.id()) >= L1) {
                if (usages.get(id.id()) >= L2 && !compiledL2.contains(id.id())) {
                    compiledL2.add(id.id());
                    methods.put(id.id(), compiler.compile_l2(id));
                } else if (!methods.containsKey(id.id())) {
                    methods.put(id.id(), compiler.compile_l1(id));
                }
            }
        }

    }

    /**
     * Returns already compiled method using method id
     * and {@link #methods} map.
     *
     * This method is thread-safe as it synchronizes on lock
     * @param id
     * @return {@link CompiledMethod}
     */
    private CompiledMethod getCompiled(MethodID id) {
        synchronized (lock) {
            return methods.get(id.id());
        }
    }

    /**
     * This method checks whether specified method is compiled.
     * For this purpose {@link #methods} map is used.
     *  <p>
     *      Method is thread-safe and uses synchronization on lock.
     *  </p>
     *
     * @param id
     * @return {@code true} if method is compiled, {@code false} if not.
     */
    synchronized private boolean isCompiled(MethodID id) {
        synchronized (lock) {
            return methods.containsKey(id.id());
        }
    }

// TODO: add methods
// TODO: add inner classes
// TODO: add utility classes in the same package
}