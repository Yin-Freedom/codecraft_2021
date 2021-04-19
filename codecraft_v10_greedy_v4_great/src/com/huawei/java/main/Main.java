package com.huawei.java.main;

import java.io.*;
import java.util.*;

// cd /mnt/d/business/competition/sdk_java
/**
 * @author 尹辉东
 */
public class Main {
    public static String path_1="./././././training/training-1.txt";
    public static String path_2="./././././training/training-2.txt";
    public static String path_out="./././././training/out.txt";
    public static String answer="./././././training/answer.txt";

    public static List<String> serverKind=new ArrayList<>();
    public static Map<String,Server> serverData=new HashMap<>();//server type
    public static Map<String,VM> vmData=new HashMap<>();//vm type
    public static List<String> vmKind=new ArrayList<>();
    //修改为list
    public static Server[] serverResource=new Server[12000];
    //public static Map<Integer,Server> serverResource=new HashMap<>();//购买的服务器
    public static List<Server> sort_serverResource=new ArrayList<>();
    public static int serverNumber=0;

    public static List<Integer> serverOnRunId =new ArrayList<>();//开机的服务器

    public static Map<String,VM> vmOnRun=new HashMap<>();//运行的虚拟机信息 有单一标识符

    public static int T=0;
    public static int[] R;// day_command
    public static int[] open_server;
    public static int[] migration_num;
    public static int[] vmOnRun_arr;
    public static float[] working_rate;

    public static List<String> request=new ArrayList<>();
    public static List<String> output=new ArrayList<>();

    public static int SERVERCOST=0,POWERCOST=0,TOTALCOST=0;//成本
    public static float[] resourceRatio;
    public static float totalRatio=0;
    public static float sum_migration=0;
    public static List<String> serverValueList=new ArrayList<>();


    public static void main(String[] args) throws IOException{
        // TODO: read standard input
        // TODO: process
        // TODO: write standard output
        // TODO: System.out.flush()
        //online
        /*Dispatch im=new Dispatch();
        im.importData();
        runDisPatch();
        systemOut();*/

        //offline
        long begin=System.currentTimeMillis();
        int two_train=0;
        ImportData im=new ImportData();
        initial();
        im.importData(path_2);
        runDisPatch();
        two_train+=TOTALCOST;

        long runtime=System.currentTimeMillis()-begin;
        System.out.printf("SERVERCOST: %s,POWERCOST: %s,TOTALCOST: %s.\n",SERVERCOST,POWERCOST,TOTALCOST);
        System.out.printf("迁移次数：%s | 利用率：%s | open_server: %s\n",sum_migration,totalRatio,Main.serverNumber);
        System.out.printf("Two train total cost: %s | runtime: %s ms.\n",two_train,runtime);
        print();

    }
    public static void runDisPatch() {
        sort_server(); // server_load 价格升序
        Dispatch d=new Dispatch();
        d.dispatch();

        int sum=0;
        for(int i=0;i<migration_num.length;i++){
            sum+=migration_num[i];
        }
        Main.sum_migration=sum;


        TOTALCOST=SERVERCOST+POWERCOST;
    }
    public static void systemOut(){ //
        PrintWriter out=new PrintWriter(new OutputStreamWriter(System.out));
        for(String s:output){
            out.println(s);
        }
        out.flush();
    }
    public static void print(){
        File file=new File(path_out);
        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));){
            for(String s:Main.output){
                writer.write(s+"\n");
            }
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    public static void sort_server(){

        for(int i=0;i<vmKind.size();i++){
            VM vm=Main.vmData.get(vmKind.get(i));
            String min_server=null;
            float min_money=Float.MAX_VALUE;
            float ratio=vm.cpu/vm.ram;
            for(int j=0;j<serverKind.size();j++){
                Server server=serverData.get(serverKind.get(j)).clone();
                if(Dispatch.judgeConfigVM(server,vm)!=-1){
                    // if( ((float)server.cpu/server.ram)<ratio ) continue;

                    if(vm.node==0){
                        if( (server.cpu/2-vm.cpu)<=30 || (server.ram/2-vm.ram)<=30 ){
                            continue;
                        }
                    }else{
                        if( (server.cpu/2-vm.cpu/2)<=40 || (server.ram/2-vm.ram/2)<=40 ){
                            continue;
                        }
                    }
                    if(server.get_valueRatioForDay()<min_money){
                        min_server=server.serverType;
                        min_money=server.get_valueRatioForDay();
                    }
                }
            }
            if(min_server!=null){
                vm.cheap_server=min_server;
            }
        }
    }

    public static void initial(){
        serverData=new HashMap<>();//server type
        serverKind=new ArrayList<>();
        vmData=new HashMap<>();//vm type
        vmKind=new ArrayList<>();
        //serverResource=new HashMap<>();//购买的服务器
        serverNumber=0;
        serverOnRunId =new ArrayList<>();//开机的服务器
        vmOnRun=new HashMap<>();//运行的虚拟机信息 有单一标识符
        T=0;
        request=new ArrayList<>();
        output=new ArrayList<>();
        SERVERCOST=0;POWERCOST=0;TOTALCOST=0;//成本
        totalRatio=0;
        sum_migration=0;
    }
}