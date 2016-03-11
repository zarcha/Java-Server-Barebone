package com.altimit_server; /**
 * Created by Zach on 10/20/15.
 */

import com.altimit_server.util.AltimitConverter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Commander {

    @AltimitCmd
    public void test1(String userUUID){
        DataOutputStream out = main.userMap.get(UUID.fromString(userUUID));
        try {
            out.write(AltimitConverter.SendConversion("msg", "hi"));
            System.out.println("Sent back a message");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
