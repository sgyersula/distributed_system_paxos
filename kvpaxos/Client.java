package kvpaxos;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.lang.Math;

public class Client {
    String[] servers;
    int[] ports;

    // Your data here
    int req_id;
    int client_id;

    public Client(String[] servers, int[] ports){
        this.servers = servers;
        this.ports = ports;
        // Your initialization code here
        this.req_id = 0;
        this.client_id = (int)(Math.random() * 1000);
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
        KVPaxosRMI stub;
        try{
            Registry registry= LocateRegistry.getRegistry(this.ports[id]);
            stub=(KVPaxosRMI) registry.lookup("KVPaxos");
            if(rmi.equals("Get"))
                callReply = stub.Get(req);
            else if(rmi.equals("Put")){
                callReply = stub.Put(req);}
            else
                System.out.println("Wrong parameters!");
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }
        return callReply;
    }

    // RMI handlers
    public Integer Get(String key){
        // Your code here
        Integer value = -1;
        this.req_id += 1;
        for(int i = 0; i < servers.length; i++) {
            Response response = this.Call("Get",new Request("Get",key,client_id, req_id), i);
//            if (response == null){
//                continue;
//            }
            if (response.ok) {
                value = response.answer;
                break;
            }
        }
        return value;
    }

    public boolean Put(String key, Integer value){
        // Your code here
        boolean success = false;
        this.req_id += 1;
        for(int i = 0; i < servers.length; i++) {
            Response response = this.Call("Put", new Request("Put", key, value, client_id, req_id), i);

            if (response.ok) {
                success = true;
                break;
            }
        }
        return success;
    }

}
