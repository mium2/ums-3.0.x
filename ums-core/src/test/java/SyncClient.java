import kr.uracle.ums.tcppitcher.codec.parser.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SyncClient {

    public static void main(String[] args) throws Exception{
        try {
            String charSet = "UTF-8";
            Socket clientSocket = new Socket("localhost", 8080);
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());

            Utils.buildProtocolJson();
            List<Map<String,String>> PROTOCOL_HEAD_LIST = Utils.PROTOCOL_HEAD_LIST;
            // 고정된 헤더 사이즈
            int staticHeadSize = 30;

            ///////////////////////////////////////////////////////////////////
            // 보낼 메세지 처리
            ///////////////////////////////////////////////////////////////////
            // STEP 1 : 고정 된 헤더메세지 만든다.
            byte[] outHeadBytes = new byte[staticHeadSize];

            // STEP 2 : 헤더메세지를 만든다.
            String msgType = "01"; //
            msgType = String.format("-%s2",msgType);
            out.write(msgType.getBytes(charSet));

            // STEP 3 : 바디메세지를 만든다.
            String sendMsg = "안녕하세요. 유라클 입니다.";
            out.writeInt(sendMsg.getBytes(charSet).length); // write length of the message
            out.write(sendMsg.getBytes(charSet));           // write the message

            ///////////////////////////////////////////////////////////////////
            // 받은 메세지 처리
            ///////////////////////////////////////////////////////////////////

            //STEP 4 : 고정 헤더사이즈 만큼 읽어 메세지 셋팅
            byte[] inHeadBytes = new byte[staticHeadSize];
            for(int i = 0; i < inHeadBytes.length; i++) {
                inHeadBytes[i] = in.readByte();
            }

            //STEP 5 : 헤더 정보 중 바디메세지 길이를 이용하여 나머지 데이타 수신처리
            int remainLen = 3490;
            byte[] inBodyBtyes = new byte[remainLen];

            for(int i = 0; i < inBodyBtyes.length; i++) {
                inBodyBtyes[i] = in.readByte();
            }

            // TODO : 해당 정보를 이용한 비즈니스 로직 처리


        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
