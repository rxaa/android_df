package net.rxaa.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Crypt {

    /**
     * SHA2加密
     */
    public static String getSHA2(String strSrc) {
        try {
            // 将此换成SHA-1、SHA-512、SHA-384等参数
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(strSrc.getBytes());
            // to HexString
            return bytes2Hex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String getMD5(String info) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(info.getBytes(StandardCharsets.UTF_8));
            return bytes2Hex(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }


    /**
     * byte数组转换为16进制字符串
     *
     * @param bts 数据源
     * @return 16进制字符串
     */
    public static String bytes2Hex(byte[] bts) {
        String des = "";
        String tmp = null;
        for (int i = 0; i < bts.length; i++) {
            tmp = (Integer.toHexString(bts[i] & 0xFF));
            if (tmp.length() == 1) {
                des += "0";
            }
            des += tmp;
        }
        return des;
    }
}
