package com.huawei.java.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author 尹辉东
 */
public class Judger {
    String path_1="./././././training/training-1.txt";
    String path_2="./././././training/training-2.txt";
    String path_3="./././././training/training-3.txt";
    String path_4="./././././training/training-4.txt";
    static String path_out;

    List<String> serverKind=new ArrayList<>();
    Map<String, Server> serverData=new HashMap<>();//server type
    List<String> filter_server_kind=new ArrayList<>();
    Map<String, VM> vmData=new HashMap<>();//vm type
    List<String> vmKind=new ArrayList<>();
    int vm_cpu_max;//training-2 128
    int vm_ram_max;//training-2 128
    float[] valueRatio; //性价比
    float[] cmRatio;   //容量比

    Map<Integer, Server> serverResource=new HashMap<>();//购买的服务器
    int serverNumber=0;
    List<Integer> serverOnRun =new ArrayList<>();//开机的服务器
    Map<String, VM> vmOnRun=new HashMap<>();//运行的虚拟机信息 有单一标识符

    static int T=0;
    int[] R;// day_command
    int[] add_day;
    int[] del_day;
    static int[] open_server;
    static int[] migration_num;
    int[] vmOnRun_arr;

    List<String> request=new ArrayList<>();

    static int SERVERCOST=0,POWERCOST=0,TOTALCOST=0;//成本
    float[] resourceRatio;
    static float totalRatio=0;
    int sum_migration=0;
    int today=0;


    public int N=0;
    public int M=0;
    public int[][] resource;
    int cpu;
    int ram;

    public int k=0;

