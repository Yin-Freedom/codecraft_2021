package com.huawei.java.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.lang.*;

/**
 * @author 尹辉东
 */
public class Server implements Cloneable, Comparable<Server>{
    private static final long serialVersionUID = 1L;
    //id 唯一，在购买时初始化id
    public int id;
    public String serverType;
    public int cpu;
    public int ram;
    public int hardwareCost;
    public int energyCost;
    public int[] dynamicInfo=new int[4];// acpu aram bcpu bram
    public List<VM> loadVM;
    public int local_node;
    public int del_day;
    public int total_sour;

    public float resourceRatio=0;
    public float crRatio;
    public float utilization;
    public float valueRationForDay;

    public int open_day;//记录服务器开机的那一天
    public int curr_size;

    public int onServer;// 运行时，即cpu和ram有使用时为1，否则为-1

    public Server(String serverType, String cpu, String ram, String hardwareCost, String energyCost) {
        this.serverType = serverType;
        this.cpu = Integer.valueOf(cpu);
        this.ram = Integer.valueOf(ram);
        this.hardwareCost = Integer.valueOf(hardwareCost);
        this.energyCost = Integer.valueOf(energyCost);
        dynamicInfo[0]=this.cpu/2;
        dynamicInfo[1]=this.ram/2;
        dynamicInfo[2]=this.cpu/2;
        dynamicInfo[3]=this.ram/2;
        this.crRatio =this.cpu/this.ram;
        loadVM=new ArrayList<>();
    }
    public Server(int id, Server server){
        this.id=id;
        this.serverType=server.serverType;
        this.cpu=server.cpu;
        this.ram=server.ram;
        this.hardwareCost=server.hardwareCost;
        this.energyCost=server.energyCost;
        dynamicInfo[0]=server.cpu/2;
        dynamicInfo[1]=server.ram/2;
        dynamicInfo[2]=server.cpu/2;
        dynamicInfo[3]=server.ram/2;
        this.crRatio =this.cpu/this.ram;
        loadVM=new ArrayList<>();
    }
    public Server(){

    }

    public void update_valueRatioForDay(){
        this.valueRationForDay=(float)(hardwareCost+(800-Dispatch.today)*energyCost)/1;
    }
    public float get_valueRatioForDay(){ //两个方法算的一样，Emmm
        //return (float)(hardwareCost+(800-Dispatch.today)*energyCost)/1;
        return (float)hardwareCost;
    }

    public float get_utilization(){
        float res;
        res=1 - (float)(dynamicInfo[0]+dynamicInfo[1]+dynamicInfo[2]+dynamicInfo[3])/(cpu+ram);
        return res;
    }

