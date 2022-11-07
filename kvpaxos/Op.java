package kvpaxos;
import java.io.Serializable;

/**
 * You may find this class useful, free to use it.
 */
public class Op implements Serializable{
    static final long serialVersionUID=33L;
    String op;
    Integer ClientSeq;
    String key;
    Integer value;

    public Op(String op, Integer ClientSeq, String key, Integer value){
        this.op = op;
        this.ClientSeq = ClientSeq;
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object v){
        Op tmp = (Op) v;
        return tmp.op.equals(this.op) && tmp.ClientSeq.equals(this.ClientSeq) && tmp.key.equals(this.key) && tmp.value.equals(this.value);
    }
}
