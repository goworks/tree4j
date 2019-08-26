package cc.goworks.tree4j.annotations;

@FunctionalInterface
public interface KeepGoing<T> {
    boolean judge(T o);
}
