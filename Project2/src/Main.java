import java.util.Random;
import java.util.concurrent.Semaphore;

import static java.lang.Thread.sleep;

public class Main
{
    public static void main(String[] args)
    {
        Setup run = new Setup();
        run.startSetup();
    }

    public static class Setup
    {
        public void startSetup()
        {
            Office office = new Office(3,50);
        }

        public static class Office
        {
            Semaphore waiting;
            Semaphore finished;

            Semaphore workerReady;
            Semaphore[] workerFreeList;
            Semaphore[] workerStartList;
            Semaphore workerGate;

            Semaphore scale;

            Job[] jobList;
            final int[] rest = new int[]{1000, 1500, 2000};

            public Office(int w, int c)
            {
                waiting = new Semaphore(10, true);
                finished = new Semaphore(c, true);
                workerReady = new Semaphore(w, true);
                workerFreeList = new Semaphore[w];
                workerStartList = new Semaphore[w];
                workerGate = new Semaphore(1, true);
                scale = new Semaphore(1, true);
                jobList = new Job[w];
                Worker[] workers = new Worker[w];
                Customer[] customers = new Customer[c];

                System.out.println("Simulating Post Office with " + c + " customers and " + w + " postal workers");

                for(int i = 0; i < w; i++)
                {
                    workerFreeList[i] = new Semaphore(1, true);
                }

                for(int i = 0; i < w; i++)
                {
                    workerStartList[i] = new Semaphore(0, true);
                }

                for(int i = 0; i < w; i++)
                {
                    workers[i] = new Worker(i);
                    workers[i].start();
                }

                for(int i = 0; i < c; i++)
                {
                    customers[i] = new Customer(i);
                    customers[i].start();
                }

                while(finished.availablePermits() != 0){}

                for(int i = 0; i < w; i++)
                {
                    workers[i].interrupt();
                }

                for(int i = 0; i < c; i++)
                {
                    if(customers[i].isAlive())
                    {
                        customers[i].interrupt();
                    }
                }

            }

            class Job
            {
                final int id;
                final int jobID;

                Job(int i, int jI)
                {
                    id = i;
                    jobID = jI;
                }

                public int getId()
                {
                    return id;
                }

                public int getJobID()
                {
                    return jobID;
                }
            }

            class Customer extends Thread
            {
                final int id;

                Customer(int id)
                {
                    this.id = id;
                }

                public void run()
                {
                    try
                    {
                        //if space in office have one customer enter waiting
                        System.out.println("Customer " + id + " created");
                        waiting.acquire();
                        System.out.println("Customer " + id + " enters postal office");

                        //grab worker or be disabled until one is available
                        workerReady.acquire();

                        //which worker free?
                        workerGate.acquire();
                        int workerIdIndex = whichWorkerFree();
                        workerFreeList[workerIdIndex].acquire();
                        workerGate.release();

                        ///random action for customer
                        Random rand = new Random();
                        int random = rand.nextInt(3);
                        jobList[workerIdIndex] = new Job(id, random);

                        //Back and forth communication
                        workerStartList[workerIdIndex].release();
                        workerFreeList[workerIdIndex].acquire();
                        switch (random)
                        {
                            case 0 ->
                                    System.out.println("Customer " + id + " asks postal worker " + random + " to buy stamps");
                            case 1 ->
                                    System.out.println("Customer " + id + " asks postal worker " + random + " to mail a letter");
                            case 2 ->
                                    System.out.println("Customer " + id + " asks postal worker " + random + " to mail a package");
                        }

                        workerStartList[random].release();
                        workerFreeList[random].acquire();

                        //after last signal finish post and end crit state
                        workerFreeList[random].release();
                        workerReady.release();

                        switch(random)
                        {
                            case 0 ->
                                    System.out.println("Customer " + id + " finished buying stamps");
                            case 1 ->
                                    System.out.println("Customer " + id + " finished mailing a letter");
                            case 2 ->
                                    System.out.println("Customer " + id + " finished mailing a package");
                        }

                        waiting.release();
                        System.out.println("Customer " + id + " leaves post office");

                        //flag customer as finished
                        finished.acquire();
                        System.out.println("Joined customer " + id);

                        //join at the end after termination
                        Thread.currentThread().join();
                    }
                    catch(InterruptedException e) {}
                }

                //return which worker is free
                public int whichWorkerFree() throws InterruptedException
                {
                    for(int i = 0; i < 3; i++)
                    {
                        if(workerFreeList[i].availablePermits() > 0)
                        {
                            return i;
                        }
                    }
                    //if this exists its very very bad
                    return -1;
                }
            }

            //Manage Workers
            class Worker extends Thread
            {
                final int id;

                Worker(int i)
                {
                    id = i;
                }

                public void run()
                {
                    System.out.println("Postal worker " + id + " created");
                    try
                    {
                        while(true)
                        {
                            //wait for customer to be ready
                            workerStartList[id].acquire();
                            System.out.println("Postal worker " + id + " serving customer " + jobList[id].getId());

                            //tell customer to print and return here
                            workerFreeList[id].release();
                            workerStartList[id].acquire();

                            //Scale?
                            if(jobList[id].getJobID() == 3)
                            {
                                scale.acquire();
                                System.out.println("Scales in use by postal worker " + id);

                                //wait
                                sleep(rest[2]);
                                System.out.println("scales released by postal worker " + id);
                                scale.release();
                            }
                            else
                            {
                                sleep(rest[jobList[id].getJobID()]);
                            }
                            System.out.println("Postal worker " + id + " finished serving customer " + jobList[id].getId());

                            //pass back to customer
                            workerFreeList[id].release();
                        }
                    } catch(InterruptedException e) {}
                }
            }
        }
    }
}