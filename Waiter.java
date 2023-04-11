import java.util.concurrent.Semaphore;

class WaiterThread extends Thread {
    private int id;
    private Semaphore[] tablSemaphores;
    private Semaphore kitchenSemaphore;
    private Semaphore[] waitSemaphores;

    public WaiterThread(int id, Semaphore[] tablSemaphores, Semaphore kitchenSemaphore, Semaphore[] waitSemaphores) {
        this.id = id;
        this.tablSemaphores = tablSemaphores;
        this.kitchenSemaphore = kitchenSemaphore;
        this.waitSemaphores = waitSemaphores;
    }

    public void run() {
        try {
            // choose a waiter to wait on
            int tableChoice = (int) (Math.random() * 3);

            // Wait for a customer from this table to call the waiter
            waitSemaphores[tableChoice].acquire();

            // go to the customer and inform them that the waiter is ready to take the order
            System.out.println("Waiter " + id + " is taking orders at Table " + (char) ('A' + tableChoice));

            // get the customer ID
            int customerID = tablSemaphores[tableChoice].availablePermits() - 1;

            // go to the kitchen and deliver the order
            kitchenSemaphore.acquire();
            System.out.println("Waiter" + id + " is delivering the order for Customer " + customerID);
            Thread.sleep((int) (Math.random() * 401 + 100));
            kitchenSemaphore.release();

            // wait for the order to be ready
            Thread.sleep((int) (Math.random() * 701 + 300));

            // go to the kitchen and get the order
            kitchenSemaphore.acquire();
            System.out.println("Waiter " + id + " is picking up the order for Customer " + customerID);
            Thread.sleep((int) (Math.random() * 401 + 100));
            kitchenSemaphore.release();

            // bring the order to the customer
            System.out.println("Waiter " + id + " is serving the order to Customer " + customerID);

            // Signal the customer that the order is ready
            tablSemaphores[tableChoice].release();

            // wait for the next customer
            waitSemaphores[tableChoice].acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class CustomerThread extends Thread {
    private int id;
    private Semaphore[] tableSemaphores;
    private Semaphore[] waiterSemaphores;

    public CustomerThread(int id, Semaphore[] tableSemaphores, Semaphore[] waiterSemaphores) {
        this.id = id;
        this.tableSemaphores = tableSemaphores;
        this.waiterSemaphores = waiterSemaphores;
    }

    int[] lineCounts = new int[3];

    public void run() {
        try {
            // Choose a table to sit at
            int tableChoice = (int) (Math.random() * 3);
            int backupChoice = (tableChoice + 1) % 3;

            // Check if the first choice's line is too long
            int firstChoiceLineCount = 4 - tableSemaphores[tableChoice].availablePermits();
            if (firstChoiceLineCount >= 7) {
                // Check if the backup choice's line is not too long
                int backupChoiceLineCount = 4 - tableSemaphores[backupChoice].availablePermits();
                if (backupChoiceLineCount < 7) {
                    tableChoice = backupChoice;
                    lineCounts[tableChoice]++;
                }
            } else {
                lineCounts[tableChoice]++;
            }

            // Wait in line for the chosen table
            tableSemaphores[tableChoice].acquire();

            // Sit at the table and call the waiter
            System.out.println("Customer " + id + " is sitting at Table " + (char) ('A' + tableChoice));
            waiterSemaphores[tableChoice].release();

            // Wait for an empty seat
            int seat = tableSemaphores[tableChoice].availablePermits();
            System.out
                    .println("Customer " + id + " is waiting for an empty seat at Table " + (char) ('A' + tableChoice));
            tableSemaphores[tableChoice].acquire();
            lineCounts[tableChoice]--;

            // Sit at the empty seat
            System.out.println(
                    "Customer " + id + " is sitting at seat " + seat + " at Table " + (char) ('A' + tableChoice));

            // Call the waiter and wait for the order
            waiterSemaphores[tableChoice].acquire();
            System.out.println("Customer " + id + " is giving the order to Waiter " + tableChoice);

            // Wait for the order to be ready
            Thread.sleep((int) (Math.random() * 1501 + 500));

            // Eat and leave the table
            System.out.println("Customer " + id + " has finished eating at Table " + (char) ('A' + tableChoice));
            tableSemaphores[tableChoice].release();

            // Pay the bill
            System.out.println("Customer " + id + " is paying the bill");

            // Leave the restaurant
            System.out.println("Customer " + id + " is leaving the restaurant");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class Waiter {
    public static void main(String[] args) {
        // initialize semaphore
        Semaphore[] tableSemaphores = new Semaphore[3];
        Semaphore kitcSemaphore = new Semaphore(1);
        Semaphore[] waiterSemaphore = new Semaphore[3];
        for (int i = 0; i < 3; i++) {
            tableSemaphores[i] = new Semaphore(4);
            waiterSemaphore[i] = new Semaphore(0);
        }

        // Create and start the waiter threads
        WaiterThread[] waiterThreads = new WaiterThread[3];
        for (int i = 0; i < 3; i++) {
            waiterThreads[i] = new WaiterThread(i, tableSemaphores, kitcSemaphore, waiterSemaphore);
            waiterThreads[i].start();
        }

        // Create the customers
        CustomerThread[] customers = new CustomerThread[12];
        for (int i = 0; i < 12; i++) {
            customers[i] = new CustomerThread(i, tableSemaphores, waiterSemaphore);
            customers[i].start();
        }

    }
}
