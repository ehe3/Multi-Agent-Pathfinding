import java.util.LinkedList;

/**
 * Created by Eric He on 4/27/17.
 *
 * A naive disjoint set union data structure to keep track of the merging in the independence detection algorithm.
 */

public class UnionFind {

    // array that keeps track of the robots, robots range from [1, size]
    private int[] x;
    // total number of agents, all IDs should be less than or equal to this number
    public int size;

    // basic constructor
    public UnionFind (int size) {
        this.size = size;
        x = new int[size];
        for (int i = 0; i < size; i++)
            x[i] = i + 1;
    }

    // merges two robots together and returns all of the robots IDs in both groups as a linked list
    // naive implementation
    public LinkedList<Integer> merge (int a, int b) {
        LinkedList<Integer> group = new LinkedList<>();
        int p1 = find(a);
        int p2 = find(b);
        for (int i = 0; i < this.size; i++) {
            if (x[i] == p1 || x[i] == p2) {
                group.add(i + 1);
                if (x[i] == p2) x[i] = p1;
            }
        }
        return group;
    }

    // helper method that returns the group representative in the array
    private int find(int i) {
        return x[i - 1];
    }
}
