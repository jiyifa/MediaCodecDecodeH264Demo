package com.example.zjf.mediacodecdecodeh264demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.example.zjf.mediacodecdecodeh264demo.widget.PreSurfaceView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class TestActivity extends AppCompatActivity {
    private static final int PEMISSION_EXTERNAL_STORAGE = 1;
    private final static String SD_PATH = Environment.getExternalStorageDirectory().getPath();
    private final static String H264_FILE = SD_PATH + "/H264.h264"+"/test.h264";
    private Button playBnt;
    private PreSurfaceView preSurfaceView;
    private DataInputStream mInputStream;
    private boolean mStopFlag = false;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_test);
        playBnt = findViewById(R.id.playBnt);
        preSurfaceView = findViewById(R.id.sv_preview);
        requestSdPemission();
        playBnt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFileInputStream();
                byte[] streamBuffer = null;
                try {
                    streamBuffer = getBytes(mInputStream);

                    byte[] make0 = new byte[]{0,0,0,1};
                    byte[] dummyFrame = new byte[]{0x00,0x00,0x01,0x20};

                    int byte_cnt = 0;
                    while (!mStopFlag){
                        //得到可用字节数组长度
                        byte_cnt = streamBuffer.length;
                        if(byte_cnt==0){
                            streamBuffer=dummyFrame;
                        }
                        int start_index = 0;
                        //定义记录剩余字节的变量
                        int remaining = byte_cnt;
                        while (true){
                            //当剩余的字节=0或者开始读取的字节下标大于可用的字节数时，不再继续读取
                            if(remaining==0||start_index>=remaining){
                                break;
                            }
                            //寻找帧头部
                            int nextFrameStart = KMPMatch(make0,streamBuffer,start_index+2,remaining);
                            //找不到头部，返回-1
                            if(nextFrameStart==-1){
                                nextFrameStart = remaining;
                            }

                            preSurfaceView.DeCode(1,1,1,2,nextFrameStart-start_index,streamBuffer);
                            //指定下一帧的位置
                            start_index = nextFrameStart;
                        }
                        mStopFlag = true;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getFileInputStream(){
        File file = new File(H264_FILE);
        try {
            mInputStream = new DataInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //动态申请外部存储读写权限
    public void requestSdPemission(){
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permission!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},PEMISSION_EXTERNAL_STORAGE);
        }
    }
    //获得可用的字节数组
    public static byte[] getBytes(InputStream is) throws IOException {
        int len;
        int size = 1024;
        byte[] buf;
        if (is instanceof ByteArrayInputStream) {
            //返回可用的剩余字节
            size = is.available();
            //创建一个对应可用相应字节的字节数组
            buf = new byte[size];
            //读取这个文件并保存读取的长度
            len = is.read(buf, 0, size);
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            buf = new byte[size];
            while ((len = is.read(buf, 0, size)) != -1)
                //将读取的数据写入到字节输出流
                bos.write(buf, 0, len);
            //将这个流转换成字节数组
            buf = bos.toByteArray();
        }
        return buf;
    }

    /**
     * 查找帧头部的位置
     * @param pattern 文件头字节数组
     * @param bytes 可用的字节数组
     * @param start 开始读取的下标
     * @param remain 可用的字节数量
     * @return
     */
    private int KMPMatch(byte[] pattern,byte[] bytes,int start,int remain){
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int[] lsp = computeLspTable(pattern);

        int j = 0;  //Number of chars matched in pattern
        for(int i = start; i < remain; i++){
            while (j > 0 && bytes[i] != pattern[j]){
                //Fall back in the pattern
                j = lsp[j -1];//Strictly decreasing
            }
            if(bytes[i] == pattern[j]){
                //Next char matched,increment posistion
                j++;
                if(j == pattern.length){
                    return i - (j-1);
                }
            }
        }
        return -1; //Not found
    }

    //0 1 2 0
    private int[] computeLspTable(byte[] pattern){
        int[] lsp = new int[pattern.length];
        lsp[0] = 0;//Base case
        for(int i = 1; i < pattern.length; i++){
            int j = lsp[i-1];
            while (j>0 && pattern[i] !=pattern[j]){
                j = lsp[j - 1];
            }
            if(pattern[i] == pattern[j]){
                j++;
            }
            lsp[i] = j;
        }
        return lsp;
    }
}