    // position==0 返回A节点cpu利用率，否则返回B节点cpu利用率
    public float get_cpu_utilization(VM vm, int position){
        if(vm.node==0){ // 单节点虚拟机，分别算A B节点利用率

            if(position==0){
                return 1 - ((float)(dynamicInfo[0]-vm.cpu)/(cpu/2));
            }else if(position==1){
                return 1 - ((float)(dynamicInfo[2]-vm.cpu)/(cpu/2));
            }
        }else{ //双节点虚拟机，分别算A B节点利用率
            if(position==0){
                return 1 - ((float)(dynamicInfo[0]-vm.cpu/2)/(cpu/2));
            }else if(position==1){
                return 1 - ((float)(dynamicInfo[2]-vm.cpu/2)/(cpu/2));
            }
        }
        return -1;
    }
    // position==0 返回A节点ram利用率，否则返回B节点ram利用率
    public float get_ram_utilization(VM vm, int position){
        if(vm.node==0){ // 单节点虚拟机，分别算A B节点利用率
            if(position==0){
                return 1-((float)(dynamicInfo[1]-vm.ram)/(ram/2));
            }else if(position==1){
                return 1-((float)(dynamicInfo[3]-vm.ram)/(ram/2));
            }
        }else{ //双节点虚拟机，分别算A B节点利用率
            if(position==0){
                return 1-((float)(dynamicInfo[1]-vm.ram/2)/(ram/2));
            }else if(position==1){
                return 1-((float)(dynamicInfo[3]-vm.ram/2)/(ram/2));
            }
        }
        return -1;
    }
    // 肯定能够放下 才计算利用率
    public float get_min_utilization(VM vm, int position){
        if(this.loadVM.isEmpty()){
            return 0;
        }{
            /*int A_cpu=dynamicInfo[0]-vm.cpu;
            int A_ram=dynamicInfo[1]-vm.ram;
            int B_cpu=dynamicInfo[2]-vm.cpu;
            int B_ram=dynamicInfo[3]-vm.ram;*/
            if(position==0 || position==1){
                /*if(position==0){
                    if( (A_cpu<3 && A_ram>20) || (A_ram<3 && A_cpu>20) ){ // 只判断单节点，双节点可能出现一个放满的情况，不好判断
                        return 0.05f;
                    }
                }
                if(position==1){
                    if( (B_cpu<3 && B_ram>20) || (B_ram<3 && B_cpu>20) ){
                        return 0.05f;
                    }
                }*/
                float cpu_u=get_cpu_utilization(vm,position);
                float ram_u=get_ram_utilization(vm,position);
                return cpu_u < ram_u ? cpu_u : ram_u;
            }else{
                float A_cpu_u=get_cpu_utilization(vm,0);
                float A_ram_u=get_ram_utilization(vm,0);
                float A_u= A_cpu_u < A_ram_u ? A_cpu_u : A_ram_u;
                float B_cpu_u=get_cpu_utilization(vm,1);
                float B_ram_u=get_ram_utilization(vm,1);
                float B_u= B_cpu_u < B_ram_u ? B_cpu_u : B_ram_u;
                return A_u < B_u ? A_u : B_u;
            }
        }
    }
    public int get_cpu_remain(VM vm, int position){
        if(vm.node==0){ // 单节点虚拟机，分别算A B节点利用率
            if(position==0){
                return dynamicInfo[0]-vm.cpu;
            }else if(position==1){
                return dynamicInfo[2]-vm.cpu;
            }
        }else{ //双节点虚拟机，分别算A B节点利用率
            if(position==0){
                return dynamicInfo[0]-vm.cpu/2;
            }else if(position==1){
                return dynamicInfo[2]-vm.cpu/2;
            }
        }
        return -1;
    }
    // position==0 返回A节点ram利用率，否则返回B节点ram利用率
    public int get_ram_remain(VM vm, int position){
        if(vm.node==0){ // 单节点虚拟机，分别算A B节点利用率
            if(position==0){
                return dynamicInfo[1]-vm.ram;
            }else if(position==1){
                return dynamicInfo[3]-vm.ram;
            }
        }else{ //双节点虚拟机，分别算A B节点利用率
            if(position==0){
                return dynamicInfo[1]-vm.ram/2;
            }else if(position==1){
                return dynamicInfo[3]-vm.ram/2;
            }
        }
        return -1;
    }
    public int get_max_remain(VM vm, int position){
        if(this.loadVM.isEmpty()){
            return Integer.MAX_VALUE-1;
        }{
            if(position==0 || position==1){
                int cpu_u=get_cpu_remain(vm,position);
                int ram_u=get_ram_remain(vm,position);
                return cpu_u > ram_u ? cpu_u : ram_u;
            }else{
                int A_cpu_u=get_cpu_remain(vm,0);
                int A_ram_u=get_ram_remain(vm,0);
                int A_u= A_cpu_u > A_ram_u ? A_cpu_u : A_ram_u;
                int B_cpu_u=get_cpu_remain(vm,1);
                int B_ram_u=get_ram_remain(vm,1);
                int B_u= B_cpu_u > B_ram_u ? B_cpu_u : B_ram_u;
                return A_u > B_u ? A_u : B_u;
            }
        }
    }

