package com.huawei.java.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author 尹辉东
 */
public class  Dispatch {

    private List<VM> vm_add = new ArrayList<>();
    private List<String> log_purchase = new ArrayList<>();  //顺序存放每天的购买信息
    private List<String> log_migration = new ArrayList<>(); //顺序存放每天的迁移信息
    private List<Server> sort_serverResource = new ArrayList<>();
    private List<Server> del_vm_server = new ArrayList<>();
    private int today_vm_num = 0;
    public static List<VM> migration_vm = new ArrayList<>();
    ;

    private int T;
    private int[] R;
    static int today;

    private void initial() {
        T = Main.T;
        R = Main.R;
        Main.working_rate = new float[T];
    }

    public void dispatch() {
        initial();
        int req_index = 0;

        for (int i = 0; i < T; i++) {
            today = i;
            System.out.println(i);
            Main.serverOnRunId = new ArrayList<>();
            // 先把一天的请求全部打包
            List<String> list = new ArrayList<>();
            for (int j = 0; j < R[i]; j++) {
                String res = Main.request.get(req_index + j);
                list.add(res);
            }
            dispatchOneDay(list, i);// 每天分配vm
            update_data_day();// 更新每天数据
            req_index += R[i];
        }
    }

    private void dispatchOneDay(List<String> list, int day) {
        //初始化 数据
        vm_add = new ArrayList<>();
        log_purchase = new ArrayList<>();
        log_migration = new ArrayList<>();

        List<Server> total_server = new ArrayList<>();
        List<String> addList = new ArrayList<>();
        List<VM> addListVM = new ArrayList<>(); // 分段操作的vm添加列表
        List<VM> roll_vmList = new ArrayList<>(); // 回滚操作的vm列表，在每次放到已有服务器上，从该表中删除，同时将vm put到vm数据中

        for (int i = 0; i < list.size(); i++) {
            String res = list.get(i);
            if (res.charAt(1) == 'a') {
                addList.add(res);
                String s = res.substring(1, res.length() - 1);
                String[] arr = s.split(", ");
                VM vm = new VM(arr[2], Main.vmData.get(arr[1]));
                addListVM.add(vm);
                roll_vmList.add(vm);
            }
            if (res.charAt(1) == 'd' || i == list.size() - 1) {
                segment_process(addListVM, roll_vmList);//先往所有服务器里面添加
                addListVM = new ArrayList<>();

                if (res.charAt(1) == 'a') continue;
                String s = res.substring(1, res.length() - 1);
                String[] arr = s.split(", ");
                delVM(arr); // 如果是当天添加，当天删除要注意，现在的程序没有把虚拟机put到vmOnRun中
            }
        }
        total_server = BFD(new ArrayList<>(), roll_vmList);

        max_migration();
        expansion(total_server);
        add_output(addList, vm_add);
    }

    private void segment_process(List<VM> addVMList, List<VM> roll_vmList) {
        // 先cpu+ram大的，再双节点优先
        Collections.sort(addVMList, new Comparator<VM>() {
            @Override
            public int compare(VM o1, VM o2) {
                //int diff=o1.cpu+o1.ram-o2.cpu-o2.ram;
                int diff = o1.node - o2.node;
                if (diff > 0) {
                    return 1;
                } else if (diff < 0) {
                    return -1;
                } else {
                    //return o1.node-o2.node;
                    return o1.cpu + o1.ram - o2.cpu - o2.ram;
                }
            }
        });

        // 放到已有服务器上，怎么放是个问题
        for (int i = addVMList.size() - 1; i > -1; i--) {
            VM vm = addVMList.get(i);
            if (deploy_VM_max_remain(vm)) {
                // deploy 函数中如果能够放到已经购买的服务器上，自动更新服务器loadVM和vmOnRun
                addVMList.remove(vm);
                roll_vmList.remove(vm);
            }
        }
    }

