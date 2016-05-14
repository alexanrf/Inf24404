import java.util.Iterator;
import java.util.TreeMap;
import java.util.Set;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        //QuickHullPara qHull = new QuickHullPara(100, 1, true);
        runTests();
    }

    static void runTests(){
        int threadCount = Runtime.getRuntime().availableProcessors();
        System.out.println("Cores available: " + threadCount);
        int tests = 9;
        int[] n = {100, 1000, 10000, 100000, 1000000, 10000000, 70000000};
        for(int i = 0; i < n.length; i++){
            System.out.print("--- n: " +  n[i] + " ---");
            for(int t = 0; t < tests; t++){
                QuickHullPara qHull = new QuickHullPara(n[i],threadCount, false); //Change threadCount to 1 if you want to run sequentially
                qHull.findHull();
                System.out.print("Test: "+ t + " - ");
            }
        }

    }
}

class QuickHullPara{
    int n;
    int[] x, y;
    Oblig4 oblig4;
    NPunkter p;
    int threadCount;
    boolean draw;

    QuickHullPara(int n, int threadCount, boolean draw){
        this.n = n;
        x = new int[n];
        y = new int[n];
        p = new NPunkter(n);
        p.fyllArrayer(x, y);
        this.threadCount = threadCount;
        this.draw = draw;
    }

    //Left, right, Point we want to check
    public int pointSide(int a, int b, int i){
        int returnVal = (x[b] - x[a]) * (y[i] - y[a]) - (y[b] - y[a]) * (x[i] - x[a]);
        if (returnVal > 0)
            return 1;
        else if (returnVal == 0)
            return 0;
        else
            return -1;
    }

