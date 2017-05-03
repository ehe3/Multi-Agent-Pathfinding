import java.util.LinkedList;

/**
 * Created by ehe on 4/27/17.
 */

public class UnionFind {

    private int[] x;
    public int size;

    public UnionFind (int size) {
        this.size = size;
        x = new int[size];
        for (int i = 0; i < size; i++)
            x[i] = i + 1;
    }

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

    private int find(int i) {
        return x[i - 1];
    }

     //method to help with testing
    public boolean inSame(int a, int b) {
        return (find(a) == find(b));
    }

    public void printArray() {
        for (int i : x)
            System.out.print(i + " ");
        System.out.println();
    }

    public static void main(String[] args) {
        UnionFind uf = new UnionFind(9);
        uf.printArray();
        System.out.println(uf.inSame(4,5));
        System.out.println(uf.inSame(2,1));
        System.out.println(uf.inSame(3,9));
        System.out.println(uf.inSame(6,2));
        uf.printArray();
        uf.merge(1,3);
        uf.printArray();
        uf.merge(1,7);
        uf.printArray();
        System.out.println(uf.inSame(3,7));
        uf.merge(2,9);
        uf.printArray();
        uf.merge(6,2);
        uf.printArray();
        System.out.println(uf.inSame(6, 9));
        System.out.println(uf.inSame(1, 2));
        LinkedList<Integer> g = uf.merge(7, 6);
        System.out.println(uf.inSame(1,2));
        for (int i : g)
            System.out.println(i);
    }


}