    public void importData(String path){
        
        N=0;
        M=0;
        T=0;
        cpu=0;
        ram=0;
        k=0;

        File file=new File(path);
        try{
            FileReader reader=new FileReader(file);
            BufferedReader br=new BufferedReader(reader);
            String line=null;
            int index=0;
            while((line=br.readLine())!=null){
                if(line.isEmpty()){
                    continue;
                }
                if(index==0){
                    N=Integer.valueOf(line);
                    valueRatio=new float[N];
                    cmRatio=new float[N];
                }
                if(index>0 && index<=N){
                    String s=line.substring(1,line.length()-1);
                    String[] arr=s.split(", ");
                    Server server=new Server(arr[0],arr[1],arr[2],arr[3],arr[4]);
                    serverData.put(arr[0],server);
                    serverKind.add(arr[0]);

                }
                if(index==N+1){
                    M=Integer.valueOf(line);
                }
                if(index>N+1 && index<=N+M+1){
                    String s=line.substring(1,line.length()-1);
                    String[] arr=s.split(", ");
                    VM vm=new VM(arr[0],arr[1],arr[2],arr[3]);
                    vmData.put(arr[0],vm);
                    vmKind.add(arr[0]);
                }
                if(M>0 && index==N+M+2){
                    T=Integer.valueOf(line);
                    R=new int[T];
                    resource=new int[2][T];
                    resourceRatio =new float[T];
                }
                if(T>0 && index>N+M+2){
                    if(line.charAt(0)!='('){
                        R[k]=Integer.valueOf(line);
                        if(index!=N+M+3){
                            resource[0][k]=cpu;
                            resource[1][k]=ram;
                        }
                        cpu=0;
                        ram=0;
                        k++;
                        continue;
                    }
                    request.add(line);
                    String s=line.substring(1,line.length()-1);
                    String[] arr=s.split(", ");
                    if(arr[0].equals("add")){
                        cpu+=vmData.get(arr[1]).cpu;
                        ram+=vmData.get(arr[1]).ram;
                    }
                }
                index++;
            }
            T=T;
            R=R;
            add_day=new int[T];
            del_day=new int[T];
            open_server=new int[T];
            migration_num=new int[T];
            vmOnRun_arr=new int[T];

            vm_cpu_max=0;
            vm_ram_max=0;
            for(String s:vmKind){
                if(vmData.get(s).cpu>vm_cpu_max){
                    vm_cpu_max=vmData.get(s).cpu;
                }
                if(vmData.get(s).ram>vm_ram_max){
                    vm_ram_max=vmData.get(s).ram;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //已经读入信息，开始进行调度
    public void dispatch() throws Exception{

        import_dispatch_info();
        int index=0;
        //先purchase migration，然后处理add del
        for(int i=0;i<T;i++){
            today=i;
            serverOnRun =new ArrayList<>();
            //获取输出文件中 服务器购买、虚拟机迁移、虚拟机部署
            List<String> add_info=get_dispatch_information();
            int index_dispatch=0;
            //处理每天的请求
            for(int j=0;j<R[i];j++){
                String element=request.get(index+j);

                if(element.charAt(1)=='a'){
                    String[] arr=element.substring(1,element.length()-1).split(", ");

                    String position=add_info.get(index_dispatch);
                    String[] position_add=position.substring(1,position.length()-1).split(", ");
                    int node;
                    if(position_add.length==1){
                        node=2;
                    }else{
                        if(position_add[1].equals("A")){
                            node=0;
                        }else{
                            node=1;
                        }
                    }
                    VM vm=new VM(arr[2],vmData.get(arr[1]));
                    if(add_vm(Integer.parseInt(position_add[0]),arr[2],arr[1],node)==-1){
                        System.out.println("放置虚拟机超出服务器资源限制(CPU或内存)，运行程序提前退出-返回码(0)");
                        throw new Exception();
                    }
                    index_dispatch++;
                }else{
                    delVM(element.substring(1,element.length()-1).split(", "));
                }
            }
            
            day_data();
            index+=R[i];
        }
        float sum=0;
        for(int i=0;i<T;i++){
            sum+=resourceRatio[i];
        }
        totalRatio=sum/T;
    }

    List<String> add_dispatch=new ArrayList<>();
    public void import_dispatch_info(){
        List<String> list=new ArrayList<>();
        File file=new File(path_out);
        try {
            FileReader reader = new FileReader(file);
            BufferedReader br = new BufferedReader(reader);
            String line;
            while((line=br.readLine())!=null){
                add_dispatch.add(line);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    int dispatch_index=0;

    public List<String> get_dispatch_information() throws Exception{
        List<String> add_list=new ArrayList<>();

        for(int i=dispatch_index;i<add_dispatch.size();i++){
            String element=add_dispatch.get(i);
            if(element.charAt(1)!='p' && element.charAt(1)!='m'){
                add_list.add(element);
            }

            if(element.charAt(1)=='p'){
                if(add_list.size()>0){
                    dispatch_index=i;
                    return add_list;
                }
                String[] arr=element.substring(1,element.length()-1).split(", ");
                /*if(arr[1].equals("0")){
                    continue;
                }*/
                int n=Integer.parseInt(arr[1]);
                for(int j=i+1;j<=i+n;j++){
                    String pur_info=add_dispatch.get(j);
                    String[] pur_arr=pur_info.substring(1,pur_info.length()-1).split(", ");
                    pur_server(Integer.parseInt(pur_arr[1]),pur_arr[0]);
                }
                i=i+n;
                continue;
            }
            String[] arr=element.substring(1,element.length()-1).split(", ");
            if(element.charAt(1)=='m' && !arr[1].equals("0")){
                int n=Integer.parseInt(arr[1]);
                migration_num[today]=n;
                for(int j=i+1;j<=i+n;j++){
                    String mi=add_dispatch.get(j);
                    String[] mig_info=mi.substring(1,mi.length()-1).split(", ");
                    migration(mig_info);
                }
                i=i+n;
                continue;
            }
        }
        return add_list;
    }
    public void pur_server(int n, String server_type){
        if(n==0){
            return;
        }else if(n>0){
            for(int i=0;i<n;i++){
                Server server=new Server(serverNumber+i,serverData.get(server_type));
                serverResource.put(server.id,server);
                SERVERCOST+=server.hardwareCost;
            }
            serverNumber+=n;
            TOTALCOST+=serverData.get(server_type).hardwareCost*n;
        }
    }

    public void migration(String[] arr) throws Exception{
        String vmId;
        int target_server;
        int target_node=0;
        if(arr.length==2){
            vmId=arr[0];
            target_server=Integer.parseInt(arr[1]);
            target_node=2;
        }else{
            vmId=arr[0];
            target_server=Integer.parseInt(arr[1]);
            String node=arr[2];
            if(node.equals("A")){
                target_node=0;
            }else if(node.equals("B")){
                target_node=1;
            }
        }

        VM vm=vmOnRun.get(vmId);

        Server origin_server=serverResource.get(vm.deployServer);
        int origin_node=vm.deployNode;
        if(configVM(serverResource.get(target_server),vmOnRun.get(vmId),target_node)==-1){
            System.out.println(Arrays.toString(arr));
            System.out.println("day:"+today+", 迁移 放置虚拟机超出服务器资源限制(CPU或内存)");
            throw new Exception();
        }

        if(origin_node==0){
            origin_server.dynamicInfo[0]+=vm.cpu;
            origin_server.dynamicInfo[1]+=vm.ram;
        }else if(origin_node==1){
            origin_server.dynamicInfo[2]+=vm.cpu;
            origin_server.dynamicInfo[3]+=vm.ram;
        }else if(origin_node==2){
            origin_server.dynamicInfo[0]+=vm.cpu/2;
            origin_server.dynamicInfo[1]+=vm.ram/2;
            origin_server.dynamicInfo[2]+=vm.cpu/2;
            origin_server.dynamicInfo[3]+=vm.ram/2;
        }
    }
    public int configVM(Server server, VM vm, int node){
        if(node == 2) {
            if(server.dynamicInfo[0]-vm.cpu/2>=0 &&
                    server.dynamicInfo[1]-vm.ram/2>=0 &&
                    server.dynamicInfo[2]-vm.cpu/2>=0 &&
                    server.dynamicInfo[3]-vm.ram/2>=0){
                vm.deployNode=2;
                vm.deployServer=server.id;
                server.dynamicInfo[0]-=vm.cpu/2;
                server.dynamicInfo[1]-=vm.ram/2;
                server.dynamicInfo[2]-=vm.cpu/2;
                server.dynamicInfo[3]-=vm.ram/2;
                return 2;
            }
            return -1;
        }else if(node==0) {
            if(server.dynamicInfo[0]-vm.cpu>=0 &&
                    server.dynamicInfo[1]-vm.ram>=0) {
                vm.deployNode = 0;
                vm.deployServer = server.id;
                server.dynamicInfo[0] -= vm.cpu;
                server.dynamicInfo[1] -= vm.ram;
                return 0;
            }
            return -1;
        }else if(node==1) {
            if (server.dynamicInfo[2] - vm.cpu >= 0 &&
                    server.dynamicInfo[3] - vm.ram >= 0) {
                vm.deployNode = 1;
                vm.deployServer = server.id;
                server.dynamicInfo[2] -= vm.cpu;
                server.dynamicInfo[3] -= vm.ram;
                return 1;
            }
            return -1;
        }
        return -1;
    }

    public int add_vm(int serverId, String vmId, String  vmType, int node){
        VM vm=new VM(vmId, vmData.get(vmType));
        Server server=serverResource.get(serverId);

        if(node==0) {
            if (server.dynamicInfo[0] - vm.cpu >= 0 &&
                    server.dynamicInfo[1] - vm.ram >= 0) {
                vmOnRun.put(vm.id,vm);
                server.loadVM.add(vm);
                vm.deployNode = 0;
                vm.deployServer = server.id;
                server.dynamicInfo[0] -= vm.cpu;
                server.dynamicInfo[1] -= vm.ram;
                return 0;
            }
        }
        if(node==1) {
            if(server.dynamicInfo[2]-vm.cpu>=0 &&
                    server.dynamicInfo[3]-vm.ram>=0) {
                vmOnRun.put(vm.id,vm);
                server.loadVM.add(vm);
                vm.deployNode = 1;
                vm.deployServer = server.id;
                server.dynamicInfo[2] -= vm.cpu;
                server.dynamicInfo[3] -= vm.ram;
                return 1;
            }
            return -1;
        }
        if(node==2){
            if(server.dynamicInfo[0]-vm.cpu/2>=0 &&
                    server.dynamicInfo[1]-vm.ram/2>=0 &&
                    server.dynamicInfo[2]-vm.cpu/2>=0 &&
                    server.dynamicInfo[3]-vm.ram/2>=0){
                vmOnRun.put(vm.id,vm);
                server.loadVM.add(vm);
                vm.deployNode=2;
                vm.deployServer=server.id;
                server.dynamicInfo[0]-=vm.cpu/2;
                server.dynamicInfo[1]-=vm.ram/2;
                server.dynamicInfo[2]-=vm.cpu/2;
                server.dynamicInfo[3]-=vm.ram/2;
                return 2;
            }
            return -1;
        }
        return -1;
    }
    public void delVM(String[] sys){
        String vmId=sys[1];
        VM vm=vmOnRun.get(vmId);
        int serverId=vm.deployServer;
        Server server=serverResource.get(serverId);
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
        vmOnRun.remove(vmId);
    }

    public void day_data(){
        for(int i=0;i<serverNumber;i++){
            Server server=serverResource.get(i);
            if( server.dynamicInfo[0]!=server.cpu/2 ||
                    server.dynamicInfo[1]!=server.ram/2 ||
                    server.dynamicInfo[2]!=server.cpu/2 ||
                    server.dynamicInfo[3]!=server.ram/2 ) {
                POWERCOST+=server.energyCost;
                serverOnRun.add(server.id);

            }
            //每台server的resourceRatio每天计算
            server.resourceRatio=dF((server.cpu+server.ram)-(server.dynamicInfo[0]+server.dynamicInfo[2]+
                    server.dynamicInfo[1]+ server.dynamicInfo[3]),(server.cpu+server.ram));
            server.utilization=dF((server.cpu+server.ram)-(server.dynamicInfo[0]+server.dynamicInfo[2]+
                    server.dynamicInfo[1]+ server.dynamicInfo[3]),(server.cpu+server.ram));
        }
        float ratio=0;
        for(int serverNumber:serverOnRun){
            Server server=serverResource.get(serverNumber);
            ratio+=server.resourceRatio;
        }
        resourceRatio[today]=dF(ratio,serverOnRun.size());
        vmOnRun_arr[today]=vmOnRun.size();
        open_server[today]=serverOnRun.size();
    }
    public float dF(int a, int b){
        DecimalFormat df=new DecimalFormat("0.00");
        return Float.parseFloat(df.format((float)a/b));
    }
    public float dF(float a, int b){
        DecimalFormat df=new DecimalFormat("0.00");
        return Float.parseFloat(df.format((float)a/b));
    }
    
    public static void main(String[] args) throws Exception{
        String path_1="D:/business/Competition/2021_HuaWei code craft/training-data/training-1.txt";
        String path_2="D:/business/Competition/2021_HuaWei code craft/training-data/training-2.txt";
        String path_s="D:/business/Competition/2021_HuaWei code craft/training-data/t2-output.txt";
        String out="D:/business/Competition/2021_HuaWei code craft/training-data/out.txt";
        path_out=out;

        Judger judger=new Judger();
        judger.importData(path_2);
        judger.dispatch();

        int sum=0;
        for(int i=0;i<migration_num.length;i++){
            sum+=migration_num[i];
        }
        judger.sum_migration=sum;
        TOTALCOST=SERVERCOST+POWERCOST;
        System.out.printf("SERVERCOST: %s,POWERCOST: %s,TOTALCOST: %s.\n",SERVERCOST,POWERCOST,TOTALCOST);
        System.out.printf("迁移次数：%s | 利用率：%s | open_server: %s\n",judger.sum_migration,totalRatio,judger.serverNumber);
    }
}
