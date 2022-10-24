package com.qiniu.voiceintercom.utils;

import android.app.Activity;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

public class ByteUtils {
    private static final char[] HEX_CHAR_TABLE = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public static int[] byteArrayToIntArray(byte[] array) {
        if (null == array) {
            return null;
        } else {
            int[] result = new int[array.length];
            for (int i = 0; i < array.length; i ++) {
                result[i] = byteToInt(array[i]);
            }
            return result;
        }
    }

    public static byte[] intArrayToByteArray(int[] array) {
        if (null == array) {
            return null;
        } else {
            byte[] result = new byte[array.length];
            for (int i = 0; i < array.length; i ++) {
                result[i] = intToByte(array[i]);
            }
            return result;
        }
    }

    public static byte intToByte(int x){
        byte b =(byte) (x & 0xff);
        return b;
    }

    public static int byteToInt(byte b){
        int x = b & 0xff;
        return x;
    }
    public static byte[] listTobyte(List<Byte> list) {
        if (list == null) {
            return null;
        } else {
            byte[] bytes = new byte[list.size()];
            int i = 0;
            Iterator<Byte> iterator = list.iterator();
            while (iterator.hasNext()) {
                bytes[i] = iterator.next();
                i++;
            }
            return bytes;
        }
    }

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    public static byte bitToByte(String bit) {
        int re, len;
        if (null == bit) {
            return 0;
        }
        len = bit.length();
        if (len != 4 && len != 8) {
            return 0;
        }
        if (len == 8) {// 8 bit处理
            if (bit.charAt(0) == '0') {// 正数
                re = Integer.parseInt(bit, 2);
            } else {// 负数
                re = Integer.parseInt(bit, 2) - 256;
            }
        } else {//4 bit处理
            re = Integer.parseInt(bit, 2);
        }
        return (byte) re;
    }

    public static String byteToBit(byte by){
        StringBuffer sb = new StringBuffer();
        sb.append((by>>7)&0x1)
                .append((by>>6)&0x1)
                .append((by>>5)&0x1)
                .append((by>>4)&0x1)
                .append((by>>3)&0x1)
                .append((by>>2)&0x1)
                .append((by>>1)&0x1)
                .append((by>>0)&0x1);
        return sb.toString();
    }



    /**
     * 十进制数据转换为十六进制字符串数
     *
     * @param dec
     * @return
     */
    public static String decToHex(String dec) {
        int data = Integer.parseInt(dec, 10);
        return Integer.toString(data, 16);
    }
    /**
     * 十六进制数据转换为十进制字符串数
     *
     * @param hex
     * @return
     */
    public static String hexToDec(String hex) {
        int data = Integer.parseInt(hex, 16);
        return Integer.toString(data, 10);
    }

    /**
     * 十六进制byte转十六进制字符串
     *
     * @param array
     * @return 如:0AFFBB
     */
    public static String hexToStr(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        return sb.toString();
    }
    public static String hexToStr(int[] array) {
        if (null != array) {
           byte[] byteArray = intArrayToByteArray(array);
           return hexToStr(byteArray);
        } else {
            return null;
        }
    }

    /**
     * 十六进制数据字符串转换为byte[]
     *
     * @param s  如：0AFFBB
     * @return
     */
    public static byte[] hexStrToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * 十六进制数据字符串转换为int[]
     *
     * @param s  如：0AFFBB
     * @return
     */
    public static int[] hexStrToIntArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return byteArrayToIntArray(data);
    }

    /**
     * char类型'0'--->转为0
     * @param c
     * @return
     */
    public static int charToInt(char c) {
        String str = String.valueOf(c);
        return Integer.parseInt(str);
    }

    /**
     * 高低位转为int
     * @param high
     * @param low
     * @return
     */
    public static int converHighLower(int high, int low) {
        int value = 0;
        value= ((high & 0xFF) << 8) | (low & 0xFF);
        return value;

//        int value = 515；
//        int big = (value & 0xFF00) >> 8;
//        int little = value & 0xFF;
    }

    /**
     * 字节数组转16进制
     * @param bytes 需要转换的byte数组
     * @return  转换后的Hex字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if(hex.length() < 2){
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }
    /**
     * 字节数组转base64
     * @param bytes 需要转换的byte数组
     * @return  转换后的base64字符串
     */
    public static String bytesToBase64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
    /**
     * 字节数组转base64
     * @return  转换后的base64字符串
     */
    public static void saveFile(Activity activity,String base64) {
        try {
            byte[] pdfAsBytes = Base64.decode(base64,Base64.NO_WRAP);
            final File file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test2.g711a");
            if (!file.mkdirs()) {
                Log.e("demo failed---->", "Directory not created");
            }
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream os = new FileOutputStream(file,true);
            os.write(pdfAsBytes);
            os.flush();
            os.close();
            Log.e("==", "11111111111");
        }catch (Exception e){e.printStackTrace();}

        Log.e("==", "2222222222222");
    }

}
