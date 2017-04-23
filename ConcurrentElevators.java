import java.util.*;
import java.util.concurrent.*;

/*
 * Tim Woods
 *
 * CS322 Winter 2017
 *
 * Concurrent implementation of elevator modeling system
 * Elevators run as threads, and a coordinator is scheduled
 * after each "run" of the elevators to dispatch floor
 * calls to the elevators.
 *
 */

public class ConcurrentElevators {
    static final int FLOORS = 100;
    static final int MAX_PASS = 10;
    public static SyncedCost cost = new SyncedCost();
    public static int allpass;
    static SyncedBool cDone;
    static SyncedBool elsDone;

    public static void main(String[] args) {
        allpass = MAX_PASS;
        int elevatorCapacity = 5;
        int numElevators = 3;

        cDone = new SyncedBool(8);
        cDone.reset();
        // If command line arguments supplied, change the default values for FLOORS and elevator size.
        if(args.length > 0 ){
            numElevators = Integer.parseInt(args[0]);
            if(args.length > 1){
                elevatorCapacity = Integer.parseInt(args[1]);
            }
        }
        elsDone = new SyncedBool(numElevators);
        elsDone.reset();

        System.out.println("Builiding starting with " + FLOORS +" floors and " + MAX_PASS + " existing passengers");
        List<cFloor> floors = new ArrayList<>();
        for(int i = 0; i < FLOORS; i++){ // create our floors
            cFloor temp = new cFloor(i);
            floors.add(temp);
        }

        // create all the elevators ... this sure could be prettier...
        cElevator[] vaders = new cElevator[numElevators];
        for(int p = 0; p < numElevators; p++){
            cElevator e = new cElevator(elevatorCapacity);
            vaders[p] = e;
        }
        System.out.println("Created " + numElevators + " elevators, which each can hold " + elevatorCapacity + " passengers");
        Random rand = new Random();
        for(int j = 0; j < MAX_PASS; j++){
            passenger temp = new passenger(rand.nextInt(FLOORS), rand.nextInt(FLOORS));
            for(cFloor f : floors){
                f.addPass(temp); // this is wicked slow...
            }
        }
        coordinator c = new coordinator(vaders);
        invokeMove(vaders, floors, c);
        System.out.println();

        int totalDelivered = 0;
        for(cElevator vad : vaders){
            totalDelivered += vad.getDelivered();
        }
        double per = ((double)totalDelivered / (double) allpass) * 100.0;
        System.out.println();
        System.out.print("Total passengers delivered: " + totalDelivered + " out of " + allpass + " possible or ");
        System.out.printf("%.2f%%\n", per);
    }// end main

    public static void invokeMove(cElevator[] elevators, List<cFloor> floors, coordinator c) {
        int howMany = 0;
        for(cElevator a : elevators){
            howMany ++;
        }
        try {
            ExecutorService els = Executors.newFixedThreadPool(howMany + 1);
            for (int i = 0; i < howMany; i++){
                final int j = i;
                final int h = howMany;
                els.execute(new Runnable() {
                    @Override
                    public void run() {
                        while(cost.value() < 1000000){
                            if((cost.value() % 100000) < 15){
                                System.out.print(".");
                            }
                            while (elsDone.allTrue()) {}
                            //System.out.println("Made it here");
                                cost.add(elevators[j].move());
                                elsDone.setIndex(j, true);
                                if (elsDone.allTrue()){
                                    invokeCoord(c, floors);
                                    elsDone.reset();
                                }
                        }}
                });
            }
            els.shutdown();
            els.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e){
            System.out.println("Threading interrupted...");
        }
    }

    public static void invokeCoord(coordinator c, List<cFloor> floors){
        Random rand = new Random();
        //System.out.println("Coord");
        c.dispatch(floors);
        for(int i = 0; i < 5; i++) {
            int shouldMakeCall = rand.nextInt((int) 1.5 * FLOORS);
            if (shouldMakeCall < FLOORS) { // keep making calls, but not all the time
                passenger p = new passenger(shouldMakeCall, rand.nextInt(FLOORS));
                floors.get(p.getStartFloor()).addPass(p);
                allpass += 1;
            }
        }
    }

}// end ConcurrentElevators


class SyncedCost{
    private int cost = 0;
    public  SyncedCost(){
        cost = 0;
    }

    public synchronized void add(int toAdd){
        cost += toAdd;
    }
    public synchronized int value(){
        return cost;
    }
}

class SyncedBool{
    private boolean[] truths;
    public SyncedBool(int size){
        this.truths = new boolean[size];
    }

