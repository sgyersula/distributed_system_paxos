package paxos;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the main class you need to implement paxos instances.
 */
public class Paxos implements PaxosRMI, Runnable{

    ReentrantLock mutex;
    String[] peers; // hostname
    int[] ports; // host port
    int me; // index into peers[]

    Registry registry;
    PaxosRMI stub;

    AtomicBoolean dead;// for testing
    AtomicBoolean unreliable;// for testing

    // Your data here
    Object value;
    int maxSeq = -1;
    HashMap<Integer,Integer> np;
    HashMap<Integer,Integer> na;
    HashMap<Integer,Object> va;
    int[] z;
    int seq;
    HashMap<Integer,retStatus> r;

    /**
     * Call the constructor to create a Paxos peer.
     * The hostnames of all the Paxos peers (including this one)
     * are in peers[]. The ports are in ports[].
     */
    public Paxos(int me, String[] peers, int[] ports){

        this.me = me;
        this.peers = peers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.dead = new AtomicBoolean(false);
        this.unreliable = new AtomicBoolean(false);

        // Your initialization code here
        this.z = new int[this.peers.length];
        Arrays.fill(this.z,-1);
        this.np = new HashMap<>();
        this.r = new HashMap<>();
        this.na = new HashMap<>();
        this.va = new HashMap<>();
        // register peers, do not modify this part
        try{
            System.setProperty("java.rmi.server.hostname", this.peers[this.me]);
            registry = LocateRegistry.createRegistry(this.ports[this.me]);
            stub = (PaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("Paxos", stub);
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    /**
     * Call() sends an RMI to the RMI handler on server with
     * arguments rmi name, request message, and server id. It
     * waits for the reply and return a response message if
     * the server responded, and return null if Call() was not
     * be able to contact the server.
     *
     * You should assume that Call() will time out and return
     * null after a while if it doesn't get a reply from the server.
     *
     * Please use Call() to send all RMIs and please don't change
     * this function.
     */
    public Response Call(String rmi, Request req, int id){
        Response callReply = null;

        PaxosRMI stub;
        try{
            Registry registry=LocateRegistry.getRegistry(this.ports[id]);
            stub=(PaxosRMI) registry.lookup("Paxos");
            if(rmi.equals("Prepare"))
                callReply = stub.Prepare(req);
            else if(rmi.equals("Accept"))
                callReply = stub.Accept(req);
            else if(rmi.equals("Decide"))
                callReply = stub.Decide(req);
            else
                System.out.println("Wrong parameters!");
        } catch(Exception e){
            return null;
        }
        return callReply;
    }


    /**
     * The application wants Paxos to start agreement on instance seq,
     * with proposed value v. Start() should start a new thread to run
     * Paxos on instance seq. Multiple instances can be run concurrently.
     *
     * Hint: You may start a thread using the runnable interface of
     * Paxos object. One Paxos object may have multiple instances, each
     * instance corresponds to one proposed value/command. Java does not
     * support passing arguments to a thread, so you may reset seq and v
     * in Paxos object before starting a new thread. There is one issue
     * that variable may change before the new thread actually reads it.
     * Test won't fail in this case.
     *
     * Start() just starts a new thread to initialize the agreement.
     * The application will call Status() to find out if/when agreement
     * is reached.
     */
    public void Start(int seq, Object value){
        this.mutex.lock();
        System.out.println("Starting"+seq);
        this.seq = seq;
        this.value = value;
        Thread thread1 = new Thread(this);
        thread1.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.mutex.unlock();
    }

    @Override
    public void run(){
        System.out.println("running");
        //this.mutex.unlock();
        int curMin = this.Min();
        //this.mutex.lock();
        Object vv = this.value;
        //this.mutex.lock();
        int curSeq = this.seq;
        this.maxSeq = Math.max(this.maxSeq,curSeq);
        System.out.println("running"+curSeq+" curMin"+curMin);
        if(curSeq < curMin) {
            //this.mutex.unlock();
            return;
        }
        long sleepTime = 10;
        while (this.Status(curSeq).state == State.Pending) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if(sleepTime < 10000) {
                sleepTime *= 2;
            }
            if(this.isDead()) {
                //this.mutex.unlock();
                return;
            }
            System.out.println("running"+curSeq);
            this.mutex.lock();
            int maxSeen = this.np.getOrDefault(curSeq,-1);
            this.mutex.unlock();
            int proposeN = (maxSeen+this.peers.length)/this.peers.length * this.peers.length + this.me;
//            int proposeN = curSeq;
//            while(proposeN < this.Max()) proposeN += this.peers.length;
            System.out.println("running"+curSeq+"proposed"+proposeN+"maxSeen"+maxSeen);
//            Object vv = this.value;
            int prepare_ok = 0;
            int highestNa = -1;
            for(int id = 0; id < ports.length; id++) {
                //this.mutex.unlock();
               Response prepare = this.Call("Prepare", new Request(curSeq,proposeN), id);
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                this.mutex.lock();
               if(prepare!=null && prepare.ack) {
                   prepare_ok++;
                   if(prepare.na != -1 && prepare.na > highestNa){
                       highestNa = prepare.na;
                       vv = prepare.value;
                   }
               }
               else if(prepare != null){
                  if(prepare.value != null) {//a deaf peer back to life, found others have decided
                      this.r.put(curSeq, new retStatus(State.Decided, prepare.value));
                      return;
                  }
               }
            }
            System.out.println(curSeq+"Prepare_ok"+ prepare_ok);
            if(prepare_ok > this.peers.length/2) {
                int accept_ok = 0;
                for(int id = 0; id < ports.length; id++) {
                    //this.mutex.unlock();
                    Response accept = this.Call("Accept", new Request(curSeq,proposeN,vv), id);
                    //this.mutex.lock();
                    if(accept!=null && accept.ack) {
                        accept_ok++;
                    }
//                    else{
//                        mutex.lock();
//                        this.maxSeq = Math.max(this.maxSeq,accept.n);
//                        mutex.unlock();
//                    }
                }
                System.out.println(curSeq+"Accept_ok"+ accept_ok);
                if(accept_ok > this.peers.length/2) {
                    for(int id = 0; id < ports.length; id++) {
                        //System.out.println("Decide"+curSeq+"send to"+id);
                        //this.mutex.unlock();
                        this.Call("Decide",new Request(curSeq,vv,this.me,this.z[this.me]),id);
                        //this.mutex.lock();
                    }
                    //this.r.put(curSeq,new retStatus(State.Decided,vv));
                }
            }
        }
        //this.mutex.unlock();
    }

    // RMI handler
    public Response Prepare(Request req){
        this.mutex.lock();
        if(req.n > this.np.getOrDefault(req.seq,-1)){
            this.np.put(req.seq,req.n);
            mutex.lock();
            this.maxSeq = Math.max(this.maxSeq,req.n);
            mutex.unlock();
            Response prepare_ok = new Response(true,req.n,this.na.getOrDefault(req.seq,-1),this.va.getOrDefault(req.seq,null));
            mutex.unlock();
            return prepare_ok;
        }
        Response prepare_reject =new Response(false,(int)this.np.getOrDefault(req.seq,-1),this.na.getOrDefault(req.seq,-1),this.va.getOrDefault(req.seq,null));
        this.mutex.unlock();
        return prepare_reject;
    }

    public Response Accept(Request req){
        mutex.lock();
        if(req.n >= this.np.getOrDefault(req.seq,-1)) {
            this.np.put(req.seq,req.n);
            this.na.put(req.seq,req.n);
            this.va.put(req.seq,req.value);
            mutex.lock();
            this.maxSeq = Math.max(this.maxSeq,req.n);
            mutex.unlock();
            mutex.unlock();
            return new Response(true,req.n);
        }
        mutex.unlock();
        return new Response(false,(int)this.np.getOrDefault(req.seq,-1));
    }

    public Response Decide(Request req){
        System.out.println(this.me+"Receive Decide from"+req.seq);
        mutex.lock();
        if(!req.isDone) {
            ////System.out.println("Receive Decide in");
        this.r.put(req.seq, new retStatus(State.Decided, req.value));
        }
        else {
            this.z[req.seq] = Math.max(this.z[req.seq],req.doneMax);
        }
        mutex.unlock();
        return null;
    }

    /**
     * The application on this machine is done with
     * all instances <= seq.
     *
     * see the comments for Min() for more explanation.
     */
    public void Done(int seq) {
//        if(seq>this.z_i) {
//            this.z_i = seq;
//            for(int i = 0;i <= seq;i++) {
////            if(this.r.containsKey(i)) {
////                this.r.get(i).state = State.Forgotten;
////                this.r.get(i).v = seq;
////            }
//                if(this.r.containsKey(i)) {
//                    this.r.put(i, new retStatus(State.Forgotten, seq));
//                }
//            }
//        }
        this.mutex.lock();
        if(seq>this.z[this.me]) {
            this.z[this.me] = seq;
            for(int id = 0; id<this.peers.length;id++) {
                if(id != this.me)
                    this.Call("Decide",new Request(this.me,seq,true),id);
            }
        }
        for(int i = 0;i <= seq;i++) {
            if(this.r.containsKey(i)) {
                this.r.get(i).state = State.Forgotten;
            }
        }
        this.mutex.unlock();
    }


    /**
     * The application wants to know the
     * highest instance sequence known to
     * this peer.
     */
    public int Max(){
        this.mutex.lock();
        int ret = this.maxSeq;
        this.mutex.unlock();
        return ret;
    }

    /**
     * Min() should return one more than the minimum among z_i,
     * where z_i is the highest number ever passed
     * to Done() on peer i. A peers z_i is -1 if it has
     * never called Done().

     * Paxos is required to have forgotten all information
     * about any instances it knows that are < Min().
     * The point is to free up memory in long-running
     * Paxos-based servers.

     * Paxos peers need to exchange their highest Done()
     * arguments in order to implement Min(). These
     * exchanges can be piggybacked on ordinary Paxos
     * agreement protocol messages, so it is OK if one
     * peers Min does not reflect another Peers Done()
     * until after the next instance is agreed to.

     * The fact that Min() is defined as a minimum over
     * all Paxos peers means that Min() cannot increase until
     * all peers have been heard from. So if a peer is dead
     * or unreachable, other peers Min()s will not increase
     * even if all reachable peers call Done. The reason for
     * this is that when the unreachable peer comes back to
     * life, it will need to catch up on instances that it
     * missed -- the other peers therefore cannot forget these
     * instances.
     */
    public int Min(){
        this.mutex.lock();
        int minz = Integer.MAX_VALUE;
        for(int i:this.z) {
            if(i!=-1)
                minz=Math.min(i,minz);
        }
        this.mutex.unlock();
        return minz == Integer.MAX_VALUE? 0:minz+1;
    }



    /**
     * the application wants to know whether this
     * peer thinks an instance has been decided,
     * and if so what the agreed value is. Status()
     * should just inspect the local peer state;
     * it should not contact other Paxos peers.
     */
    public retStatus Status(int seq){
        // Your code here
        this.mutex.lock();
        retStatus status= this.r.getOrDefault(seq,new retStatus(State.Pending, null));
        if(seq < this.Min()) {
            status.state = State.Forgotten;
        }
        this.mutex.unlock();
        return status;
    }

    /**
     * helper class for Status() return
     */
    public class retStatus{
        public State state;
        public Object v;

        public retStatus(State state, Object v){
            this.state = state;
            this.v = v;
        }
    }

    /**
     * Tell the peer to shut itself down.
     * For testing.
     * Please don't change these four functions.
     */
    public void Kill(){
        this.dead.getAndSet(true);
        if(this.registry != null){
            try {
                UnicastRemoteObject.unexportObject(this.registry, true);
            } catch(Exception e){
                ////System.out.println("None reference");
            }
        }
    }

    public boolean isDead(){
        return this.dead.get();
    }

    public void setUnreliable(){
        this.unreliable.getAndSet(true);
    }

    public boolean isunreliable(){
        return this.unreliable.get();
    }


}
