package dev.aoc.common.graphsearch;

/** A route node extended with estimated score, for A* algorithm */
public class RouteNodeEstimated<T extends GraphNode> extends RouteNode<T> {
    private long estimatedScore;

    public RouteNodeEstimated(T current) {
        this(current, null, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    public RouteNodeEstimated(T current, RouteNodeEstimated<T> previous, long routeScore, long estimatedScore) {
        super(current, previous, routeScore);
        this.estimatedScore = estimatedScore;
    }

    public RouteNodeEstimated<T> getPrevious() {
        return (RouteNodeEstimated<T>)previous;
    }

    public long getEstimatedScore() {
        return estimatedScore;
    }

    public void setEstimatedScore(long estimatedScore) {
        this.estimatedScore = estimatedScore;
    }

    public boolean equals(RouteNodeEstimated<T> that) {
        return current.equals(that.current);
    }

    @Override
    public int compareTo(RouteNode other) {
        if (getClass() == other.getClass()) {
            RouteNodeEstimated<?> otherRNE = (RouteNodeEstimated<?>)other;
            return Long.compare(this.estimatedScore, otherRNE.estimatedScore);
        } else {
            return super.compareTo(other);
        }
    }

    @Override
    public String toString() {
        return "[%s << %s|%d,%d]".formatted(current, false && previous != null ? previous : "{}", routeScore, estimatedScore);
    }
}
