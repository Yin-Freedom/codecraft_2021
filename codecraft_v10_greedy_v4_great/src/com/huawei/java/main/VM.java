package com.huawei.java.main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.lang.*;

/**
 * @author 尹辉东
 */
public class VM implements Cloneable, Comparable<VM>{
    //id 唯一，请求时初始化id。不超过带符号32位整数表示的范围
    public String id;
    public String vmType;
    public int cpu;
    public int ram;
    public int node;

    public int deployNode;// 0-双节点 1-A 2-B
    public int deployServer;// id
    public String effective_load;
    public String cheap_server;
    public String min_server;
    public int open_day;
    public int migration_day;

    public float crRatio;

    public VM(String vmType, String cpu, String ram, String node) {
        this.vmType = vmType;
        this.cpu = Integer.valueOf(cpu);
        this.ram = Integer.valueOf(ram);
        this.node = Integer.valueOf(node);
        this.crRatio =this.cpu/this.ram;
    }

    public VM(String id, VM vm) {
        this.id=id;
        this.vmType=vm.vmType;
        this.cpu=vm.cpu;
        this.ram=vm.ram;
        this.node=vm.node;
        this.crRatio=vm.crRatio;
        this.effective_load=vm.effective_load;
        this.cheap_server=vm.cheap_server;
        this.min_server=vm.min_server;
        //this.deployNode = vm.deployNode;
        //this.deployServer = vm.deployServer;
    }
    public VM(){

    }


    /**
     * 原生clone是protected，如果用这个标记会访问父类的浅拷贝 clone()
     * 所以，将其可见性标记为public
     * @return
     */
    public VM clone(){
        VM clone=null;
        try{
            clone=(VM) super.clone();
        }catch(CloneNotSupportedException e){
            throw new RuntimeException(e);  // won't happen
        }
        return clone;
    }

    public static List<VM> copy(List<VM> list){
        List<VM> copy=new ArrayList<>(list.size());
        Iterator<VM> itr=list.iterator();
        while(itr.hasNext()){
            copy.add(itr.next().clone());
        }
        return copy;
    }
    // cpu 相比 ram 是更贵的，所以 cr 比越大 占用的服务器价值也就越大
    public int compareTo(VM vm){
        float diff=vm.crRatio-this.crRatio;
        if(diff>0){
            return 1;
        }else if(diff<0){
            return -1;
        }
        return 0;
    }
        @Override
    public String toString() {
        return "VM{" +
                "id='" + id + '\'' +
                ", vmType='" + vmType + '\'' +
                ", cpu=" + cpu +
                ", ram=" + ram +
                ", node=" + node +
                '}';
    }
}