    synchronized public void setIndex(int i, boolean val){
        truths[i] = val;
    }

    synchronized public boolean allTrue(){
        boolean ret = true;
        for(boolean b : truths){
            if(!b) ret = false;
        }
        return ret;
    }

    synchronized public void reset(){
        for(int i = 0; i<truths.length; i++){
            truths[i] = false;
        }
    }
    synchronized public void setTrue(){
        for(int i = 0; i<truths.length; i++){
            truths[i] = true;
        }
    }
}

class coordinator {
    private cElevator[] elevators;


    public coordinator(cElevator[] elevators) {
        this.elevators = elevators;
    }

    synchronized public void giveCallToClosest(List<cFloor> floors){
        HashMap<Integer, cElevator> elsByFloor = new HashMap<>();
        for(cElevator el : elevators){
            elsByFloor.put(el.getFloor(), el);
        }

        for(cFloor cF : floors){
            if (cF.hasUpCall()){
                for(int walkdown = cF.getLevel(); walkdown >= 0; walkdown--){
                    if(elsByFloor.containsKey(walkdown) && !elsByFloor.get(walkdown).isLocked()){
                        elsByFloor.get(walkdown).addCall(cF);
                        cF.clearUp();
                        // System.out.println("Found an up ele");
                        break;
                    }
                }
            }
            if (cF.hasDownCall()){
                for(int walkup = cF.getLevel(); walkup <= ConcurrentElevators.FLOORS; walkup++){
                    if(elsByFloor.containsKey(walkup) && !elsByFloor.get(walkup).isLocked()){
                        elsByFloor.get(walkup).addCall(cF);
                        cF.clearDown();
                        // System.out.println("Found a down ele");
                        break;
                    }
                }
            }
        }
    }

    public void dispatch(List<cFloor> floors){
        giveCallToClosest(floors);
    }
}

class cFloor implements Comparable<cFloor> {
    // organizes passengers waiting for elevators
    // and tells elevators if someone has pressed the
    // "up" or "down" button on that floor
    private ConcurrentSkipListSet<passenger> goingUp = new ConcurrentSkipListSet<>();
    private ConcurrentSkipListSet<passenger> goingDown = new ConcurrentSkipListSet<>();
    private int level;
    private boolean UpCall = false;
    private boolean DownCall = false;

    cFloor(int level){
        this.level = level;
    }

    public int compareTo(cFloor other){
        return this.level - other.getLevel();
    }

    synchronized void addPass(passenger toAdd){
        if (toAdd.getStartFloor() == level) {
            if(toAdd.isGoingUp()){
                goingUp.add(toAdd);
                if(goingUp.size() == 1) this.UpCall = true;
            } else {
                goingDown.add(toAdd);
                if(goingDown.size() == 1) this.DownCall = true;
            }
        }
    }
    synchronized Iterator<passenger> getUpIter(){
        return this.goingUp.iterator();
    }
    synchronized Iterator<passenger> getDownIter(){
        return this.goingDown.iterator();
    }

    synchronized void getOn(cElevator e) { // this now checks elevator direction
        assert e.onSame(level);            // and whether or not it's full

        if(e.isGoingUp()){
            Iterator<passenger> upPass = getUpIter();
            while(!e.isFull() && upPass.hasNext()){
                passenger p = upPass.next();
                e.addPass(p);
                goingUp.remove(p);
            }
            UpCall = !goingUp.isEmpty();
        } else {
            Iterator<passenger> downPass = getDownIter();
            while(!e.isFull() && downPass.hasNext()){
                passenger p = downPass.next();
                e.addPass(p);
                goingDown.remove(p);
            }
            DownCall = !goingDown.isEmpty();
        }

    }
    // These to be used by coordinator to have elevators "claim" calls.
    void clearUp(){this.UpCall = false;}
    void clearDown(){this.DownCall = false;}
    boolean hasUpCall() {
        return UpCall;
    }
    boolean hasDownCall() {
        return DownCall;
    }

    int getLevel() {
        return level;
    }
}


class cElevator {
    private ConcurrentSkipListSet<cFloor> callQ = new ConcurrentSkipListSet<>();
    private ConcurrentSkipListSet<passenger> passengers = new ConcurrentSkipListSet<>();
    private final int MAX_CALLS = 10;
    private int capacity;
    private boolean locked = false;
    private int currentFloor;
    private boolean goingUp = true;
    private Set<Integer> dests = new HashSet<>();
    private SyncedCost delivered = new SyncedCost();

    public cElevator(int capacity) {
        this.capacity = capacity;
        this.currentFloor = 0;
    }

