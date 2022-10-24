package com.qiniu.voiceintercom;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.qiniu.voiceintercom.model.SendUrl;
import com.qiniu.voiceintercom.utils.ByteUtils;
import com.qiniu.voiceintercom.utils.G711;
import com.qiniu.util.Auth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 语音实时对讲
 */
public class PCM2G711aActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_record;
    private TextView tv;//当前状态
    private boolean isRecording;//是否正在在录制和童话
    private AudioRecord audioRecord;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    public static final String ACCESS_KEY = "your AK";
    public static final String SECRET_KEY = "your SK";
    public static final String NAMESPACES = "your 空间ID";
    public static final String DEVICES = "your 设备国标ID";
    private String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcm_2_g711a);

        btn_record = findViewById(R.id.btn_record);
        tv = findViewById(R.id.tv);

        btn_record.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btn_record) {
            Log.e("isRecording:", isRecording + "");
            if (!isRecording) {
                if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(this, permissions, 1);
                }else {
                    showStates("正在获取音频发送url");
                    getUrl();
                }

            }else{
                closeRecord();
                showStates("已结束");
                btn_record.setText("开始语音对讲");
            }

        }
    }

    /**
     * 停止录制和通话
     */
    private void closeRecord() {
        showStates("已结束");
        btn_record.setText("开始语音对讲");
        isRecording = false;
        if (null != audioRecord) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }
    /**
     * 停止录制和通话
     */
    private void creatAudioRecord() {
        final int minBufferSize = AudioRecord
                .getMinBufferSize(8000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
        //构造函数参数：
        //1.记录源
        //2.采样率，以赫兹表示
        //3.音频声道描述，声道数
        //4.返回音频声道的描述，格式
        //5.写入音频数据的缓冲区的总大小（字节），小于最小值将创建失败

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);


//        final File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.g711a");
//        if (!file.mkdirs()) {
//            Log.e("demo failed---->", "Directory not created");
//        }
//        if (file.exists()) {
//            file.delete();
//        }

//        Log.e("TAG", "creatAutiRecord: " + file.getAbsolutePath());
//        tv.setText(file.getAbsolutePath());
        //实时获取语音数据并且转g711a编码后发送
        executor.execute(new Runnable() {
            @Override
            public void run() {
                audioRecord.startRecording();
                isRecording = true;
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
//                    FileOutputStream os = new FileOutputStream(file);
                short[] inG711Buffer = new short[minBufferSize];
                byte[] outG711Buffer = new byte[minBufferSize];

//                    if (null != os) {
                while (isRecording) {
                    int read = audioRecord.read(inG711Buffer, 0, minBufferSize);
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        //转g711a
                        G711.linear2alaw(inG711Buffer, 0, outG711Buffer, inG711Buffer.length);
                        sendAudio(outG711Buffer);
//                                try {
//                                    os.write(outG711Buffer);
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
                    }

                }
//                Log.e("run------>", "close file output stream !=" + file.getAbsolutePath());
//                        os.close();
//                    }
            }
        });
    }
    private void showStates(String stateName){
        tv.setText(stateName);
    }
    /**
     * 生成qiniutoken
     * @param url 您的url
     * @param body 您的body入参
     * @return
     */
    private String qiniuToken(String url, String body) {
        Auth auth = Auth.create(ACCESS_KEY, SECRET_KEY);
        String authorization = (String) auth.authorizationV2(url, "POST", body.getBytes(), "application/json; charset=utf-8").get("Authorization");
        System.out.println(authorization);
        return authorization;
    }


    private SendUrl sendUrl;

    private void getUrl() {
        String url = "http://qvs.qiniuapi.com/v1/namespaces/"+NAMESPACES+"/devices/"+DEVICES+"/talk";
        Gson gson = new Gson();

        Map params = new HashMap();
//        isV2	是	bool	该字段为true时，启用低延迟版本，收到返回地址后在发送语音数据
//        channels	否	string数组	设备类型为平台时，需指定通道ID
//        version	否	string	对讲国标协议版本，取值"2014"或"2016"，默认为2014，例如大部分大华摄像头为GBT 28181-2014版本对讲模式
//        transProtocol	否	string	取值"tcp"或"udp"，流传输模式，默认udp
        params.put("isV2", true);//该字段为true时，启用低延迟版本，收到返回地址后在发送语音数据
        String body = gson.toJson(params);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body);
        Request.Builder requestBuild = new Request.Builder();
        requestBuild.url(url);

        requestBuild.header("Authorization", qiniuToken(url, body));
        requestBuild.post(requestBody);
        Request request = requestBuild.build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String eStr = e.toString();
                        showToast("获取发送url失败，" + eStr);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String body = response.body().string();
                            sendUrl = new Gson().fromJson(body, SendUrl.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (sendUrl != null && !TextUtils.isEmpty(sendUrl.audioSendAddrForHttp)) {
                            creatAudioRecord();
                            btn_record.setText("停止语音对讲");
                            showStates("正在语音对讲");
//                            showToast("开始录音");
                        } else {
                            showToast("解析发送url失败");
                        }

                    }
                });

            }
        });

    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String tag = "PCM2G711";
    Gson gson = new Gson();

    /**
     * 发送g711a编码语音数据
     * @param outG711Buffer
     */
    void sendAudio(byte[] outG711Buffer) {
        String base64_pcm = ByteUtils.bytesToBase64(outG711Buffer);
//        ByteUtils.saveFile(this,base64_pcm);
        Map params = new HashMap();
        params.put("base64_pcm", base64_pcm);
        Log.e(tag, base64_pcm);
        String url = sendUrl.audioSendAddrForHttp;
        String body = gson.toJson(params);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body);
        Request.Builder requestBuild = new Request.Builder();
        requestBuild.url(url);
        requestBuild.post(requestBody);
        requestBuild.header("Authorization", qiniuToken(sendUrl.audioSendAddrForHttp, body));
        Request request = requestBuild.build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(tag, "发送Audio数据失败:" + e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.e(tag, "发送Audio数据成功:" + response.body().string());
            }
        });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeRecord();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PackageManager packageManager = this.getPackageManager();
        PermissionInfo permissionInfo = null;
        for (int i = 0; i < permissions.length; i++) {
            try {
                permissionInfo = packageManager.getPermissionInfo(permissions[i], 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            CharSequence permissionName = permissionInfo.loadLabel(packageManager);
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED){
                showStates("正在获取音频发送url");
                getUrl();
            } else {
                showToast("您拒绝了【" + permissionName + "】权限");
            }
        }
    }
}