    private List<Server> BFD(List<Server> total_server, List<VM> vmList) {
        List<Server> res_servers = new ArrayList<>();
        List<VM> cpu_asc = VM.copy(vmList);
        Collections.sort(cpu_asc, new Comparator<VM>() { // 先节点升序，会先处理双节点，然后按cpu+ram升序，会先处理空间大的
            @Override
            public int compare(VM o1, VM o2) {
                int diff = o1.node - o2.node;
                //int diff=o1.cpu+o1.ram-o2.cpu-o2.ram;
                if (diff > 0) {
                    return 1;
                } else if (diff < 0) {
                    return -1;
                } else {
                    return o1.cpu + o1.ram - o2.cpu - o2.ram;
                    //return o1.node-o2.node;
                }
            }
        });

        while (!cpu_asc.isEmpty()) {
            VM vm = cpu_asc.get(cpu_asc.size() - 1);
            Server best_server = null;
            int i = 0;
            // 新买服务器 最小剩余资源
            int min_score = Integer.MAX_VALUE;
            for (Server server : res_servers) {
                int position = judgeConfigVM(server, vm);
                int score = server.get_max_remain(vm, position);
                if (position != -1 && score < min_score) {
                    best_server = server;
                    min_score = score;
                }
                i++;
            }

            if (best_server != null) {
                configVM(best_server, vm);
                best_server.loadVM.add(vm);
                cpu_asc.remove(vm);
                i = 0;
                continue;
            }
            if (i == res_servers.size()) {
                Server cheap_server = Main.serverData.get(vm.cheap_server).clone();
                res_servers.add(cheap_server);

            }
        }

        return res_servers;
    }

    // 添加运行的服务器，运行的虚拟机
    // 可以购买多种类型服务器，自动调整虚拟机的deploy信息，同时添加服务器和虚拟机
    private void expansion(List<Server> servers) {
        int n = servers.size();
        if (n == 0) {
            log_purchase.add("(purchase, " + 0 + ")");
        } else {
            Collections.sort(servers, new Comparator<Server>() {
                @Override
                public int compare(Server o1, Server o2) {
                    return o1.serverType.compareTo(o2.serverType);
                }
            });
            //排序后 计算类型数 重新调整其上loadVM的deploy信息
            String type = servers.get(0).serverType;
            int type_index = 0;
            List<Integer> type_num = new ArrayList<>();
            List<String> type_name = new ArrayList<>();
            type_name.add(type);
            for (int i = 0; i < servers.size(); i++) {
                Server server = servers.get(i);
                server.id = Main.serverNumber + i;

                for (VM vm : server.loadVM) {
                    vm.deployServer = server.id;
                    vm_add.add(vm);
                    vm.open_day = today;
                    Main.vmOnRun.put(vm.id, vm);//将vm添加到 运行vm中
                }
                Main.serverResource[server.id] = server;//将服务器添加到 运行服务器中
                if (!server.serverType.equals(type)) {
                    type_num.add(i - type_index);
                    type_index = i;
                    type = server.serverType;
                    type_name.add(type);
                }
                if (i == servers.size() - 1) {
                    type_num.add(i - type_index + 1);
                }
                Main.SERVERCOST += server.hardwareCost;
            }
            Main.serverNumber += servers.size();
            log_purchase.add("(purchase, " + type_num.size() + ")");
            for (int i = 0; i < type_num.size(); i++) {
                log_purchase.add("(" + type_name.get(i) + ", " + type_num.get(i) + ")");
            }
        }
    }

    public void update_data_day() {
        sort_serverResource = new ArrayList<>();
        int working_server = 0;
        for (int i = 0; i < Main.serverNumber; i++) {
            Server server = Main.serverResource[i];
            if (server.dynamicInfo[0] != server.cpu / 2 ||
                    server.dynamicInfo[1] != server.ram / 2 ||
                    server.dynamicInfo[2] != server.cpu / 2 ||
                    server.dynamicInfo[3] != server.ram / 2) {
                Main.POWERCOST += server.energyCost;
                server.utilization = server.get_utilization();
                sort_serverResource.add(server); // 把开机服务器进行排序，可能第二天有删除操作，所以不去除元素
            }
            server.update_total_resour();
        }
        Main.working_rate[today] = (float) working_server / Main.serverNumber;

        sort_serverResource.sort(new Comparator<Server>() { // 降序，从头开始
            @Override
            public int compare(Server o1, Server o2) {
                if (o1 == null || o2 == null) return 0;
                int a = 1000 * o1.loadVM.size() + (o1.cpu + o1.ram - o1.dynamicInfo[0] - o1.dynamicInfo[1] - o1.dynamicInfo[2] - o1.dynamicInfo[3]);
                int b = 1000 * o2.loadVM.size() + (o2.cpu + o2.ram - o2.dynamicInfo[0] - o2.dynamicInfo[1] - o2.dynamicInfo[2] - o2.dynamicInfo[3]);
                return b - a;
            }
        });

        today_vm_num = Main.vmOnRun.size();
    }

