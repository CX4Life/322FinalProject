import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/* Tim Woods
 * CS322 Winter 2017
 *
 * Final Project
 * The purpose of this program is to model the behavior of an elevator
 * control system using principles of concurrent programming. The initial
 * version will not rely on concurrency and use a "naive" approach to
 * dispatching elevators to pick up passengers, while the concurrent method
 * will rely heavily on message passing and mutex
 *
 * While the runtime for the concurrent method may very well be reduced,
 * the intended outcome is not related to runtime, but to the efficient
 * operation of the elevators, and success will be measured as a factor
 * of completed trips per some unit of simulated time.
 *
 * This is very much a working draft, with very long methods which potentially
 * hamper the clarity of this code. Further work could be done to simplify and
 * clarify the code base.
 */
public class nonConcurrentElevators {
    static final int FLOORS = 100;
    private static final int MAX_PASS = 70;

    public static void main(String[] args) {
        int elevatorCapacity = 5;
        int numVaders = 3;
        // If command line arguments supplied, change the default values for FLOORS and elevator size.
        if(args.length > 0 ){
            numVaders = Integer.parseInt(args[0]);
            if(args.length > 1){
                elevatorCapacity = Integer.parseInt(args[1]);
            }
        }
        System.out.println("Builiding starting with " + FLOORS +" floors and " + MAX_PASS + " existing passengers");
        List<floor> floors = new ArrayList<>();
        int allPass = MAX_PASS;
        for(int i = 0; i < FLOORS; i++){ // create our floors
            floor temp = new floor(i);
            floors.add(temp);
        }

        // create all the elevators ... this sure could be prettier...
        Collection<elevator> vaders = new ArrayList<>();
        for(int p = 0; p < numVaders; p++){
            elevator e = new elevator(elevatorCapacity);
            vaders.add(e);
        }
        System.out.println("Created " + vaders.size() + " elevators, which each can hold " + elevatorCapacity + " passengers");

        // seed the floors with passengers who want to go somewhere
        Random rand = new Random();
        for(int j = 0; j < MAX_PASS; j++){
            passenger temp = new passenger(rand.nextInt(FLOORS), rand.nextInt(FLOORS));
            for(floor f : floors){
                f.addPass(temp); // this is wicked slow...
            }
        }

        int totalCost = 0;
        System.out.print("Elevators running");
        while (totalCost < 100000000){
            int i = totalCost % 10000000;
            if (16 > i) System.out.print(".");
            for(elevator vad : vaders){
                totalCost += vad.move(floors);
            }
            int shouldMakeCall = rand.nextInt(5*FLOORS);
            if (shouldMakeCall < FLOORS){ // keep making calls, but not all the time
                passenger p = new passenger(shouldMakeCall, rand.nextInt(FLOORS));
                floors.get(p.getStartFloor()).addPass(p);
                allPass++;
            }
        }
        System.out.println();
        int totalDelivered = 0;
        for(elevator vad : vaders){
            totalDelivered += vad.getDelivered();
        }
        double per = ((double)totalDelivered / (double) allPass) * 100.0;

        System.out.print("Total passengers delivered: " + totalDelivered + " out of " +allPass + " possible or ");
        System.out.printf("%.2f%%\n", per);
    }
}

class floor{
    // organizes passengers waiting for elevators
    // and tells elevators if someone has pressed the
    // "up" or "down" button on that floor
    private LinkedList<passenger> waiting = new LinkedList<>();
    private int level;
    private boolean UpCall = false;
    private boolean DownCall = false;

    floor(int level){
        this.level = level;
    }

    void addPass(passenger toAdd){
        if (toAdd.getStartFloor() == level) {
            waiting.add(toAdd);
            if(toAdd.isGoingUp()){
                this.UpCall = true;
            } else {
                this.DownCall = true;
            }
        }
    }

