import java.util.Random;

/**
 * Class NPunkter for aa finne n tilfeldige, ulike  punkter i x,y-planet
 * ver 7.mai 2015
 ************************************************************************/
public class NPunkter {
    Random r;
    int n;
    byte [] bitArr;
    static int maxXY, xShift =3;
    int scaleFactor = 3;  // scaleFactor * scaleFactor * n= antall mulige punkter i planet (her: 4*n)
    final  int [] bitMask ={1,2,4,8,16,32,64,128};

    NPunkter(int n) {
        this.n =n;
        maxXY = Math.max(10,(int) Math.sqrt(n) * scaleFactor); // st�rste X og Y verdi
        while ((1<<xShift) < maxXY) xShift++;
        xShift = xShift - 3;                    // 8 bits per byte
        bitArr = new byte[(maxXY<<xShift |(maxXY>>3))  + 1];
        r = new Random(123);
    }

    private void setUsed(int x, int y) {
        bitArr[(x<<xShift) | (y >>3)] |= bitMask[(y&7)];
    }

    private boolean used (int x, int y) {
        return  (bitArr[(x<<xShift) | (y >>3)] & bitMask[y&7]) != 0;
    }

    public void fyllArrayer(int [] x, int[] y) {
        int next =0;
        int xval, yval;
        while (next < n) {
            do{
                xval = r.nextInt(maxXY)+1;
                yval = r.nextInt(maxXY)+1;
            } while (used (xval, yval));
            x[next] = xval;
            y[next] = yval;
            setUsed (xval,yval);
            next++;
        } // next point
    }// end fyllArrayer

} // end class NPunkter