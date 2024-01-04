package dev.aoc.common.graphsearch;

import java.util.Objects;

/** A node on the route, contains its associated graph node, previous route node and current route score. */
public class RouteNode<T extends GraphNode> implements Comparable<RouteNode<T>> {
    protected final T current;
    protected RouteNode<T> previous;
    protected long routeScore;

    public RouteNode(T current) {
        this(current, null, Long.MAX_VALUE);
    }

    public RouteNode(T current, RouteNode<T> previous, long routeScore) {
        if (current == null) {
            throw new IllegalArgumentException("graph node must not be null");
        }
        this.current = current;
        this.previous = previous;
        this.routeScore = routeScore;
    }

    public boolean isUninitialized() {
        return routeScore == Long.MAX_VALUE;
    }

    public T getCurrent() {
        return current;
    }

    public RouteNode<T> getPrevious() {
        return previous;
    }

    public void setPrevious(RouteNode<T> previous) {
        this.previous = previous;
    }

    public long getRouteScore() {
        return routeScore;
    }

    public void setRouteScore(long routeScore) {
        this.routeScore = routeScore;
    }

    public boolean equals(RouteNode<T> that) {
        return current.equals(that.current);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteNode<?> routeNode = (RouteNode<?>) o;
        return Objects.equals(current, routeNode.current);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(current);
    }

    @Override
    public int compareTo(RouteNode other) {
        return Long.compare(this.routeScore, other.routeScore);
    }

    @Override
    public String toString() {
        return "[%s << %s|%d]".formatted(current, previous != null ? previous : "{}", routeScore);
    }
}
