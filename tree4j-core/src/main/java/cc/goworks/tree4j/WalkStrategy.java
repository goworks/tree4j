package cc.goworks.tree4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cc.goworks.tree4j.annotations.KeepGoing;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

public abstract class WalkStrategy<T> {
    public T filterTree(T rootNode, Predicate<T> predicate) {
        List<T> matchedNodes = Lists.newArrayList();
        postWalk(rootNode, it -> {
            boolean isMatch = predicate.test(it);
            if (isMatch) {
                matchedNodes.add(it);
            }
            return true;
        });

        List<List<T>> pathList = emptyIfNull(matchedNodes).stream()
            .map(this::path)
            .collect(Collectors.toList());

        return mergePath(pathList);
    }

    private T mergePath(List<List<T>> pathList) {
        List<T> roots = bundlePath(pathList, 0);
        if (CollectionUtils.isEmpty(roots)) {
            return null;
        }
        return roots.get(0);
    }

    private List<T> bundlePath(List<List<T>> pathList, int level) {
        Map<T, List<List<T>>> groupedPath = groupPath(pathList, level);
        List<T> parents = Lists.newArrayList();
        groupedPath.forEach((parent, childrenPath) -> {
            T clonedParent = this.clone(parent);
            parents.add(clonedParent);
            if (CollectionUtils.isEmpty(childrenPath)) {
                return ;
            }
            List<T> children = bundlePath(childrenPath, level + 1);
            this.setChildren(clonedParent, children);
        });
        return parents;
    }

    private Map<T, List<List<T>>> groupPath(List<List<T>> pathList, int level) {
        return emptyIfNull(pathList).stream()
            .filter(x -> x.size() > level)
            .collect(Collectors.groupingBy(x -> {
                return x.get(level);
            }));
    }

    /**
     * 深度优先遍历
     * @param node
     * @param doKeepGoing
     * @return true/false 是否继续遍历
     */
    private boolean postWalk(T node, KeepGoing<T> doKeepGoing) {
        boolean keepGoing;
        List<T> children = this.getChildren(node);
        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                keepGoing = postWalk(child, doKeepGoing);
                if (!keepGoing) {
                    return false;
                }
            }
        }
        return doKeepGoing.judge(node);
    }

    public T pruneTree(T node, Predicate<T> predicate) {
        if (CollectionUtils.isEmpty(this.getChildren(node))) {
            boolean isMatch = predicate.test(node);
            if (isMatch) {
                return node;
            }
            return null;
        }
        List<T> filteredChildren = emptyIfNull(this.getChildren(node)).stream()
            .map(child -> pruneTree(child, predicate))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        this.setChildren(node, filteredChildren);
        if (CollectionUtils.isEmpty(filteredChildren)) {
            return null;
        }
        return node;
    }


    private boolean bubbleWalk(T node, KeepGoing<T> doKeepGoing) {
        boolean keepGoing = doKeepGoing.judge(node);
        if (!keepGoing) {
            return false;
        }
        T parent = this.getParent(node);
        if (Objects.nonNull(parent)) {
            return bubbleWalk(parent, doKeepGoing);
        }
        return true;
    }

    protected List<T> path(T node) {
        List<T> path = Lists.newArrayList();
        this.bubbleWalk(node, it -> {
            path.add(it);
            return true;
        });
        Collections.reverse(path);
        return path;
    }

    public abstract List<T> getChildren(T parent);
    public abstract void setChildren(T parent, List<T> children);
    public abstract T getParent(T child);
    public abstract T clone(T node);
}
