package kvpaxos;
import paxos.Paxos;
import paxos.State;
import paxos.Paxos.retStatus;
import paxos.State;
// You are allowed to call Paxos.Status to check if agreement was made.

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Server implements KVPaxosRMI {

    ReentrantLock mutex;
    Registry registry;
    Paxos px;
    int me;

    String[] servers;
    int[] ports;
    KVPaxosRMI stub;

    // Your definitions here
    Integer firstUnchosenIndex;
    Integer nextAvail;
    HashMap<String, Integer> mapping;


    public Server(String[] servers, int[] ports, int me){
        this.me = me;
        this.servers = servers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.px = new Paxos(me, servers, ports);
        // Your initialization code here
        this.firstUnchosenIndex = 0;
        this.nextAvail = 0;
        this.mapping = new HashMap<>();

        try{
            System.setProperty("java.rmi.server.hostname", this.servers[this.me]);
            registry = LocateRegistry.getRegistry(this.ports[this.me]);
            stub = (KVPaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("KVPaxos", stub);
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    // RMI handlers
    public Response Get(Request req){
        // Your code here
        Integer answer = -1;
        this.mutex.lock();
        retStatus status = this.px.Status(this.nextAvail);
        while (status.state != State.Pending){
            this.nextAvail += 1;
            status = this.px.Status(this.nextAvail);
        }
        this.mutex.unlock();

        // retry if needed
        int to = 10;
        boolean finished = false;
        Op op = new Op("Get", req.req_id, req.key, req.value);
        while (true) {
            to = 10;
            this.px.Start(this.nextAvail, op);
            while (true) {
                retStatus ret = this.px.Status(this.nextAvail);

                if (ret.state == State.Decided) {
                    if (op.equals(ret.v)) {
                        finished = true;
                    }
                    this.nextAvail += 1;
                    break;
                }
                else if (ret.state == State.Forgotten){
                    this.nextAvail += 1;
                    break;
                }

                try {
                    Thread.sleep(to);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (to < 1000) {
                    to = to * 2;
                }
            }
            if (finished){
                break;
            }
        }

        mutex.lock();
        for (int i = this.firstUnchosenIndex; i < this.nextAvail; i += 1){
            Op log = (Op) this.px.Status(this.firstUnchosenIndex).v;
            if (log.op.equals("Put")){
                this.mapping.put(log.key, log.value);
            }
            this.firstUnchosenIndex += 1;
        }
        mutex.unlock();
        Integer value = this.mapping.get(req.key);
        this.px.Done(this.nextAvail-1);
        return new Response(true, value);
    }

    public Response Put(Request req){
        // Your code here
        // get an initial seq number
        this.mutex.lock();
        retStatus status = this.px.Status(this.nextAvail);
        while (status.state != State.Pending){
            this.nextAvail += 1;
            status = this.px.Status(this.nextAvail);
        }
        this.mutex.unlock();

        // retry if needed
        int to = 10;
        boolean finished = false;
        Op op = new Op("Put", req.req_id, req.key, req.value);
        while (true) {
            to = 10;
            this.px.Start(this.nextAvail, op);
            while (true) {
                retStatus ret = this.px.Status(this.nextAvail);
                if (ret.state == State.Decided) {
                    if (op.equals(ret.v)) {
                        finished = true;
                    }
                    this.nextAvail += 1;
                    break;
                }
                else if (ret.state == State.Forgotten){
                    this.nextAvail += 1;
                    break;
                }

                try {
                    Thread.sleep(to);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (to < 1000) {
                    to = to * 2;
                }
            }
            if (finished){
                break;
            }
        }

        this.px.Done(this.nextAvail-1);
        return new Response(finished, 1);
    }


}
