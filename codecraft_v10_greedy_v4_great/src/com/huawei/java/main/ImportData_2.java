package com.huawei.java.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.Scanner;
import java.util.Vector;
// javac -encoding utf8 -d .
/**
 * @author 尹辉东
 */
public class ImportData_2 {
    int N=0;
    int M=0;
    int T=0;
    int[] R;
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
