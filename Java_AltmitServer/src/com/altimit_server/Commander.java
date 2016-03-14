 /**
 * Created by Zach on 10/20/15.
  * this file is just an example file with a method that can be called from a client
 */
 package com.altimit_server;

import com.altimit_server.util.AltimitConverter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Commander {

    @AltimitCmd
    public void test1(UUID sentUUID){
        DataOutputStream out = main.userMap.get(sentUUID);
        try {
            out.write(AltimitConverter.SendConversion("msg", "hi"));
            System.out.println("Sent back a message");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
