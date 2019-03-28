import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import io.vertx.core.Vertx;
import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.crypto.sodium.Signature;
import net.consensys.cava.io.Base64;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;
import net.consensys.cava.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient;
import net.consensys.cava.scuttlebutt.mux.RPCHandler;
import net.consensys.cava.scuttlebutt.rpc.RPCCodec;
import net.consensys.cava.scuttlebutt.rpc.RPCFlag;
import net.consensys.cava.scuttlebutt.rpc.RPCMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logl.Level;
import org.logl.LoggerProvider;
import org.logl.logl.SimpleLogger;
import org.logl.vertx.LoglLogDelegateFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;

@ExtendWith(VertxExtension.class)
public class PatchworkIntegrastionTest {


    @Test
    @Disabled
    public void testWithPatchwork(@VertxInstance Vertx vertx) throws Exception {
        Signature.KeyPair keyPair = getLocalKeys();
        String networkKeyBase64 = "1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s=";
        Bytes32 networkKeyBytes32 = Bytes32.wrap(Base64.decode(networkKeyBase64));

        String host = "localhost";
        int port = 8008;
        LoggerProvider loggerProvider = SimpleLogger.withLogLevel(Level.DEBUG).toPrintWriter(
                new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8))));
        LoglLogDelegateFactory.setProvider(loggerProvider);


        SecureScuttlebuttVertxClient secureScuttlebuttVertxClient =
                new SecureScuttlebuttVertxClient(loggerProvider, vertx, keyPair, networkKeyBytes32);

        AsyncResult<RPCHandler> onConnect =
                secureScuttlebuttVertxClient.connectTo(port, host, keyPair.publicKey(), RPCHandler::new);

        RPCHandler rpcHandler = onConnect.get();

        String rpcRequestBody = "{\"name\": [\"whoami\"],\"type\": \"async\",\"args\":[]}";

        RPCFlag.BodyType jsonType = RPCFlag.BodyType.BodyType.JSON;

        Bytes bytes = RPCCodec.encodeRequest(rpcRequestBody, jsonType);
        RPCMessage message = new RPCMessage(bytes);

        CompletableFuture<RPCMessage> rpcMessageCompletableFuture = rpcHandler.makeAsyncRequest(message);

        RPCMessage rpcMessage = rpcMessageCompletableFuture.get();

        String response = rpcMessage.asString();

        System.out.print(response);


    }


    // TODO: Move this to a utility class that all the scuttlebutt modules' tests can use.
    private Signature.KeyPair getLocalKeys() throws Exception {
        Optional<String> ssbDir = Optional.fromNullable(System.getenv().get("ssb_dir"));
        Optional<String> homePath =
                Optional.fromNullable(System.getProperty("user.home")).transform(home -> home + "/.ssb");

        Optional<String> path = ssbDir.or(homePath);

        if (!path.isPresent()) {
            throw new Exception("Cannot find ssb directory config value");
        }

        String secretPath = path.get() + "/secret";
        File file = new File(secretPath);

        if (!file.exists()) {
            throw new Exception("Secret file does not exist");
        }

        Scanner s = new Scanner(file, UTF_8.name());
        s.useDelimiter("\n");

        ArrayList<String> list = new ArrayList<String>();
        while (s.hasNext()) {
            String next = s.next();

            // Filter out the comment lines
            if (!next.startsWith("#")) {
                list.add(next);
            }
        }

        String secretJSON = String.join("", list);

        ObjectMapper mapper = new ObjectMapper();

        HashMap<String, String> values = mapper.readValue(secretJSON, new TypeReference<Map<String, String>>() {});
        String pubKey = values.get("public").replace(".ed25519", "");
        String privateKey = values.get("private").replace(".ed25519", "");

        Bytes pubKeyBytes = Base64.decode(pubKey);
        Bytes privKeyBytes = Base64.decode(privateKey);

        Signature.PublicKey pub = Signature.PublicKey.fromBytes(pubKeyBytes);
        Signature.SecretKey secretKey = Signature.SecretKey.fromBytes(privKeyBytes);

        return new Signature.KeyPair(pub, secretKey);
    }


}
