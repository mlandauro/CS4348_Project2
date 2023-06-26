/*
Micaela Landauro
MXL190030
CS 4348.002
 */

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class PostOffice {
    Semaphore max_capacity, stations, coord, mutex2;
    Semaphore cust_ready, leave_station, scale;
    Semaphore [] finished;
    Thread [] custThreads;
    Thread [] workThreads;
    private int limit = 0;
    int i;
    Queue<Integer> q = new LinkedList<>();//stores customer ids
    Queue<Integer> jobs = new LinkedList<>();//stores customer jobs

    public PostOffice(int limit){
        this.limit = limit;
        custThreads = new Thread[limit];//keep track of all customer threads
        workThreads = new Thread[3];//keep track of worker threads

        //initialize semaphores
        max_capacity = new Semaphore(10);//only 10 customers allowed in shop at a time
        stations = new Semaphore(3);// only three stations working
        coord = new Semaphore(3);
        mutex2 = new Semaphore(1);
        cust_ready = new Semaphore(0);
        leave_station = new Semaphore(0);
        scale = new Semaphore(1);
        finished = new Semaphore[limit];

        //initialize all finished semaphores as 0, signal will make them 1
        for(i = 0; i < limit; i++)
            finished[i] = new Semaphore(0);


        //create worker threads >> 3
        for(i = 0; i < 3; i++){
            workThreads[i] = new Thread(new workerThread(this, i));//give worker thread post office and unique id
        }

        //create customer threads >> limit
        for(i = 0; i < limit; i++){
            custThreads[i] = new Thread(new customerThread(this, i));//give worker thread post office and unique id
            custThreads[i].start();
        }

        for(i = 0; i < 3; i++){workThreads[i].start();}

        //JOIN THREADS
        for(i = 0; i < limit; i++){
            try {
                custThreads[i].join();
            } catch (InterruptedException e){}

            System.out.println("Joined customer " + i);
        }
        for(i = 0; i < 3; i++){
            try {
                workThreads[i].join();
            } catch (InterruptedException e){}
            System.out.println("Joined Postal worker " + i);
        }
    }

    public void customer(int num, int job){
        //wait(max_capacity)
        try{
            max_capacity.acquire();
        } catch (InterruptedException e){}
        //enterShop()
        System.out.println("Customer " + num + " enters post office");
        //wait(stations)
        try{
            stations.acquire();
        } catch (InterruptedException e) {}
        //wait(mutex2)
        try {
            mutex2.acquire();
        } catch (InterruptedException e) {}
        q.add(num);
        jobs.add(job);
        cust_ready.release();
        mutex2.release();
        //wait(finished[num])
        try{
            finished[num].acquire();
        } catch (InterruptedException e){}
        System.out.println("Customer " + num + " finished " + getTaskCustomer(job));
        leave_station.release();
        System.out.println("Customer " + num + " leaves post office");
        //signal(max_capacity)
        max_capacity.release();

    }

    public void worker(int num){
        int custNum;
        int job;
        String task;

        //continue as long as there are customer threads available
        while(!q.isEmpty()) {
            //wait(cust_ready)
            try {
                cust_ready.acquire();
            } catch (InterruptedException e) {}
            //wait(mutex2)
            try {
                mutex2.acquire();
            } catch (InterruptedException e) {}
            //get customer job, id, and get job as a string
            custNum = q.remove();
            job = jobs.remove();
            task = getTask(job);
            mutex2.release();
            System.out.println("Postal worker " + num + " serving customer " + custNum);
            System.out.println("Customer " + custNum + " asks postal worker " + num + " to " + task);
            //wait(coord)
            try{
                coord.acquire();
            } catch (InterruptedException e) {}

            //execute proper job
            if(job == 0){//buy stamps
                try{
                    Thread.sleep(1000); // 60 sec
                } catch (InterruptedException e){}
            } else if(job == 1){//mail a letter
                try{
                    Thread.sleep(1500); //90 sec
                } catch (InterruptedException e){}
            } else if(job == 2){//mail a package
                //try and use scale, only one
                try {
                    scale.acquire();
                } catch (InterruptedException e){}
                System.out.println("Scales in use by postal worker " + num);
                try{
                    Thread.sleep(2000); //120 sec
                } catch (InterruptedException e){}
                System.out.println("Scales released by postal worker " + num);
                //release scale so other worker can use
                scale.release();
            }
            coord.release();
            //signal(finished[custNum])
            finished[custNum].release();
            //wait(leave_station)
            try{
                leave_station.acquire();
            } catch (InterruptedException e) {}
            stations.release();
            leave_station.release();
        }
    }
    //make given task a string for printing >> for worker output
    public String getTask(int job){
        if(job == 0)
            return "buy stamps";
        else if(job == 1)
            return  "mail a letter";
        else if(job == 2)
            return "mail a package";
        else
            return "ERROR invalid job";
    }
    //same as above but -ing to the end of task >> for customer output
    public String getTaskCustomer(int job){
        if(job == 0)
            return "buying stamps";
        else if(job == 1)
            return  "mailing a letter";
        else if(job == 2)
            return "mailing a package";
        else
            return "ERROR invalid job";
    }

    public static void main(String [] args){
        PostOffice post = new PostOffice(50);
    } //creating 50 worker threads
}

class customerThread implements Runnable{
    int customerNum;
    int job = 0; //need to assign random job
    PostOffice post;//all in same Post Office
    Random rand = new Random();

    public customerThread(PostOffice post, int customerNum){
        this.post = post;
        this.customerNum = customerNum;
        job = rand.nextInt(3);//assigning random job
        System.out.println("Customer " + customerNum + " created");
    }

    public void run(){
        post.customer(customerNum ,job);//to customer logic
    }

}

class workerThread implements Runnable{
    int workNum;
    PostOffice post;

    public workerThread(PostOffice post, int workNum){
        this.post = post;
        this.workNum = workNum;
        System.out.println("Postal worker " + workNum + " created");
    }

    public void run(){
        post.worker(workNum);//to worker logic
    }

}
