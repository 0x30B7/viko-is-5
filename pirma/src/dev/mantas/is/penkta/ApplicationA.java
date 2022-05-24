package dev.mantas.is.penkta;

import javax.net.SocketFactory;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class ApplicationA {

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
                case "text" -> commandText(arguments);
                case "sign" -> commandSign(arguments);
                case "send" -> commandSend(arguments);
                case "exit" -> System.exit(0);
            }

        }
    }

    private static void commandHelp() {
        System.out.println("Available commands:");
        System.out.println("  text <message payload> - input the text payload");
        System.out.println("  sign - signs the input text payload");
        System.out.println("  send - sends the input & signed text payload");
        System.out.println("  exit - exits the application");
    }

    private static void commandText(String[] args) {
        if (args.length == 0) {
            System.err.println("You must enter a non-empty text message");
            return;
        }

        payloadText = String.join(" ", args);
        System.out.println("Message payload updated to '" + payloadText + "'");
    }

    private static void commandSign(String[] args) throws Exception {
        if (payloadText == null) {
            System.err.println("You must first set the message payload");
            return;
        }

        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(1024);

        KeyPair keyPair = keyPairGen.generateKeyPair();
        Signature signature = Signature.getInstance("SHA256WithRSA");

        byte[] data = payloadText.getBytes(StandardCharsets.UTF_8);

        signature.initSign(keyPair.getPrivate());
        signature.update(data);
        payloadSignature = signature.sign();

        payloadPublicKey = keyPair.getPublic().getEncoded();
        System.out.println("Message payload signed and ready for transfer");
    }

    private static void commandSend(String[] args) throws Exception {
        if (payloadText == null) {
            System.err.println("You must first set the message payload");
            return;
        }

        if (payloadSignature == null) {
            System.err.println("You must first sign the message payload");
            return;
        }

        Socket socket;

        try {
            socket = SocketFactory.getDefault().createSocket("localhost", 10000);
            System.out.println("Connected to dispatch service!");
        } catch (Exception ex) {
            System.err.println("Could not connect to dispatch service.");
            return;
        }

        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeByte(0); // inform server we are the signer

        out.writeUTF(payloadText);
        out.writeInt(payloadPublicKey.length);
        out.write(payloadPublicKey);
        out.writeInt(payloadSignature.length);
        out.write(payloadSignature);
        out.flush();

        socket.close();
        System.out.println("Payload sent!");
    }


}
