package com.altimit_server.util;

import jdk.nashorn.internal.runtime.Debug;

import javax.swing.*;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by Zach on 10/21/15.
 */
public class AltimitConverter {

    //Gets a list of class types of the paramaters provided
    public static Class[] GetParameterClasses(Object[] paramaters){
        Class[] endParameters = new Class[paramaters.length];

        for(int i = 0; i < paramaters.length; i ++){
            endParameters[i] = paramaters[i].getClass();
        }
        return endParameters;
    }

    //Creates a byte array of the message that will be sent.
    //The first 4 bytes is the end size of the whole message
    //The last 4 bytes is the message key used to identify the end of a message and later to be used for a security purpose
    public static byte[] SendConversion(String methodName, Object... args){
        List<byte[]> byteList = new ArrayList<>();

        byte[] byteArray = null;
        if(methodName != null || methodName != "") {
            byte[] method = convertToByteArray(methodName);
            byteList.add(method);

            Integer size = method.length;
            String type = "";

            for (Object params : args) {
                type = params.getClass().getTypeName();
                byte[] currentArr = null;
                switch (type) {
                    case "java.lang.Character":
                        char castChar = (char) params;
                        currentArr = convertToByteArray(castChar);
                        break;
                    case "java.lang.Integer":
                        int castInt = (Integer) params;
                        currentArr = convertToByteArray(castInt);
                        break;
                    case "java.lang.Long":
                        long castLong = (long) params;
                        currentArr = convertToByteArray(castLong);
                        break;
                    case "java.lang.Short":
                        short castShort = (short) params;
                        currentArr = convertToByteArray(castShort);
                        break;
                    case "java.lang.Float":
                        float castFloat = (float) params;
                        currentArr = convertToByteArray(castFloat);
                        break;
                    case "java.lang.Double":
                        double castDouble = (double) params;
                        currentArr = convertToByteArray(castDouble);
                        break;
                    case "java.lang.Boolean":
                        boolean castBoolean = (boolean) params;
                        currentArr = convertToByteArray(castBoolean);
                        break;
                    case "java.lang.String":
                        String castString = (String) params;
                        currentArr = convertToByteArray(castString);
                        break;
                    default:
                        System.out.println("No supported type for this variable");
                        break;
                }

                size += currentArr.length;
                byteList.add(currentArr);
            }

            byteArray = new byte[size];
            Integer currentIndex = 0;
            for (byte[] byteArr : byteList) {
                System.arraycopy(byteArr, 0, byteArray, currentIndex, byteArr.length);
                currentIndex += byteArr.length;
            }
        }

        return byteArray;
    }

    //Converts a Char to a btye array
    public static byte[] convertToByteArray(char value) {
        ByteBuffer buffer = ByteBuffer.allocate(3);
        buffer.put((byte)1);
        buffer.position(1);
        buffer.putChar(value);
        return buffer.array();
    }

    //Converts a Int to a btye array
    public static byte[] convertToByteArray(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put((byte)2);
        buffer.position(1);
        buffer.putInt(value);
        return buffer.array();
    }

    //Converts a Long to a btye array
    public static byte[] convertToByteArray(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put((byte)3);
        buffer.position(1);
        buffer.putLong(value);
        return buffer.array();
    }

    //Converts a Short to a btye array
    public static byte[] convertToByteArray(short value) {
        ByteBuffer buffer = ByteBuffer.allocate(3);
        buffer.put((byte)4);
        buffer.position(1);
        buffer.putShort(value);
        return buffer.array();
    }

    //Converts a Float to a btye array
    public static byte[] convertToByteArray(float value) {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put((byte)5);
        buffer.position(1);
        buffer.putFloat(value);
        return buffer.array();
    }

    //Converts a Double to a btye array
    public static byte[] convertToByteArray(double value) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put((byte)6);
        buffer.position(1);
        buffer.putDouble(value);
        return buffer.array();
    }

    //Converts a String to a btye array
    public static byte[] convertToByteArray(String value) {
        Integer strLength = value.length();
        ByteBuffer buffer = ByteBuffer.allocate(strLength + 5);
        buffer.put((byte)7);
        buffer.position(1);
        buffer.putInt(strLength);
        buffer.position(5);
        buffer.put(value.getBytes());
        return buffer.array();

    }

    //Converts a Boolean to a btye array
    public static byte[] convertToByteArray(boolean value) {
        byte[] array = new byte[2];
        array[0] = 8;
        array[1] = (byte)(value == true ? 1 : 0);
        return array;
    }

    //This is used to convert the byte array recieved from a client into the method name and paramaters for a method call
    public static List<Object> ReceiveConversion(byte[] array){
        //String methodName = mapMethod.get(array[0]);
        List<Object> paramaters = new ArrayList<>();
        for(int i = 0; i < array.length; i++){
            byte[] ar = null;
            Integer size = 0;
            Integer current = i;
            switch (array[i]){
                case 1:
                    paramaters.add(convertToChar(Arrays.copyOfRange(array, i+1, i+3)));
                    i += 2;
                    break;
                case 2:
                    paramaters.add(convertToInteger(Arrays.copyOfRange(array, i+1, i+5)));
                    i += 4;
                    break;
                case 3:
                    paramaters.add(convertToLong(Arrays.copyOfRange(array, i+1, i+9)));
                    i += 8;
                    break;
                case 4:
                    paramaters.add(convertToShort(Arrays.copyOfRange(array, i+1, i+3)));
                    i += 2;
                    break;
                case 5:
                    paramaters.add(convertToFloat(Arrays.copyOfRange(array, i+1, i+5)));
                    i += 4;
                    break;
                case 6:
                    paramaters.add(convertToDouble(Arrays.copyOfRange(array, i+1, i+9)));
                    i += 8;
                    break;
                case 7:
                    int length = convertToInteger(Arrays.copyOfRange(array, i+1, i+5));
                    i += 5;
                    try {
                        paramaters.add(new String(Arrays.copyOfRange(array, i, i + length), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    i += length - 1;
                    break;
                case 8:
                    paramaters.add(convertToBoolean(array[i+1]));
                    i += 1;
                    break;
            }
        }

        return paramaters;
    }

    //Converts a byte array to a Char
    public static char convertToChar(byte[] array){
        return ByteBuffer.wrap(array).getChar();
    }

    //Converts a byte array to a Int
    public static Integer convertToInteger(byte[] array){
        return new BigInteger(array).intValue();
    }

    //Converts a byte array to a Long
    public static long convertToLong(byte[] array){
        return ByteBuffer.wrap(array).getLong();
    }

    //Converts a byte array to a Short
    public static short convertToShort(byte[] array){
        return ByteBuffer.wrap(array).getShort();
    }

    //Converts a byte array to a Float
    public static float convertToFloat(byte[] array){
        return ByteBuffer.wrap(array).getFloat();
    }

    //Converts a byte array to a Double
    public static double convertToDouble(byte[] array){
        return ByteBuffer.wrap(array).getDouble();
    }

    //Converts a byte array to a String
    public static String convertToString(byte[] array){
        return ByteBuffer.wrap(array).toString();
    }

    //Converts a byte array to a Boolean
    public static boolean convertToBoolean(byte array){
        return (Byte.toString(array) == "1");
    }
}
