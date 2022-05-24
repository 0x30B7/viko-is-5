package dev.mantas.is.penkta;

import javax.net.ServerSocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class ApplicationB {

    private static ServerSocket socket;

    private static String nextMessagePayload;
    private static byte[] nextPublicKey;
    private static byte[] nextPayloadSignature;

    public static void main(String[] args) throws Exception {
        socket = ServerSocketFactory.getDefault().createServerSocket(10000);

        new Thread(() -> {

            try {
                Socket nextClient;
                while ((nextClient = socket.accept()) != null) {

                    DataInputStream is = new DataInputStream(nextClient.getInputStream());
                    byte type = is.readByte();

                    if (type == 0) { // signer
                        System.out.println("Receiving data from the signer");

                        nextMessagePayload = is.readUTF();
                        nextPublicKey = new byte[is.readInt()];
                        is.read(nextPublicKey);
                        nextPayloadSignature = new byte[is.readInt()];
                        is.read(nextPayloadSignature);
                    } else if (type == 1) { // verifier
                        DataOutputStream os = new DataOutputStream(nextClient.getOutputStream());

                        if (nextMessagePayload == null) {
                            System.out.println("Informing verifier that no data is available yet");

                            os.writeBoolean(false);
                            os.flush();
                        } else {
                            System.out.println("Dispatching data to verifier");

                            os.writeBoolean(true);
                            os.writeUTF(nextMessagePayload);
                            os.writeInt(nextPublicKey.length);
                            os.write(nextPublicKey);
                            os.writeInt(nextPayloadSignature.length);
                            os.write(nextPayloadSignature);
                            os.flush();

                            nextMessagePayload = null;
                            nextPublicKey = null;
                            nextPayloadSignature = null;
                        }
                    }

                    nextClient.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }, "Socket Thread").start();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter command line input, type 'help' for a list of commands.");

        while (scanner.hasNextLine()) {

            String rawCommand = scanner.nextLine();
            String[] split = rawCommand.split(" ");

            String command = split[0];
            String[] arguments = split.length == 1 ? new String[0] : Arrays.copyOfRange(split, 1, split.length);

            switch (command.toLowerCase(Locale.ROOT)) {
                case "help" -> commandHelp();
                case "available" -> commandAvailable(arguments);
                case "view" -> commandView(arguments);
                case "text" -> commandText(arguments);
                case "key" -> commandKey(arguments);
                case "signature" -> commandSignature(arguments);
                case "exit" -> System.exit(0);
            }

        }
    }

    private static void commandView(String[] args) {
        if (nextMessagePayload == null) {
            System.err.println("Message payload not yet received from signer");
            return;
        }

        System.out.println("Payload text: '" + nextMessagePayload + "'");
        System.out.println("Public key: " + Arrays.toString(nextPublicKey) + " (" + nextPublicKey.length + ")");
        System.out.println("Signature: " + Arrays.toString(nextPayloadSignature) + " (" + nextPayloadSignature.length + ")");
    }

    private static void commandHelp() {
        System.out.println("Available commands:");
        System.out.println("  available - check if a message payload has been sent by the signer");
        System.out.println("  view - view the current message payload received from the signer");
        System.out.println("  text <message payload> - update the message payload text");
        System.out.println("  key <index> <new byte> - update the message payload public key");
        System.out.println("  signature <index> <new byte> - update the message payload signature");
        System.out.println("  exit - exits the application");
    }

    private static void commandAvailable(String[] args) {
        if (nextMessagePayload == null) {
            System.out.println("Message payload has NOT been received from signer");
        } else {
            System.out.println("Message payload has been received from signer");
        }
    }

    private static void commandText(String[] args) {
        if (nextMessagePayload == null) {
            System.err.println("Message payload not yet received from signer");
            return;
        }

        nextMessagePayload = String.join(" ", args);
        System.out.println("Updated message payload text");
    }

    private static void commandKey(String[] args) {
        if (nextPublicKey == null) {
            System.err.println("Message payload not yet received from signer");
            return;
        }

        int index = Integer.parseInt(args[0]);
        byte newByte = Byte.parseByte(args[1]);

        byte oldByte = nextPublicKey[index];
        nextPublicKey[index] = newByte;

        System.out.println("Updating public key byte '" + index + "' (" + oldByte + " -> " + newByte + ")");
    }

    private static void commandSignature(String[] args) {
        if (nextPayloadSignature == null) {
            System.err.println("Message payload not yet received from signer");
            return;
        }

        int index = Integer.parseInt(args[0]);
        byte newByte = Byte.parseByte(args[1]);

        byte oldByte = nextPayloadSignature[index];
        nextPayloadSignature[index] = newByte;

        System.out.println("Updating signature byte '" + index + "' (" + oldByte + " -> " + newByte + ")");
    }

}
