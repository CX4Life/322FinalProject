import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
 */
public class main {
    public static final int FLOORS = 10;
    public static void main(String[] args) {
        
    }
}

class floor{
    private List<passenger> waiting = new ArrayList<>();
    private int level;
    private boolean hasCall = false;

    public floor(int level){
        this.level = level;
    }

    public void addPass(passenger toAdd){
        assert toAdd.getCurrentFloor() == level;
        waiting.add(toAdd);
        hasCall = true;
    }

    public void getOn(elevator e){
        for (passenger p : waiting){
            e.addPass(p);
        }
        hasCall = false;
    }

    public boolean hot(){return this.hasCall;}

    public int getLevel() {
        return level;
    }
}

class elevator{
    private List<passenger> p = new ArrayList<>();
    private int floor;
    private boolean[] calls = new boolean[main.FLOORS];
    private int id;
    private final int MAX_LOAD = 5;
    private int delivered;

    public elevator(int id){
        this.id = id;
        this.floor = 0;
    }

    public void receiveCall(floor f){
        this.calls[f.getLevel()] = true;
    }

    public void addPass(passenger pass){
        p.add(pass);
    }

    public int startElevator(Collection<floor> building){
        int count = 0;
        while(count < 10000){
            for (floor f : building){
                if (f.hot()){
                    calls[f.getLevel()] = true;
                }
            }
            if (!p.isEmpty()){

            }
        }
        return delivered;
    }
}

class passenger{
    private int currentFloor;
    private int destFloor;
    private boolean arrived = false;

    public passenger(int start, int dest){
        this.currentFloor = start;
        this.destFloor = dest;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public boolean setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
        if (this.currentFloor == this.destFloor){
            this.arrived = true;
        }
        return this.arrived;
    }
}

// Changes to be added to local copy on "Partionator"

//within elevator.move
if (this.goingUp){
    if(this.floor < Math.max(maxCall, maxDest)){
        this.floor++;
        cost += 2;
    } else {
        goingUp = false;
    }
} else {
    if(this.floor > Math.min(minCall, minDest)){
        this.floor--;
        cost += 2;
    } else {
        goingUp = true;
    }
}
// change passenger constructor
public passenger(int start, int dest){
    this.currentFloor = start;
    if (dest != start){
        this.destFloor = dest;
    } else {
        this.destFloor = ((4 + dest) % 10);
    }
    if(currentFloor < destFloor){
        this.goingUp = true;
    } else {
        this.goingUp = false;
    }
}
//getter for goingUp
public boolean goingUp(){
    return this.goingUp;}

//change getOn in floor
public void getOn(elevator e){
    for (passenger p : waiting){
        if(e.goingUp()){
            if (p.goingUp() && !e.isFull()) e.add(p);
            waiting.remove(p);
        } else {
            if (!p.goingUp() && !e.isFull()) e.add(p);
            waiting.remove(p);
        }
    }
}

// how to organize floors?
List<floors> building = new ArrayList<>();
for (int i = 0; i < FLOORS; i++){
    floor f = new floor(i);
    building.add(f);
    assert building.get(i) = f; // make sure indexed as expected
}


// generate calls
int totalCost = 0;
while (totalCost < 10000){
    totalCost += e1.move(building);
    totalCost += e2.move(building);
    int shouldMakeCall = rand.nextInt(20);
    if (shouldMakeCall < 10){
        passenger p = new passenger(shouldMakeCall, rand.nextInt(FLOORS));
        building.get(p.getFloor()).add(p); // this might work if building is iteratable
    }
}
int totalDelivered = 0;
totalDelivered += e1.getDelivered();
totalDelivered += e1.getDelivered();
System.out.println("Total passengers: " + totalDelivered);
    
