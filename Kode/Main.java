import java.util.Iterator;
import java.util.TreeMap;
import java.util.Set;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        //QuickHullPara qHull = new QuickHullPara(50, 1, true);
        //qHull.findHull();
        runTests();
    }

    static void runTests(){
        int threadCount = Runtime.getRuntime().availableProcessors();
        System.out.println("Cores available: " + threadCount);
        int tests = 9;
        int[] n = {100, 1000, 10000, 100000, 1000000, 10000000, 50000000};

        for(int i = 0; i < n.length; i++){
            System.out.println("n   tidSekv     tidPara     speedup");
            for(int t = 0; t < tests; t++){
                QuickHullPara qHullSekv = new QuickHullPara(n[i], 1, false);
                double tidSekv = qHullSekv.findHull();
                tidSekv = Math.floor(tidSekv * 100) / 100;
                System.gc(); //Makes the times a loooooot more normalized

                QuickHullPara qHullPara = new QuickHullPara(n[i],threadCount, false); //Change threadCount to 1 if you want to run sequentially
                double tidPara = qHullPara.findHull();
                tidPara = Math.floor(tidPara * 100) / 100;
                System.gc();

                double speedUp = tidSekv / tidPara;
                speedUp = Math.floor(speedUp * 100) / 100;

                if(qHullSekv.hull.size() != qHullPara.hull.size()){
                    System.out.println("HULL ERROR - Sekv.size() = " + qHullSekv.hull.size() + " - Para.size() = " + qHullPara.hull.size());
                }

                System.out.println(n[i] + "\t" + tidSekv + "ms\t" + tidPara + "ms\t" + speedUp);
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
    IntList hull;

    QuickHullPara(int n, int threadCount, boolean draw){
        this.n = n;
        x = new int[n];
        y = new int[n];
        p = new NPunkter(n);
        p.fyllArrayer(x, y);
        this.threadCount = threadCount;
        this.draw = draw;
        this.hull = new IntList();
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

    double findHull(){
        long t = System.nanoTime(); //Starts clock

        //Find smallest and largest points on the X axis
        int minP = -1, maxP = -1;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;

        for(int i = 0; i < x.length; i++){
            if(x[i] < minX){
                if(minP == -1){
                    minX = x[i];
                    minP = i;
                }
                else if(y[minP] >= y[i]){
                    minX = x[i];
                    minP = i;
                }
            }
            else if(x[i] > maxX) {
                if (maxP == -1) {
                    maxX = x[i];
                    maxP = i;
                } else if (y[maxP] <= y[i]) {
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



        if(threadCount == 1){ //1 core on CPU
            hull.add(minP);
            HullSet hullSet = new HullSet(hull, rightPoints, minP, maxP, 0, new IntList());
            hullSet.run();
            hull.add(maxP);
            hullSet = new HullSet(hull, leftPoints, maxP, minP, 0, new IntList());
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
            level--;
            IntList hull1 = new IntList();
            HullSet hullSet1 = new HullSet(hull1, rightPoints, minP, maxP, level, new IntList());
            hullSet1.start();
            IntList hull2 = new IntList();
            HullSet hullSet2 = new HullSet(hull2, leftPoints, maxP, minP, level, new IntList());
            hullSet2.start();

            try{ //Waits for them to finish, then adds the hulls together
                hullSet1.join();
                for(int i = 0; i < hullSet1.hull.size(); i++){
                    hull.add(hullSet1.hull.get(i));
                }
                hull.add(maxP);
                hullSet2.join();
                for(int i = 0; i < hullSet2.hull.size(); i++){
                    hull.add(hullSet2.hull.get(i));
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        double time = (System.nanoTime() - t) / 1000000.0; //Ends clock

        if(draw){ //Draws the graph if constructor argument is true
            Oblig4 o = new Oblig4(x, y, n, hull, p);
            o.draw();
        }
        return time;

        /* Prints out the points - Doesn't make sense doing this when taking the time
        for(int i = 0; i < hull.size(); i++){
            System.out.println("P: " + hull.get(i) + " - X: " + x[hull.get(i)] + " - Y: " + y[hull.get(i)]);
        }
        System.out.println("HULL SIZE: " + hull.size());
        */
    }


    public int distance(int a, int b, int i){ //Finds the distance from the vector to the point
        return Math.abs((x[b] - x[a]) * (y[a] - y[i]) - (y[b] - y[a]) * (x[a] - x[i]));
    }

    private class HullSet extends Thread{
        int level;
        IntList hull, pointSet, onPoint, onPointLeft;
        int a, b;

        HullSet(IntList hull, IntList pointSet, int a, int b, int level, IntList onPoint){
            this.hull = hull;
            this.pointSet = pointSet;
            this.a = a;
            this.b = b;
            this.level = level;
            this.onPoint = onPoint;
            this.onPointLeft = new IntList();
        }

        @Override
        public void run(){
            //No more points - Exit
            if(pointSet.size() == 0){
                return;
            }

            //Clears the onPoints, as they aren't on the hull.
            onPoint.clear();
            onPointLeft.clear();



            //Find point furthest away
            int dist = -1, furthestPoint = -1;
            for(int i = 0; i < pointSet.size(); i++){
                int tempDist = distance(a, b, pointSet.get(i));
                if(tempDist > dist){
                    dist = tempDist;
                    furthestPoint = pointSet.get(i);
                }
            }

            IntList rightPoints = new IntList();
            IntList leftPoints = new IntList();

            //Finds out which side the points are on (or on the line)
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

                else if((sideInt == 0) && (point != a && point != b && point != furthestPoint)){
                    onPoint.add(point);
                }
                else if((sideInt2 == 0) && (point != a && point != b && point != furthestPoint)){
                    onPointLeft.add(point);
                }
            }


            if(level < 1){//Sequential
                HullSet hullSet = new HullSet(hull, rightPoints, a, furthestPoint, level, onPoint);
                hullSet.run();
                addSortedToHull(hull, onPoint);
                hull.add(furthestPoint);
                hullSet = new HullSet(hull, leftPoints, furthestPoint,b, level, onPointLeft);
                hullSet.run();
                addSortedToHull(hull, onPointLeft);
            }
            else{ //Parallell
                level--; //We go down a level
                IntList hull1 = new IntList(); //Two new hull sets, one for each side.
                HullSet hullSet1 = new HullSet(hull1, rightPoints, a, furthestPoint, level, onPoint);
                hullSet1.start();
                IntList hull2 = new IntList();
                HullSet hullSet2 = new HullSet(hull2, leftPoints, furthestPoint,b, level, onPointLeft);
                hullSet2.run(); //I don't make a new thread here, this nodes thread goes down to the other set instead of waiting for 2 threads to finish.

                try{
                    hullSet1.join();
                    addSortedToHull(hull, onPoint);
                    for(int i = 0; i < hullSet1.hull.size(); i++){
                        hull.add(hullSet1.hull.get(i));
                    }
                    hull.add(furthestPoint);

                    for(int i = 0; i < hullSet2.hull.size(); i++){
                        hull.add(hullSet2.hull.get(i));
                    }
                    addSortedToHull(hull, onPointLeft);

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }

        void addSortedToHull(IntList hull, IntList onPoint){
            if(onPoint.size() == 1){
                hull.add(onPoint.get(0));
            }else if(onPoint.size() > 1){
                //Sorts the points in a sorted container
                TreeMap<Double,Integer> sorted = new TreeMap<Double,Integer>();
                for(int i = 0; i < onPoint.size(); i++){
                    int point = onPoint.get(i);
                    double distance = Math.sqrt(Math.abs((double)(x[point] - x[a]) + (y[point] - y[a])));
                    sorted.put(distance, point);
                }
                //Adds them sorted to the hull
                for(Map.Entry<Double,Integer> entry : sorted.entrySet()) {
                    hull.add(entry.getValue());
                }
            }
            onPoint.clear();
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