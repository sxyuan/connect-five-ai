/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package AI;

import java.awt.Point;

/**
 *
 * @author samuel
 */
public class VETest {

    public static void main(String[] args) {

        /* Inc/dec tests */
        Point a = new Point(6, 6),
              b = new Point(3, 3);
        Point c = VectorEval.inc(a, b),
              d = VectorEval.inc(b, a),
              e = VectorEval.dec(a, b),
              f = VectorEval.dec(b, a);
        System.out.println(c);
        System.out.println(d);
        System.out.println(e);
        System.out.println(f);

        
        
    }

}