    public boolean deploy_VM_min_utilization(VM vm) {
        float max_utilization = -1;
        Server max_server = new Server();
        // sort列表每天更新，所以当前已经购买的服务器没有被更新
        for (int i = 0; i < Main.serverNumber; i++) {
            Server server = Main.serverResource[i];
            int position = judgeConfigVM(server, vm);
            if (position != -1) {
                float utilization;
                utilization = server.get_min_utilization(vm, position);
                if (utilization > max_utilization) {
                    max_utilization = utilization;
                    max_server = server;
                }
            }
        }
        if (max_utilization >= 0) {
            Main.vmOnRun.put(vm.id, vm);
            max_server.loadVM.add(vm);
            configVM(max_server, vm);
            vm.open_day = today;
            return true;
        }
        return false;
    }

    public boolean deploy_VM_max_remain(VM vm) {
        int max_remain = Integer.MAX_VALUE;
        Server max_server = null;
        // sort列表每天更新，所以当前已经购买的服务器没有被更新
        for (int i = 0; i < Main.serverNumber; i++) {
            Server server = Main.serverResource[i];
            int position = judgeConfigVM(server, vm);
            if (position != -1) {
                int score = server.get_max_remain(vm, position);
                if (score < max_remain) {
                    max_remain = score;
                    max_server = server;
                }
            }
        }
        if (max_server != null) {
            Main.vmOnRun.put(vm.id, vm);
            max_server.loadVM.add(vm);
            configVM(max_server, vm);
            vm.open_day = today;
            return true;
        }
        return false;
    }

    public int configVM(Server server, VM vm) {
        //TODO
        int position = judgeConfigVM(server, vm);

        if (position == 0) {
            vm.deployNode = 0;
            vm.deployServer = server.id;
            server.dynamicInfo[0] -= vm.cpu;
            server.dynamicInfo[1] -= vm.ram;
            return 0;
        }
        if (position == 1) {
            vm.deployNode = 1;
            vm.deployServer = server.id;
            server.dynamicInfo[2] -= vm.cpu;
            server.dynamicInfo[3] -= vm.ram;
            return 1;
        }
        if (position == 2) {
            vm.deployNode = 2;
            vm.deployServer = server.id;
            server.dynamicInfo[0] -= vm.cpu / 2;
            server.dynamicInfo[1] -= vm.ram / 2;
            server.dynamicInfo[2] -= vm.cpu / 2;
            server.dynamicInfo[3] -= vm.ram / 2;
            return 2;
        }
        return -1;
    }

    //返回 节点代表值
    public static int judgeConfigVM(Server server, VM vm) {
        //TODO
        if (vm.node == 0) {
            // 如果AB节点都可以放置，应该返回让选择一个利用率更大的
            boolean A_p = false;
            boolean B_p = false;
            float A_utilization = -1;
            float B_utilization = -1;

            if (server.dynamicInfo[0] - vm.cpu >= 0 &&
                    server.dynamicInfo[1] - vm.ram >= 0) {
                A_p = true;
                A_utilization = server.get_min_utilization(vm, 0);
            }
            if (server.dynamicInfo[2] - vm.cpu >= 0 &&
                    server.dynamicInfo[3] - vm.ram >= 0) {
                B_p = true;
                B_utilization = server.get_min_utilization(vm, 1);
            }
            // 如果 A B 节点都可以放，返回利用率最高的节点
            if (A_p && B_p) {
                if (A_utilization > B_utilization) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if (A_p) {
                    return 0;
                } else if (B_p) {
                    return 1;
                }
            }
            return -1;
        } else if (vm.node == 1) {
            int A_cpu = server.dynamicInfo[0] - vm.cpu / 2;
            int A_ram = server.dynamicInfo[1] - vm.ram / 2;
            int B_cpu = server.dynamicInfo[2] - vm.cpu / 2;
            int B_ram = server.dynamicInfo[3] - vm.ram / 2;
            if (server.dynamicInfo[0] - vm.cpu / 2 >= 0 &&
                    server.dynamicInfo[1] - vm.ram / 2 >= 0 &&
                    server.dynamicInfo[2] - vm.cpu / 2 >= 0 &&
                    server.dynamicInfo[3] - vm.ram / 2 >= 0) {
                return 2;
            }
            return -1;
        }
        return -1;
    }

