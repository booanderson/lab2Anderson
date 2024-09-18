/* Usage ---------------------------
 *
 * 1. Run "rmiregistry" to start the rmiregistry
 *
 * 2. Run "java BSSManager" in another terminal to start the server 
 *
 * 3. Run "java client2 0", "java client2 1", "java client2 2" on seperate terminals to run clients 
 *
 * 4. Enter messages to be sent
 */



import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;


//Interface for process remote method calls to be implimented 
interface ProcessInterface extends Remote {
    void send(String message) throws RemoteException;
    void deliver(String message, int[] vectorClock) throws RemoteException;
    int[] getVectorClock() throws RemoteException;
}


//interface for BSSManager server 
interface BSSManagerInterface extends Remote {
    void send(String message, int[] vectorClock, int processId) throws RemoteException;
}


//class impliments ProcessInteface making it a remote process 
class ProcessImpl extends UnicastRemoteObject implements ProcessInterface {
    //unique id for the process 
    private int id;
    //array for the vector clock, each entry corresponds to a client process in the system
    private int[] vectorClock;
    //creates instance variable of BSSManager to be reference below
    private BSSManagerInterface bssManager;


    protected ProcessImpl(int id, int numProcesses, BSSManagerInterface bssManager) throws RemoteException {
	//initializes new process id 
	this.id = id;
	//initializes vector clock with total number of process (assumes 3)
        this.vectorClock = new int[numProcesses];
	//individually references bssmanager reference above
        this.bssManager = bssManager;
    }


    //increments the vector clock, and sends message to the BSSManager
    public void send(String message) throws RemoteException {
        vectorClock[id]++;
        System.out.println("Process " + id + " sending message: " + message);
        bssManager.send(message, vectorClock, id);
    }


    public void deliver(String message, int[] senderVectorClock) throws RemoteException {
        //updates the local vector clock to the max of the it's own vector clock and the senders vector clock
        for (int i = 0; i < vectorClock.length; i++) {
            vectorClock[i] = Math.max(vectorClock[i], senderVectorClock[i]);
        }

	//prints the sent message from specific process. 
        System.out.println("Process " + id + " delivered message: " + message);
	//print the updated vector clock with algorithm above
        System.out.println("Updated Vector Clock: " + Arrays.toString(vectorClock));
    }
	
    //returns current vector clock 
    public int[] getVectorClock() throws RemoteException {
        return vectorClock;
    }
}

class BSSManager extends UnicastRemoteObject implements BSSManagerInterface {
    //implimenting queue to make sure deliver order is BSS casual 
    private Queue<Message> messageQueue = new LinkedList<>();
    private int numProcesses;

    //takes numProcesses parameter and sets to this numProcesses for BSSManager
    protected BSSManager(int numProcesses) throws RemoteException {
        this.numProcesses = numProcesses;
    }


    
    public synchronized void send(String message, int[] vectorClock, int processId) throws RemoteException {
        //adding message to queue when needed
	messageQueue.add(new Message(message, vectorClock, processId));
        //calling private method to deliver message from the queue where needed
	processMessages();
    }

    private void processMessages() throws RemoteException {
	//while the message quee is not empty
        while (!messageQueue.isEmpty()) {
	    //gets message from the front of the queue
            Message msg = messageQueue.poll();

            for (int i = 0; i < numProcesses; i++) {
                try {
		    //looks up rmote object by rmi registry name
                    ProcessInterface process = (ProcessInterface) Naming.lookup("rmi://localhost/Process" + i);
                    //when found, passes current vector clock and message
		    process.deliver(msg.getMessage(), msg.getVectorClock());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private class Message {
	//declaring variables
        private String message;
        private int[] vectorClock;
        private int processId;

	//sets current message, vector clock, and pid
        public Message(String message, int[] vectorClock, int processId) {
            this.message = message;
            this.vectorClock = vectorClock;
            this.processId = processId;
        }

	//getter methdo to return message
        public String getMessage() {
            return message;
        }

	//getter method to return VC
        public int[] getVectorClock() {
            return vectorClock;
        }
    }

    public static void main(String[] args) {
        try {
	    //initializing BSSManager instance assuming 3 clients
            BSSManager bssManager = new BSSManager(3); 
	    //binds bssmanager to rmiregistry to be referenced
            Naming.rebind("rmi://localhost/BSSManager", bssManager);
            System.out.println("BSSManager is running...");

	    //print exceptions with built in function
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
