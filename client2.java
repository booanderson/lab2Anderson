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
import java.rmi.registry.*;
import java.util.Scanner;

public class client2 {
    public static void main(String[] args) {
        try {
            //connecting to the BSSmanager to run client process
            BSSManagerInterface bssManager = (BSSManagerInterface) Naming.lookup("rmi://localhost/BSSManager");

            //creating the process to go to BSSManager server
            //determining process ID from "java client2 ' '"
	    int processId = Integer.parseInt(args[0]); 
            //actually creating the process
	    ProcessInterface process = new ProcessImpl(processId, 3, bssManager);
            Naming.rebind("rmi://localhost/Process" + processId, process);

            System.out.println("Process " + processId + " is running...");

            //getting the message information from user
            Scanner scanner = new Scanner(System.in);
	    //loop to keep process running until intentionally terminated. Loop will continue to get messages from user. 
            while (true) {
                System.out.println("Enter a message to send:");
                String message = scanner.nextLine();
                process.send(message);
            }

	    //if there were any exceptions during process execution
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