    public void delVM(String[] sys) {
        String vmId = sys[1];
        VM vm = Main.vmOnRun.get(vmId);
        int serverId = vm.deployServer;
        Server server = Main.serverResource[serverId];
        if (vm.deployNode == 0) {
            server.dynamicInfo[0] += vm.cpu;
            server.dynamicInfo[1] += vm.ram;
        } else if (vm.deployNode == 1) {
            server.dynamicInfo[2] += vm.cpu;
            server.dynamicInfo[3] += vm.ram;
        } else if (vm.deployNode == 2) {
            server.dynamicInfo[0] += vm.cpu / 2;
            server.dynamicInfo[1] += vm.ram / 2;
            server.dynamicInfo[2] += vm.cpu / 2;
            server.dynamicInfo[3] += vm.ram / 2;
        }
        server.loadVM.remove(vm);
        Main.vmOnRun.remove(vmId);

        server.del_day = today;
        if (!del_vm_server.contains(server)) {
            del_vm_server.add(server);
        }
    }

    public void add_output(List<String> addList, List<VM> vmList) {
        Main.output.addAll(log_purchase);
        Main.output.addAll(log_migration);

        String info_deploy = null;
        for (int i = 0; i < addList.size(); i++) {
            String ele = addList.get(i);
            String s = ele.substring(1, ele.length() - 1);
            String[] arr = s.split(", ");

            VM vm = Main.vmOnRun.get(arr[2]);
            if (vm == null) {
                System.out.println("输出日志 ??");
            }
            if (vm.deployNode == 2) {
                info_deploy = "(" + vm.deployServer + ")";
            } else if (vm.deployNode == 0) {
                info_deploy = "(" + vm.deployServer + ", A)";
            } else if (vm.deployNode == 1) {
                info_deploy = "(" + vm.deployServer + ", B)";
            }
            Main.output.add(info_deploy);
        }
    }

    // 将删除过虚拟机的服务器上的虚拟机迁移到其他非新买的服务器上
    private void max_migration() {
        migration_vm.clear();

        sort_serverResource.sort(new Comparator<Server>() { // 降序，从头开始
            @Override
            public int compare(Server o1, Server o2) {
                if (o1 == null || o2 == null) return 0;
                int a = 120 * o1.loadVM.size() + (o1.cpu - o1.dynamicInfo[0] - o1.dynamicInfo[2])+ (o1.ram- o1.dynamicInfo[1] - o1.dynamicInfo[3]);
                int b = 120 * o2.loadVM.size() + (o2.cpu - o2.dynamicInfo[0] - o2.dynamicInfo[2])+ (o2.ram - o2.dynamicInfo[1]  - o2.dynamicInfo[3]);
                return b - a;
            }
        });

        outer:
        for (int i = sort_serverResource.size() - 1; i > -1; i--) { // 从低到高
            if (migration_vm.size() + 1 > today_vm_num * 5 / 1000) { // 迁移vm超出数量限制，pass
                break;
            }

            Server migration_server = sort_serverResource.get(i);
            if (migration_server.del_day == today_vm_num) continue;

            List<VM> vmList = migration_server.loadVM;
            if (migration_server.get_total_resour() < 39) continue; // 这个10可以调参
            /*vmList.sort(new Comparator<VM>() {
                @Override
                public int compare(VM o1, VM o2) {
                    return o1.cpu + o1.ram - o2.cpu - o2.ram; //降序 , 升序有调优
                }
            });*/

            out:
            for (int k = vmList.size() - 1; k > -1; k--) { //从小的开始
                VM vm = vmList.get(k);
                if (vm.open_day == today) {
                    break;
                }
                if (vm.migration_day == today) { // 直接判断比查找要好
                    break;
                }
                if (migration_vm.contains(vm)) continue;
                int origin_deployNode = vm.deployNode;
                Server best_server = null;
                int min_score = Integer.MAX_VALUE;
                float max_utilization = -1;
                // 从删除过vm的server上往所有服务器上放
                for (int j = 0; j < sort_serverResource.size(); j++) {
                    Server search_server = sort_serverResource.get(j);// 不会是空
                    if (search_server.id == migration_server.id) continue;
                    if (search_server.del_day == today || search_server.get_total_resour() < vm.cpu + vm.ram) {
                        continue;
                    }
                    if (migration_vm.size() + 1 > today_vm_num * 5 / 1000) { // 迁移vm超出数量限制，pass
                        break outer;
                    }
                    if (search_server.del_day == today) {
                        //search_server.id==migration_server.id || || search_server.get_total_resour() < 10
                        continue;
                    }

                    int position = judgeConfigVM(search_server, vm);
                    int score = search_server.get_max_remain(vm, position);
                    if (position != -1 && score < min_score) {
                        min_score = score;
                        best_server = search_server;
                    }
                    /*int position=judgeConfigVM(search_server,vm);
                    float utilization=search_server.get_min_utilization(vm,position);
                    if(position != -1 && utilization>max_utilization){
                        max_utilization=utilization;
                        best_server=search_server;
                    }*/
                }
                if (best_server != null) {
                    vm.migration_day = today;
                    vmList.remove(vm);
                    configVM(best_server, vm);
                    best_server.loadVM.add(vm);
                    migration_vm.add(vm);
                    remove(migration_server, vm, origin_deployNode);
                }

            }
        }

        int n = migration_vm.size();
        Main.migration_num[today] = n;
        if (n == 0) {
            log_migration.add("(migration, " + 0 + ")");
        } else {
            log_migration.add("(migration, " + n + ")");
            for (int i = 0; i < n; i++) {
                VM vm = migration_vm.get(i);
                switch (vm.deployNode) {
                    case 2:
                        log_migration.add("(" + vm.id + ", " + vm.deployServer + ")");
                        break;
                    case 0:
                        log_migration.add("(" + vm.id + ", " + vm.deployServer + ", " + "A)");
                        break;
                    case 1:
                        log_migration.add("(" + vm.id + ", " + vm.deployServer + ", " + "B)");
                        break;
                }
            }
        }
    }

