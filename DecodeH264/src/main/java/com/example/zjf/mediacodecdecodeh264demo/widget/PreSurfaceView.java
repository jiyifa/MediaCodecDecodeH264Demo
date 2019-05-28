package com.example.zjf.mediacodecdecodeh264demo.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.camera2.params.LensShadingMap;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.ContextMenu;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sy on 2016/10/28.
 */
public class PreSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
    public   MediaCodec videoCoder;//视频解码器
    private  Surface surface;
    private  SurfaceHolder holder;
    public  Integer Handle = 0;
    public  PreSurfaceView(Context context, AttributeSet attrs){
        super(context,attrs);
        holder = this.getHolder();
        holder.addCallback(this);

    }

    public void InitDecoder(){
        int width = this.getWidth();
        int height = this.getHeight();
        Handle = 0;
        try {
            if (videoCoder == null) {
                videoCoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                MediaFormat format =MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
                videoCoder.configure(format,surface,null,0);
                if (videoCoder == null){
                    return;
                }
                videoCoder.start();
            }
        }
        catch (Exception e) {
        }
    }

    public void stopCoder(){
        if (videoCoder != null) {
            //停止解码，此时可以再次调用confire()方法
            videoCoder.stop();
            //释放内存
            videoCoder.release();
            //videoCoder = null;
        }

    }

    public void clearHolder(){
        //创建画布
        Canvas canvas = holder.lockCanvas(null);
        //设置画布颜色
        canvas.drawColor(Color.RED);
        //锁定画布
        holder.unlockCanvasAndPost(canvas);
    }

    public void DeCode(int handle,int CH,int iFrameType,int iDataType,int buffsize,byte[] strBuff){
        if (surface == null || videoCoder ==null){
            return;
        }
        if (iDataType == 2 || iDataType == 4){
            try {
                if (strBuff != null){
                    ByteBuffer[] inputBuffers = videoCoder.getInputBuffers();
                    ByteBuffer[] outputBuffer = videoCoder.getOutputBuffers();
                    int inputBufferIndex = videoCoder.dequeueInputBuffer(100);
                    if (inputBufferIndex >= 0){
                        //ByteBuffer inputBuffer = videoCoder.getInputBuffer(inputBufferIndex);
                        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                        inputBuffer.clear();
                        inputBuffer.put(strBuff,0, buffsize);
                        videoCoder.queueInputBuffer(inputBufferIndex,0,buffsize,100,0);
                    }
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outIndex = videoCoder.dequeueOutputBuffer(bufferInfo, 100);  //获取接收到结果的ByteBuffer索引位置
                    //outIndex = videoCoder.dequeueOutputBuffer(bufferInfo, 100);  //获取接收到结果的ByteBuffer索引位置
                    while (outIndex>=0){
                        //对outputbuffer的处理完后，调用这个函数把buffer重新返回给codec类。
                        //调用这个api之后，SurfaceView才有图像
                        videoCoder.releaseOutputBuffer(outIndex, true);
                        outIndex = videoCoder.dequeueOutputBuffer(bufferInfo, 100);
                    }
                }
            }
            catch (Throwable t){
                t.printStackTrace();
            }
        }
    }

    /**
     * surface生命周期
     * @param holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (surface==null) {
            surface = holder.getSurface();
            InitDecoder();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (surface!=null) {
            surface=null;
            stopCoder();
        }
    }
}
