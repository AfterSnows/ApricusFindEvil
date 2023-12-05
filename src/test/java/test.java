import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class test {
    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        System.out.println("running JVM start ");
        String pid="19884";
        VirtualMachine vm = VirtualMachine.attach(pid);
        Path agentPath = Paths.get("D:\\WorkSpace\\java\\ApricusFindEvil\\target\\ApricusFindEvil-1.0-SNAPSHOT-jar-with-dependencies.jar");
        String path = agentPath.toAbsolutePath().toString();
        vm.loadAgent(path);
        vm.detach();


    }
}
 //    curl "http://localhost:2333/cc" --data-binary "D:\java project collection\HardWork\cc11Step2.ser"