    void getOn(elevator e){
        assert e.onSame(level);
        assert !waiting.isEmpty();
        Iterator<passenger> pass = waiting.iterator();
        while (pass.hasNext()){
            passenger p = pass.next();
            if(e.isGoingUp()){
                if (p.isGoingUp() && !e.isFull()) e.addPass(p);
                pass.remove();
            } else {
                if (!p.isGoingUp() && !e.isFull()) e.addPass(p);
                pass.remove();
            }
        }
        boolean up = false;
        boolean down = false;
        Iterator<passenger> pass2 = waiting.iterator();
        while (pass2.hasNext()){
            passenger p = pass2.next();
            if (p.isGoingUp()){
                up = true;
            } else {
                down = true;
            }
        }
        this.UpCall = up;
        this.DownCall = down;
    }

    boolean hot(){return (this.UpCall || this.DownCall);} // are there any calls on this floor?

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

class elevator{
    // Organizes most of the logic for this
    // model. Contains passengers, an updated
    // copy of all the floors in the building
    // and other parameters relevant to
    // its operation
    private Collection<passenger> passengers = new LinkedList<>();
    private int floor;
    private boolean goingUp = true;
    private Set<Integer> dests = new HashSet<>();
    private int capacity;
    private int delivered;

    public elevator(int capacity){
        this.floor = 0;
        this.capacity = capacity;
    }

    void addPass(passenger pass){
        passengers.add(pass);
        dests.add(pass.getDestFloor());
    }

    int move(List<floor> b){ // this is a monstrosity...
        int cost = 0;
        int maxCall = -1;
        int minCall = nonConcurrentElevators.FLOORS + 1;
        for (floor f : b){ // Get any new calls
            if (f.hot()){
                int i = f.getLevel();
                if(i > maxCall) maxCall = i; // set max and min calls
                if(i < minCall) minCall = i;
            }
        }
        floor currentFloor = b.get(this.floor);
        if(currentFloor.hasUpCall() && this.isGoingUp()){ // Gets any passengers wanting to get on
            currentFloor.getOn(this);
            cost += 4;
        } else if (currentFloor.hasDownCall() && !this.isGoingUp()){
            currentFloor.getOn(this);
            cost += 4;
        }
        if (dests.contains(this.floor)){ // Lets any passengers out
            Iterator<passenger> pass = passengers.iterator();
            while(pass.hasNext()){
                passenger p = pass.next();
                if(p.getDestFloor() ==  this.floor){
                    pass.remove();
                    delivered += 1;
                }
            }
            cost += 3;
            dests.remove(this.floor);
        }
        int maxDest = -1;
        int minDest = nonConcurrentElevators.FLOORS + 1;
        if (!dests.isEmpty()){ // get the max and min destinations
            for (Integer i : dests){
                if (i > maxDest) maxDest = i;
            }
            for (Integer j : dests){
                if (j < minDest) minDest = j;
            }
        }
        if((maxCall < 0 && maxDest < 0) ||                          // if no calls and no passengers
                (minCall > nonConcurrentElevators.FLOORS &&         // in practice, this doesn't occur
                        maxDest > nonConcurrentElevators.FLOORS)) { // unless very few calls are placed
            return cost;
        } else { // move!
            if (this.goingUp){
                if(this.floor < Math.max(maxCall, maxDest)){
                    this.floor++;
                    cost += 2;
                } else {
                    goingUp = false;
                    cost += 1;
                    //System.out.println("Elevator " + this.id + " going down");
                }
            } else {
                if(this.floor > Math.min(minCall, minDest)){
                    this.floor--;
                    cost += 2;
                } else {
                    goingUp = true;
                    cost += 1;
                    //System.out.println("Elevator " + this.id + " going up");
                }
            }
        }
        return cost;
    }// end move

    boolean isGoingUp() {
        return goingUp;
    }
    boolean isFull(){
        return (this.passengers.size() == capacity);
    }
    boolean onSame(int floor) {
        return (this.floor == floor);
    }
    int getDelivered() {
        return delivered;
    }
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