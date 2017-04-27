import java.util.LinkedList;

/**
 * Created by ehe on 4/27/17.
 */

public class UnionFind {

    private int[] x;
    private int size;

    public UnionFind (int size) {
        this.size = size;
        x = new int[size];
        for (int i = 0; i < size; i++)
            x[i] = i;
    }

    public LinkedList<Integer> merge (int a, int b) {
        LinkedList<Integer> group = new LinkedList<>();
        int p1 = find(a);
        int p2 = find(b);
        for (int i = 0; i < this.size; i++) {
            if (x[i] == p1 || x[i] == p2) {
                group.add(i);
                if (x[i] == p2) x[i] = p1;
            }
        }
        return group;
    }

    private int find(int i) {
        return x[i];
    }

    // method to help with testing
//    public boolean inSame(int a, int b) {
//        return (find(a) == find(b));
//    }
//
//    public void printArray() {
//        for (int i : x)
//            System.out.print(i + " ");
//        System.out.println();
//    }

    public static void main(String[] args) {
//        UnionFind uf = new UnionFind(10);
//        System.out.println(uf.inSame(4,5));
//        System.out.println(uf.inSame(2,1));
//        System.out.println(uf.inSame(3,9));
//        System.out.println(uf.inSame(0,2));
//        uf.printArray();
//        uf.merge(1,3);
//        uf.printArray();
//        uf.merge(1,7);
//        uf.printArray();
//        System.out.println(uf.inSame(3,7));
//        uf.merge(2,9);
//        uf.printArray();
//        uf.merge(0,2);
//        uf.printArray();
//        System.out.println(uf.inSame(0, 9));
//        System.out.println(uf.inSame(1, 2));
//        LinkedList<Integer> g = uf.merge(7, 0);
//        System.out.println(uf.inSame(1,2));
//        for (int i : g)
//            System.out.println(i);
    }


}
