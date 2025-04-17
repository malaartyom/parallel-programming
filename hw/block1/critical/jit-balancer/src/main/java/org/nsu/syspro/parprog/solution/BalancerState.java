package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompiledMethod;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
// TODO: Передавать в конструктор  (DONE)
public class BalancerState {
    /**
     * Stores the usage count of methods, where the key is the method's {@code id}
     * and the value is the number of times it has been used.
     */
    private final HashMap<Long, Integer> usages = new HashMap<>();

    /**
     * Stores compiled methods, where the key is the method's {@code id}
     * and the value is a {@link CompiledMethod} object representing the compiled version of the method.
     */
    private static final HashMap<Long, CompiledMethod> compiledMethods = new HashMap<>();

    /**
     * Contains the {@code id} of methods that have been compiled by level 2 (L2).
     */
    private static final HashSet<Long> compiledByLevel2 = new HashSet<>();

    /**
     * This map contains all methods that are compiling at level 2.
     * <p>
     * В этой хэшмапе хранится количество использований
     * уже скомпилированного под уровень L1 метода пока этот метод
     * компиляруется под L2. Т.е я помещаю метод в эту хэмапу,
     * когда начинаю компилировать этот метод под L2.
     * Далее если у меня вызывается метод getCompiled, то я
     * добавлю единицу в хэшапу, используя в качестве ключа id этого метода.
     * Если в какой-то момент количество использований такого метода
     * превышает {@link #MAX_WAITING} константы, то я дожидаюсь
     * пока компиляция под L2 завершится. Что обеспечивает Eventual-per-thread-progress-2
     * </p>
     */
    private static final HashMap<Long, Integer> compiling = new HashMap<>();

    /**
     * A ReadWriteLock to work with shared resources.
     */
    private final static ReadWriteLock lock = new ReentrantReadWriteLock();

    public static final int L1 = 20;
    public static final int L2 = 900;

    private static final int MAX_WAITING = 90_000;

    public BalancerState() {
    }


    /**
     * Increments the usage count for the specified {@link MethodID}.
     * <p>
     * This method updates the {@link #usages} HashMap in a thread-safe manner
     * using {@link ReadWriteLock} {@link #lock}. If the method ID is not already in the map,
     * it is initialized with a count of zero before being incremented.
     * </p>
     *
     * @param id the {@link MethodID} whose usage count should be incremented
     */
    public void incrementUsages(MethodID id) {
        lock.writeLock().lock();
        try {
            usages.put(id.id(), usages.getOrDefault(id.id(), 0) + 1);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Getter of usages for specified {@link MethodID}
     * <p>
     * This method is thread safe as it acquires read lock before getting the value
     * </p>
     *
     * @param id
     * @return number of usages of methods with given {@link MethodID}
     */

    public int getUsages(MethodID id) {
        lock.readLock().lock();
        try {
            return usages.getOrDefault(id.id(), 0);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Checks if given {@link MethodID} is compiled at level 2.
     * Uses compiledByLevel2 {@link HashSet} for this purpose.
     * <p>
     * This method is thread-safe as it acquires a read lock before checking
     * {@link #compiledByLevel2} for presence of {@link MethodID}.
     * </p>
     *
     * @param id {@link MethodID} that will be checked
     * @return {@code true} if given method is compiled at level 2, {@code false} otherwise
     */

    public boolean isCompiledL2(MethodID id) {
        lock.readLock().lock();
        try {
            return compiledByLevel2.contains(id.id());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Adds given {@link MethodID} in {@link #compiledByLevel2} {@link HashSet}
     *
     * <p>
     * This method is thread-safe as it acquires a write lock before modifying
     * the {@link #compiledByLevel2} set.
     * </p>
     *
     * @param id that will be added to {@link #compiledByLevel2}
     */

    public void addCompiledL2(MethodID id) {
        lock.writeLock().lock();
        try {
            compiledByLevel2.add(id.id());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adds a pair of key and value into {@link #compiledMethods} {@link HashMap}.
     * <p>
     * This method is thread-safe as it acquires a write lock before modifying
     * the {@link #compiledMethods} map.
     * </p>
     *
     * @param id a {@link MethodID} key representing the unique identifier
     * @param m  a {@link CompiledMethod} value to be associated with the given key
     */
    public void addCompiled(MethodID id, CompiledMethod m) {
        lock.writeLock().lock();
        try {
            compiledMethods.put(id.id(), m);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * This method checks whether specified method is compiled.
     * For this purpose {@link #compiledMethods} map is used.
     * <p>
     * Method is thread-safe and uses {@link ReadWriteLock} {@link #lock}.
     * </p>
     *
     * @param id
     * @return {@code true} if method is compiled, {@code false} if not.
     */
//    public boolean isCompiled(MethodID id) {
//        lock.readLock().lock();
//        try {
//            return compiledMethods.containsKey(id.id());
//        } finally {
//            lock.readLock().unlock();
//        }
//    }

    /**
     * Returns already compiled method using method id
     * and {@link #compiledMethods} map.
     * <p>
     * This method is thread-safe as it uses {@link ReadWriteLock} {@link #lock}
     *
     * @param id
     * @return {@link CompiledMethod}
     */

    public Optional<CompiledMethod> getCompiled(MethodID id) {
        lock.writeLock().lock();
        try {
            if (compiling.containsKey(id.id())) {
                updateCompiling(id);
            }
            if (!compiledMethods.containsKey(id.id())) return Optional.empty();
            return Optional.of(compiledMethods.get(id.id()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Updates value in {@link #compiling} by adding one to it
     * If there is no this key in {@link HashMap} {@link #compiling} then
     * a zero value is placed at this key. And after that one adds to this value.
     *
     * @param id {@link MethodID} the key by which the value will be updated
     */
    public void updateCompiling(MethodID id) {
        compiling.put(id.id(), compiling.getOrDefault(id.id(), 0) + 1);

    }

    /**
     * Checks if compiling of specified method is too slow.
     *
     * @param id {@link MethodID} key
     * @return {@code true} if {@link #compiling} contains id key and if
     * value at this key is greater than {@link #MAX_WAITING} and {@code false} otherwise
     */
    public boolean checkTimeOfCompilation(MethodID id) {
        lock.readLock().lock();
        try {
            return compiling.containsKey(id.id()) && compiling.get(id.id()) >= MAX_WAITING;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void deleteCompiling(MethodID id) {
        lock.writeLock().lock();
        try {
            compiling.remove(id.id());
        } finally {
            lock.writeLock().unlock();
        }
    }
    public void updateUsages(MethodID id, int localUsages) {
        lock.writeLock().lock();
        try {
            usages.put(id.id(), usages.getOrDefault(id.id(), 0) + localUsages);
        } finally {
            lock.writeLock().unlock();
        }

    }
    public void syncUsages(HashMap<Long, Integer> localUsages) {
        lock.writeLock().lock();
        try {
            for (Long key : localUsages.keySet()) {
                usages.put(key, localUsages.get(key));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
