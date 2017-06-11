package chord;

import java.util.HashMap;

/**
 * @author Team-6
 * @description This Class has been used to maintain the cache for each node which would be used while searching 
 * the data efficiently
 */


public class LRUCache {
    int capacity;
    HashMap<String, NodeInfo> map = new HashMap<String, NodeInfo>();
    NodeInfo head=null;
    NodeInfo end=null;
 
    public LRUCache(int capacity) {
        this.capacity = capacity;
    }
 
    public NodeInfo get(String key) {
        if(map.containsKey(key)){
        	NodeInfo n = map.get(key);
            remove(n);
            setHead(n);
            return n;
        }
        else {
        	return null;
        }
    }
 
    public void remove(NodeInfo n) {
    	
        if(n.pre != null){
            n.pre.next = n.next;
        }else{
            head = n.next;
        }

        if(n.next!=null){
            n.next.pre = n.pre;
        }else{
            end = n.pre;
        }
    }
    
public void remove(NodeInfo n,int delete) {
    	
        if(n.pre != null){
            n.pre.next = n.next;
        }else{
            head = n.next;
        }

        if(n.next!=null){
            n.next.pre = n.pre;
        }else{
            end = n.pre;
        }
        map.remove(n.key);
    }
    public void print() {
    	//System.out.println("Printing cache");
    	NodeInfo iterator = head;
    	while(iterator != null) {
    		//System.out.println(iterator.key + " " + iterator.nodeId);
    		iterator = iterator.next;

    	}
    	//if(head.next != null) 
    	//System.out.println(head.next.key + " " + head.next.nodeId);
//    	while(iterator != null) {
//    		System.out.println(iterator.key + " " + iterator.nodeId);
//    	}
    	
    }
 
    public void setHead(NodeInfo n) {
        n.next = head;
        n.pre = null;
 
        if(head != null)
            head.pre = n;
 
        head = n;
 
        if(end == null)
            end = head;
    }
 
    //call when no data available in cache and insert the new da
    public void set(String key, String ip , int port , int nodeId) {
        if(map.containsKey(key)){
        	NodeInfo old = map.get(key);
            //old = nodeInfo;
            remove(old);
            setHead(old);
        }else{
        	NodeInfo created = new NodeInfo(key,ip,port,nodeId);
            if(map.size()>=capacity){
                map.remove(key);
                remove(end);
                setHead(created);
 
            }else{
                setHead(created);
            }    
 
            map.put(key, created);
        }
    }
}
