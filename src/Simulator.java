import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

/**
 * Created by Eric He on 5/3/17.
 */
public class Simulator {

    public static void main(String[] args) throws TimeoutException, ContradictionException {
        int iterations = 100;
        int gridLength = 7;
        int agents = 10;
        int bound = 3;

        if (2 * agents > gridLength * gridLength) {
            System.out.println("Agents too large or Grid too small");
            return;
        }

        for (int i = 1; i <= iterations; i++) {
            Game g = new Game(gridLength, bound);
            System.out.println();
            System.out.println("Agents:");
            for (int a = 1; a <= agents; a++) {
                Agent zero = new Agent(a, gridLength);
                while(!g.add(zero)) {
                    zero = new Agent(a, gridLength);
                }
                System.out.println(a + ": start: (" + zero.getSI() + ", " + zero.getSJ() + "), end: (" + zero.getEI() + ", " + zero.getEJ() + ")");

            }
            g.run();
        }
    }
}
