class IntList{
    int [] data;
    int len =0;

    IntList(int len) {
        data = new int[Math.max(1,len)];
    }

    IntList() {
        data = new int[16];  // some default size
    }
    void add(int elem) {
        if (len == data.length-1) {
            int [] b = new int [data.length*2];
            System.arraycopy(data,0, b,0,data.length);
            //for (int i = 0; i < data.length; i++) b[i] = data[i];
            data =b;
        }
        data[len++] = elem;
    }// end add

    void addAt (int elem, int pos) {
        while (pos > data.length) {
            int [] b = new int [data.length*2];
            for (int i = 0; i < data.length; i++) b[i] = data[i];
            data =b;
        }
        data[pos] = elem;
    } // end addAt



    void clear(){
        len =0;
    } // end clear;

    int get (int pos){
        if (pos > len-1 ) return -1; else return data [pos];
    }//end get

    int size() {
        return len;
    }//end size
} // end class IntList