    private void remove(Server server, VM vm, int origin_node) {
        server.loadVM.remove(vm);
        if (origin_node == 0) {
            server.dynamicInfo[0] += vm.cpu;
            server.dynamicInfo[1] += vm.ram;
        } else if (origin_node == 1) {
            server.dynamicInfo[2] += vm.cpu;
            server.dynamicInfo[3] += vm.ram;
        } else if (origin_node == 2) {
            server.dynamicInfo[0] += vm.cpu / 2;
            server.dynamicInfo[1] += vm.ram / 2;
            server.dynamicInfo[2] += vm.cpu / 2;
            server.dynamicInfo[3] += vm.ram / 2;
        }
    }

    int N=0;
    int M=0;
    int[][] resource;
    int cpu;
    int ram;

    public void importData() throws IOException {

        BufferedReader cin=new BufferedReader(new InputStreamReader(System.in));
        String line;
        while(true){
            N = Integer.parseInt(cin.readLine());
            for(int i=0;i<N;i++){
                line=cin.readLine();
                String s=line.substring(1,line.length()-1);
                String[] arr=s.split(", ");
                Server server=new Server(arr[0],arr[1],arr[2],arr[3],arr[4]);
                Main.serverData.put(arr[0],server);
                Main.serverKind.add(arr[0]);
            }
            M = Integer.parseInt(cin.readLine());
            for(int i=0;i<M;i++){
                line=cin.readLine();
                String s=line.substring(1,line.length()-1);
                String[] arr=s.split(", ");
                VM vm=new VM(arr[0],arr[1],arr[2],arr[3]);
                Main.vmData.put(arr[0],vm);
                Main.vmKind.add(arr[0]);
            }
            T = Integer.parseInt(cin.readLine());
            Main.T=T;
            R=new int[T];
            resource=new int[2][T];
            Main.resourceRatio=new float[T];
            for(int i=0;i<T;i++){
                R[i]=Integer.parseInt(cin.readLine());
                cpu=0;
                ram=0;
                for(int j=0;j<R[i];j++) {
                    line=cin.readLine();
                    Main.request.add(line);
                    String s=line.substring(1,line.length()-1);
                    String[] arr=s.split(", ");
                    if(arr[0].equals("add")){
                        cpu+=Main.vmData.get(arr[1]).cpu;
                        ram+=Main.vmData.get(arr[1]).ram;
                    }
                }
                resource[0][i]=cpu;
                resource[1][i]=ram;
            }
            break;
        }

        Main.R=R;
        Main.open_server=new int[T];
        Main.migration_num=new int[T];
        Main.vmOnRun_arr=new int[T];

    }
}