    void findHull(){

        IntList hull = new IntList();
        IntList onPoint = new IntList();

        //Find smallest and largest points on the X axis
        int minP = -1, maxP = -1;
        int minX = x[0], maxX = x[0];

        for(int i = 0; i < x.length; i++){
            if(x[i] < minX){
                if(minP == -1){
                    minX = x[i];
                    minP = i;
                }
                else if(y[minP] > y[i]){
                    minX = x[i];
                    minP = i;
                }
            }
            if(x[i] > maxX) {
                if (maxP == -1) {
                    maxX = x[i];
                    maxP = i;
                } else if (y[maxP] < y[i]) {
                    maxX = x[i];
                    maxP = i;
                }
            }
        }


        //Make 2 sets of the points above/below the line
        IntList leftPoints = new IntList(); //More logical saying its below the line at this stage
        IntList rightPoints = new IntList(); //And above the line
        for(int i = 0; i < n; i++){
            int sideInt = pointSide(minP, maxP, i);
            if(sideInt == -1){
                leftPoints.add(i);
            }else if(sideInt == 1){
                rightPoints.add(i);
            }
        }


        long t = System.nanoTime(); //Starts clock
        if(threadCount == 1){ //1 core on CPU
            hull.add(minP);
            HullSet hullSet = new HullSet(hull, rightPoints, minP, maxP, 0, onPoint);
            hullSet.run();
            hull.add(maxP);
            hullSet = new HullSet(hull, leftPoints, maxP, minP, 0, onPoint);
            hullSet.run();

        }
        else{
            hull.add(minP);
            //Decides how far down to go. Only utilises 1,2,4,8,16,32 etc cores. Will utilise all the cores on MOST machines (3, 6, 12 core CPU's are rare)
            int level = -1;
            for(int i = threadCount; i > 0; i = i/2){
                level++;
            }

            //Starts up 2 threads
            IntList onPoint2 = new IntList();
            level--;
            IntList hull1 = new IntList();
            HullSet hullSet1 = new HullSet(hull1, rightPoints, minP, maxP, level, onPoint);
            hullSet1.start();
            IntList hull2 = new IntList();
            HullSet hullSet2 = new HullSet(hull2, leftPoints, maxP, minP, level, onPoint2);
            hullSet2.start();

            try{ //Waits for them to finish, then adds the hulls together, and the onPoints.
                hullSet1.join();
                for(int i = 0; i < hullSet1.hull.size(); i++){
                    hull.add(hullSet1.hull.get(i));
                }
                hull.add(maxP);
                hullSet2.join();
                for(int i = 0; i < hullSet2.hull.size(); i++){
                    hull.add(hullSet2.hull.get(i));
                }
                for(int i = 0; i < onPoint2.size(); i++){
                    onPoint.add(onPoint2.get(i));
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        double time = (System.nanoTime() - t) / 1000000.0; //Ends clock
        System.out.println("Time to run: " + time);

        if(draw){ //Draws the graph if constructor argument is true
            Oblig4 o = new Oblig4(x, y, n, hull, p);
            o.draw();
        }


        //Prints out info about the hull
        //System.out.println("Found: " + (hull.size() + onPoint.size()) + " points in the convex hull (" + onPoint.size()+" unsorted) where n = " + n);

        /* Prints out the points
        for(int i = 0; i < hull.size(); i++){
            System.out.println("P: " + hull.get(i) + " - X: " + x[hull.get(i)] + " - Y: " + y[hull.get(i)]);
        }
        System.out.println("--- Unsorted Points ---");
        for(int i = 0; i < onPoint.size(); i++){
            System.out.println("P: " + onPoint.get(i) + " - X: " + x[onPoint.get(i)] + " - Y: " + y[onPoint.get(i)]);
        }
        */
    }


    public int distance(int a, int b, int i){ //Finds the distance from the vector to the point
        return Math.abs((x[b] - x[a]) * (y[a] - y[i]) - (y[b] - y[a]) * (x[a] - x[i]));
    }

    private class HullSet extends Thread{
        int level;
        IntList hull, pointSet, onPoint;
        int a, b;

        HullSet(IntList hull, IntList pointSet, int a, int b, int level, IntList onPoint){
            this.hull = hull;
            this.pointSet = pointSet;
            this.a = a;
            this.b = b;
            this.level = level;
            this.onPoint = onPoint;
        }

        @Override
        public void run(){
            if(pointSet.size() == 0){ //No more points - Exit
                return;
            }

            //Find point furthest away
            int dist = -1, furthestPoint = -1;
            for(int i = 0; i < pointSet.size(); i++){
                int tempDist = distance(a, b, pointSet.get(i));
                if(tempDist > dist){
                    dist = tempDist;
                    furthestPoint = pointSet.get(i);
                }
            }

            if(pointSet.size() == 1){
                hull.add(pointSet.get(0)); //Only 1 point - Add to set and exit
                return;
            }

            //Find the points to the right of A to C (Right set)
            //Then find the points the the left of B to C (left set)
            //Left/Right is such an abstract concept when applied here. Names could be wrong, but it works

            IntList rightPoints = new IntList();
            IntList leftPoints = new IntList();
            //Below was an attempt at trying to get the onPoints in the hull set in the correct order.
            //I wasn't able to make it work.
            //My idea was to sort the values on the distance from a/b. And add them together at the end.
            //Didn't find anything that worked
            //Made an unsorted list instead
            //TreeMap<Double,Integer> tmRight = new TreeMap<Double,Integer>();
            //TreeMap<Double,Integer> tmLeft = new TreeMap<Double,Integer>();

            //Finds out which side the points are on (or on the line)
            //There could be a small inefficiency on the ifs here. But not much compared to the math.
            for(int i = 0; i < pointSet.size(); i++){
                int point = pointSet.get(i);
                int sideInt = pointSide(a, furthestPoint, point);
                int sideInt2 = pointSide(b, furthestPoint, point);
                if(sideInt == 1 && sideInt2 != -1){
                    rightPoints.add(point);
                }
                else if(sideInt == -1 && sideInt2 == -1){
                    leftPoints.add(point);
                }

                else if((sideInt == 0 || sideInt2 == 0) && (point != a && point != b && point != furthestPoint)){
                    onPoint.add(point);
                    //double distance = Math.sqrt(Math.abs((double)(x[point] - x[a]) + (y[point] - y[a])));
                    //tmRight.put(distance, point);
                }
                /*
                else if(sideInt2 == 0 && (point != a && point != b && point != furthestPoint)){
                    //double distance = Math.sqrt(Math.abs((double)(x[point] - x[a]) + (y[point] - y[a])));
                    //tmLeft.put(distance, point);
                }
                */

            }


            if(level < 1){//Sequential
                HullSet hullSet = new HullSet(hull, rightPoints, a, furthestPoint, level, onPoint);
                hullSet.run();
                hull.add(furthestPoint);
                hullSet = new HullSet(hull, leftPoints, furthestPoint,b, level, onPoint);
                hullSet.run();
            }
            else{ //Parallell
                level--; //We go down a level
                IntList hull1 = new IntList(); //Two new hull sets, one for each side.
                IntList onPoint1 = new IntList(); //New on Point as well.
                HullSet hullSet1 = new HullSet(hull1, rightPoints, a, furthestPoint, level, onPoint1);
                hullSet1.start();
                IntList hull2 = new IntList();
                HullSet hullSet2 = new HullSet(hull2, leftPoints, furthestPoint,b, level, onPoint);
                hullSet2.run(); //I don't make a new thread here, this nodes thread goes down to the other set instead of waiting for 2 threads to finish.

                try{
                    hullSet1.join();
                    for(int i = 0; i < hullSet1.hull.size(); i++){
                        hull.add(hullSet1.hull.get(i));
                    }
                    hull.add(furthestPoint);
                    for(int i = 0; i < onPoint1.size(); i++){
                        onPoint.add(onPoint1.get(i));
                    }

                    for(int i = 0; i < hullSet2.hull.size(); i++){
                        hull.add(hullSet2.hull.get(i));
                    }

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}


class Oblig4{
    int MAX_Y;
    int MAX_X;
    int[] x;
    int[] y;
    int n;
    IntList coHull;
    TegnUt draw;
    NPunkter nPunkter;

    Oblig4(int[] x, int[] y, int n, IntList coHull, NPunkter nPunkter){
        this.nPunkter = nPunkter;
        this.MAX_X = nPunkter.maxXY;
        this.MAX_Y = nPunkter.maxXY;
        this.x = x;
        this.y = y;
        this.n = n;
        this.coHull = coHull;
    }

    void draw(){
        draw = new TegnUt(this, coHull, "Oblig4");
    }

}
