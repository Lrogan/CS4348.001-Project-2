import java.util.Random;
import java.util.concurrent.Semaphore;

import static java.lang.Thread.sleep;

public class Main
{
    public static void main(String[] args)
    {
        Office run = new Office(3, 15);
    }
}

class Office
{
    Semaphore workerReady;
    Semaphore customerReady;
    Semaphore scales;
    Semaphore waiting;
    Semaphore accessCustomerThreadName;
    Semaphore accessWorkerThreadName;
    Semaphore[] Finished;
    Thread[] PostalWorkers;
    Thread[] Customers;

    String customerThreadName;
    String workerThreadName;
    String[] jobs = {"buy stamps", "mail a letter", "mail a package"};

    public Office(int w, int c)
    {
        waiting = new Semaphore(10, true);
        workerReady = new Semaphore(3, true);
        customerReady = new Semaphore(0, true);
        scales = new Semaphore(1, true);
        accessCustomerThreadName = new Semaphore(1, true);
        accessWorkerThreadName = new Semaphore(1, true);

        PostalWorkers = new Thread[w];
        Customers = new Thread[c];

        customerThreadName = "";
        workerThreadName = "";
        Finished = new Semaphore[c];

        System.out.println("Simulating Post Office with " + c + " customers and " + w + " postal workers");

        //create worker threads
        for(int i = 0; i < w; i++)
        {
            PostalWorkers[i] = new Thread(this::worker, Integer.toString(i));
            PostalWorkers[i].start();
        }

        //create all customer threads with random jobs and populate Finished Semaphore[]
        Random rand = new Random();
        for(int i = 0; i < c; i++)
        {
            int jobIndex = rand.nextInt(3);
            Customers[i] = new Thread(this::customer, Integer.toString(i) + " "  + (i % 3));
            Finished[i] = new Semaphore(0);
            Customers[i].start();
        }
    }

    //Worker thread
    public void worker()
    {
        long start = System.currentTimeMillis(); //used for timing
        String custName = ""; //local variable to store global shared name

        String name = Thread.currentThread().getName(); //parsing what thread this is
        System.out.println("Postal Worker " + name + " created");

        while(true)
        {
            try
            {
                //wait for a ready customer, then signal a worker is no longer ready
                customerReady.acquire();

                accessCustomerThreadName.acquire(); //try to acquire Customer's ThreadName, then notify the Customer to read the worker ThreadName. otherwise wait for Customer Threadname to become available
                custName = customerThreadName;
                workerThreadName = name;
                accessCustomerThreadName.release();

                //parse customerThreadName and print service notif
                int custInt = Integer.parseInt(custName.substring(0,2).trim());
                int jobIndex = Integer.parseInt(String.valueOf(custName.charAt(custName.length() - 1)));
                System.out.println("Postal worker " + name + " serving customer " + custInt);

                //Handle all cases of jobs
                switch (jobIndex)
                {
                    case 0 ->
                    {
                        sleep(1000);
                    }
                    case 1 ->
                    {
                        sleep(1500);
                    }
                    case 2 ->
                    {
                        //try to get scale access or wait to get it. once access is gained, break loop and sleep for 2s. then release the scales
                        scales.acquire();
                        sleep(2000);
                        scales.release();
                    }
                }

                //print service finished notif, then notify customer that worker is done, then get ready for next customer.
                System.out.println("Postal worker " + name + " finished serving customer " + custInt);
                Finished[custInt].release();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public void customer()
    {
        long start = System.currentTimeMillis();
        String wrkName = "";

        String cName = Thread.currentThread().getName();
        int name = Integer.parseInt(cName.substring(0,2).trim());
        int jobIndex = Integer.parseInt(String.valueOf(cName.charAt(cName.length() - 1)));

        System.out.println("Customer " + name + " created");

        try
        {
            waiting.acquire();

            System.out.println("Customer " + name + " enters post office");
            customerReady.release();

            workerReady.acquire();

            accessWorkerThreadName.acquire();
            customerThreadName = cName;
            wrkName = workerThreadName;
            accessWorkerThreadName.release();

            //parse workerThreadName and print service request notif
            int wrkInt = Integer.parseInt(String.valueOf(wrkName.charAt(0)));

            System.out.println("Customer " + name + " asks postal worker " + wrkInt + " to " + jobs[jobIndex]);
            Finished[name].acquire();
            waiting.release();

            System.out.println("Customer " + name + " asks postal worker " + wrkInt + " to " + jobs[jobIndex]);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}