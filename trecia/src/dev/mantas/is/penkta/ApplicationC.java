package dev.mantas.is.penkta;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Scanner;

public class ApplicationC {

    private static String payloadText;
    private static byte[] payloadPublicKey;
    private static byte[] payloadSignature;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter command line input, type 'help' for a list of commands.");

        while (scanner.hasNextLine()) {

            String rawCommand = scanner.nextLine();
            String[] split = rawCommand.split(" ");

            String command = split[0];
            String[] arguments = split.length == 1 ? new String[0] : Arrays.copyOfRange(split, 1, split.length);

            switch (command.toLowerCase(Locale.ROOT)) {
                case "help" -> commandHelp();
                case "fetch" -> fetchCommand(arguments);
                case "exit" -> System.exit(0);
            }

        }
    }

    private static void commandHelp() {
        System.out.println("Available commands:");
        System.out.println("  fetch - receive and verify message payload from the dispatch service");
        System.out.println("  exit - exits the application");
    }

    private static void fetchCommand(String[] args) throws Exception {
        Socket socket;

        try {
            socket = SocketFactory.getDefault().createSocket("localhost", 10000);
            System.out.println("Connected to dispatch service!");
        } catch (Exception ex) {
            System.err.println("Could not connect to dispatch service.");
            return;
        }

        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        os.writeByte(1); // inform server we are the verifier
        os.flush();

        DataInputStream is = new DataInputStream(socket.getInputStream());
        boolean available = is.readBoolean();

        if (!available) {
            System.err.println("Message payload not available yet");
            socket.close();
        }

        payloadText = is.readUTF();
        payloadPublicKey = new byte[is.readInt()];
        is.read(payloadPublicKey);
        payloadSignature = new byte[is.readInt()];
        is.read(payloadSignature);

//        System.out.println("Payload text: '" + payloadText + "'");
//        System.out.println("Public key: " + Arrays.toString(payloadPublicKey) + " (" + payloadPublicKey.length + ")");
//        System.out.println("Signature: " + Arrays.toString(payloadSignature) + " (" + payloadSignature.length + ")");

        socket.close();

        System.out.println("Message payload received, verifying...");

        try {
            Signature signature = Signature.getInstance("SHA256WithRSA");
            signature.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(payloadPublicKey)));
            signature.update(payloadText.getBytes(StandardCharsets.UTF_8));
            boolean verified = signature.verify(payloadSignature);

            if (verified) {
                System.out.println("Payload verified: '" + payloadText + "'");
            } else {
                System.err.println("Message payload has been tampered with! (1)");
            }
        } catch (Exception ex) {
            System.err.println("Message payload has been tampered with! (2)");
            ex.printStackTrace();
        }
    }

}