    public int get_total_resour(){
        return dynamicInfo[0]+dynamicInfo[1]+dynamicInfo[2]+dynamicInfo[3];
    }
    public void update_total_resour(){
        total_sour=dynamicInfo[0]+dynamicInfo[1]+dynamicInfo[2]+dynamicInfo[3];
    }

    @Override
    // Server 克隆时复制了当前资源使用，初始化了loadVM
    public Server clone(){
        Server clone=null;
        try{
            clone=(Server)super.clone();
            Server clone2=(Server)super.clone();
            clone.dynamicInfo=new int[4];
            clone.dynamicInfo[0]=clone2.dynamicInfo[0];
            clone.dynamicInfo[1]=clone2.dynamicInfo[1];
            clone.dynamicInfo[2]=clone2.dynamicInfo[2];
            clone.dynamicInfo[3]=clone2.dynamicInfo[3];

            clone.loadVM=new ArrayList<>();
        }catch(CloneNotSupportedException e){
            return null;
        }
        return clone;
    }

    public Server clone_loadVM(){
        Server clone=null;
        try{
            clone=(Server)super.clone();
            Server clone2=(Server)super.clone();
            clone.dynamicInfo=new int[4];
            clone.dynamicInfo[0]=clone2.dynamicInfo[0];
            clone.dynamicInfo[1]=clone2.dynamicInfo[1];
            clone.dynamicInfo[2]=clone2.dynamicInfo[2];
            clone.dynamicInfo[3]=clone2.dynamicInfo[3];

            clone.loadVM=VM.copy(clone.loadVM);
        }catch(CloneNotSupportedException e){
            return null;
        }
        return clone;
    }

    public static List<Server> copy_loadVM(List<Server> list){
        List<Server> copy=new ArrayList<>(list.size());
        Iterator<Server> itr=list.iterator();
        while(itr.hasNext()){
            copy.add((Server)itr.next().clone_loadVM());
        }
        return copy;
    }

    //@Override
    public int compareTo(Server server){
        float diff=server.utilization-this.utilization;
        if(diff>0){
            return 1;
        }else if(diff<0){
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "Server{" +
                "id=" + id +
                ", serverType='" + serverType + '\'' +
                ", cpu=" + cpu +
                ", ram=" + ram +
                ", hardwareCost=" + hardwareCost +
                ", energyCost=" + energyCost +
                Arrays.toString(dynamicInfo)+
                '}';
    }

    // 找到loadVM中最大的，并删去 传过来server中的vm，添加容量
    // 如果loadVM中没有vm，返回null
    public VM get_max_cpu_vm(){
        if(loadVM==null || loadVM.size()==0){
            return null;
        }
        int max=Integer.MIN_VALUE;
        int maxId=0;
        for(int i=0;i<this.loadVM.size();i++){
            VM vm=this.loadVM.get(i);
            if(vm.cpu+vm.ram>max){
                max=vm.cpu+vm.ram;
                maxId=i;
            }
        }
        VM max_vm=loadVM.get(maxId);
        delVM(this, max_vm);
        return max_vm;
    }
    public VM get_last_vm(){
        if(loadVM==null || loadVM.size()==0){
            return null;
        }
        VM last_vm=loadVM.get(this.loadVM.size()-1);
        delVM(this, last_vm);
        return last_vm;
    }

    public void delVM(Server server, VM vm){
        if(vm.deployNode==0){
            server.dynamicInfo[0]+=vm.cpu;
            server.dynamicInfo[1]+=vm.ram;
        }else if(vm.deployNode==1){
            server.dynamicInfo[2]+=vm.cpu;
            server.dynamicInfo[3]+=vm.ram;
        }else if(vm.deployNode==2){
            server.dynamicInfo[0]+=vm.cpu/2;
            server.dynamicInfo[1]+=vm.ram/2;
            server.dynamicInfo[2]+=vm.cpu/2;
            server.dynamicInfo[3]+=vm.ram/2;
        }
        server.loadVM.remove(vm);
    }
}