    void addPass(passenger pass) {
        passengers.add(pass);
        dests.add(pass.getDestFloor());
    }

    public void addCall(cFloor f) {
        if (!locked) {
            callQ.add(f);
            if (callQ.size() == MAX_CALLS) {
                locked = true;
            }
            if (f.getLevel() < this.currentFloor) {
                goingUp = false;
            } else {
                goingUp = true;
            }
        } else {
            System.out.println("Elevator was locked");
        }
    }

    public boolean isLocked(){return this.locked;}

    synchronized public int move(){
        int cost = 0;

        while(!callQ.isEmpty()){
            for(Iterator<cFloor> calls = callQ.iterator(); calls.hasNext(); ){ // get on
                cFloor f = calls.next();
                if(currentFloor == f.getLevel()){
                    f.getOn(this); // don't have to check direction
                    callQ.remove(f);
                    cost += 4;
                }
            }
            if(dests.contains(currentFloor)){ // let em out
                for (Iterator<passenger> pass = passengers.iterator(); pass.hasNext(); ){
                    passenger p = pass.next();
                    if(p.getDestFloor() ==  currentFloor){
                        passengers.remove(p);
                        delivered.add(1);
                        //System.out.println("I've delivered: " + delivered);
                    }
                }
                dests.remove(currentFloor);
                cost += 3;
                }
            // now where?
            if(goingUp){
                if(callQ.isEmpty()){ // gotten all calls
                    while(!dests.isEmpty()){ // but still have passengers
                        int minDest = ConcurrentElevators.FLOORS + 1;
                        for(Integer i : dests){
                            if (i < minDest) minDest = i;
                        }
                        cost += 2 * (minDest - currentFloor); // skip to dest floor
                        currentFloor = minDest;
                        for (Iterator<passenger> pass = passengers.iterator(); pass.hasNext(); ){
                            passenger p = pass.next();
                            if(p.getDestFloor() ==  currentFloor){
                                passengers.remove(p);
                                delivered.add(1);
                                //System.out.println("I've delivered: " + delivered);
                            }
                        }
                        dests.remove(currentFloor);
                    } // all passengers out now
                    locked = false;
                } else {
                    if(currentFloor == ConcurrentElevators.FLOORS){
                        goingUp =false;
                    } else {
                        currentFloor += 1;
                    }
                    cost += 2;
                }
            } else {
                if(callQ.isEmpty()){ // gotten all calls
                    while(!dests.isEmpty()){ // but still have passengers
                        int maxDest = -1;
                        for(Integer i : dests){
                            if (i > maxDest) maxDest = i;
                        }
                        cost += 2 * (currentFloor - maxDest); // skip to dest floor
                        currentFloor = maxDest;
                        for (Iterator<passenger> pass = passengers.iterator(); pass.hasNext(); ){
                            passenger p = pass.next();
                            if(p.getDestFloor() ==  currentFloor){
                                //System.out.println(passengers.size());
                                passengers.remove(p);
                                //System.out.println(passengers.size());

                                delivered.add(1);
                                //System.out.println(delivered.value());
                                //System.out.println("I've delivered: " + delivered);
                            }
                        }
                        dests.remove(currentFloor);
                    } // all passengers out now
                    locked = false;
                } else {
                    if (currentFloor == 0){
                        goingUp = true;
                    } else {
                        currentFloor -= 1;
                    }
                    cost += 2;
                }
            }
        }
        assert (passengers.isEmpty());
        if(dests.size() == 0) locked = false;
        return cost;
    }

    public boolean onSame(int level){
        return currentFloor == level;
    }

    public boolean isFull(){
        return capacity <= passengers.size();
    }

    public boolean isGoingUp(){return goingUp;}

    public int getDelivered(){
        return delivered.value();
    }
    public int getFloor(){return currentFloor;}
}

class passenger implements Comparable<passenger>{
    // organizes info about
    // passengers.
    private int startFloor;
    private int destFloor;
    private boolean goingUp = false;

    passenger(int start, int dest){
        this.startFloor = start;
        this.destFloor = dest;
        if (dest != start){             // arbitrarily reset dest floor if RNG set it
            this.destFloor = dest;      // to same as start
        } else {
            this.destFloor = ((4 + dest) % nonConcurrentElevators.FLOORS);
        }
        this.goingUp = (startFloor < destFloor);
    }
    int getDestFloor(){return destFloor;}
    int getStartFloor() {
        return startFloor;
    }
    public int compareTo(passenger other){return this.startFloor - other.getStartFloor();}
    boolean isGoingUp() {
        return goingUp;
    }
}
