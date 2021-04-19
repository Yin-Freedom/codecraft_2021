package com.huawei.java.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

/**
 * @author 尹辉东
 */
public class ImportData {
    public int N=0;
    public int M=0;
    public int T=0;
    public int[] R;
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
                }
                if(index>0 && index<=N){
                    String s=line.substring(1,line.length()-1);
                    String[] arr=s.split(", ");
                    Server server=new Server(arr[0],arr[1],arr[2],arr[3],arr[4]);
                    Main.serverData.put(arr[0],server);
                    Main.serverKind.add(arr[0]);
                    Main.serverValueList.add(arr[0]);
                }
                if(index==N+1){
                    M=Integer.valueOf(line);
                }
                if(index>N+1 && index<=N+M+1){
                    String s=line.substring(1,line.length()-1);
                    String[] arr=s.split(", ");
                    VM vm=new VM(arr[0],arr[1],arr[2],arr[3]);//
                    Main.vmData.put(arr[0],vm);
                    Main.vmKind.add(arr[0]);
                }
                if(M>0 && index==N+M+2){
                    T=Integer.valueOf(line);
                    R=new int[T];
                    resource=new int[2][T];
                    Main.resourceRatio =new float[T];
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
                    Main.request.add(line);
                    String s=line.substring(1,line.length()-1);
                    String[] arr=s.split(", ");
                    if(arr[0].equals("add")){
                        cpu+=Main.vmData.get(arr[1]).cpu;
                        ram+=Main.vmData.get(arr[1]).ram;
                    }
                }
                index++;
            }
            Main.T=T;
            Main.R=R;
            Main.open_server=new int[T];
            Main.migration_num=new int[T];
            Main.vmOnRun_arr=new int[T];

        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
