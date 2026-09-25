package nl.melonstudios.create.entity.train.graph;

/**
 * One undirected link between two neighbouring rail nodes.
 *
 * <p>Length is the horizontal centre distance between the two rail blocks
 * (1.0 for straight steps, about 1.41 across a corner diagonal). Cost starts
 * equal to length; routing may add junction penalties on top without touching
 * this stored base value.</p>
 */
public final class TrackGraphEdge {
    private final TrackGraphNode a;
    private final TrackGraphNode b;
    private final double length;
    private final double cost;

    public TrackGraphEdge(TrackGraphNode a, TrackGraphNode b, double length) {
        this.a = a;
        this.b = b;
        this.length = length;
        this.cost = length;
    }

    public TrackGraphNode getA() {
        return this.a;
    }

    public TrackGraphNode getB() {
        return this.b;
    }

    public double getLength() {
        return this.length;
    }

    public double getCost() {
        return this.cost;
    }

    /** The endpoint that is not {@code from} (expects {@code from} to be one end). */
    public TrackGraphNode other(TrackGraphNode from) {
        return from.equals(this.a) ? this.b : this.a;
    }

    @Override
    public String toString() {
        return "Edge" + this.a.getPos() + "<->" + this.b.getPos() + " len=" + this.length;
    }
}
