package paxos;
import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the request message for each RMI call.
 * Hint: You may need the sequence number for each paxos instance and also you may need proposal number and value.
 * Hint: Make it more generic such that you can use it for each RMI call.
 * Hint: Easier to make each variable public
 */
public class Request implements Serializable {
    static final long serialVersionUID=1L;
    // Your data here
    int seq;
    int n;
    Object value;

    boolean isDone;
    // Your constructor and methods here
    int from;
    int doneMax;

    public Request(int seq, Object value, int from, int doneMax) {
        this.seq = seq;
        this.value = value;
        this.from = from;
        this.doneMax = doneMax;
    }

    public Request(int seq, int n) {
        this.seq = seq;
        this.n = n;
    }

    public Request(int seq, Object value) {
        this.seq = seq;
        this.value = value;
    }

    public Request(int seq, int n, Object value) {
        this.seq = seq;
        this.n = n;
        this.value = value;
    }

    public Request(int seq, int doneMax, boolean isDone) {
        this.seq = seq;
        this.doneMax = doneMax;
        this.isDone = isDone;
    }
}
