package dev.aoc.common.graphsearch;

import java.util.Objects;

public class RouteNode<T extends GraphNode> implements Comparable<RouteNode<T>> {
    private final T current;
    private RouteNode<T> previous;
    private long routeScore;
    private long estimatedScore;

    public RouteNode(T current) {
        this(current, null, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    public RouteNode(T current, RouteNode<T> previous, long routeScore, long estimatedScore) {
        if (current == null) {
            throw new IllegalArgumentException("graph node must not be null");
        }
        this.current = current;
        this.previous = previous;
        this.routeScore = routeScore;
        this.estimatedScore = estimatedScore;
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

    public void setEstimatedScore(long estimatedScore) {
        this.estimatedScore = estimatedScore;
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
        return Objects.hash(current);
    }

    @Override
    public int compareTo(RouteNode other) {
        return Long.compare(this.estimatedScore, other.estimatedScore);
    }

    @Override
    public String toString() {
        return "{%s << %s|%d,%d}".formatted(current, previous != null ? previous : "0", routeScore, estimatedScore);
    }
}
