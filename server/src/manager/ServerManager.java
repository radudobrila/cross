package manager;

import auth.Login;
import RMI.Register;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ServerManager {
    private static final ConcurrentHashMap<String, Long> loggedUsers = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 100 * 60 * 1000;

    public static int handleLogin(String username, String password, String clientIP, int udpPort) {
        try {
            int code = Login.login(username, password, loggedUsers.keySet());
            if (code == 100) {
                System.out.println("User logged in successfully");
                loggedUsers.put(username, System.currentTimeMillis());
                UdpSessionManager.registerClientUdpInfo(username, clientIP, udpPort);
            }
            if (code == 101) {
                System.out.println("Username or password incorrect");
            }
            if (code == 102) {
                System.out.println("User already logged in");
            }
            if (code == 103) {
                System.out.println("Error logging in");
            }
            return code;
        } catch (Exception e) {
            System.err.println("Errore durante l'handleLogin per " + username + ": " + e.getMessage());
            e.printStackTrace();
            return 103;
        }
    }

    public static int handleUpdateCredentials(String username, String password, String newPassword) {
        try {
            int result = Register.updatePassword(username, password, newPassword);
            if (result == 100) {
                System.out.println("Password updated successfully");
            }
            if (result == 101) {
                System.out.println("Invalid new password");
            }
            if (result == 102) {
                System.out.println("Username or password incorrect");
            }
            if (result == 103) {
                System.out.println("You cannot use the same password");
            }
            return result;
        } catch (IOException e) {
            System.err.println("Errore durante l'handleUpdateCredentials per " + username + ": " + e.getMessage());
            e.printStackTrace();
            return 104;
        }
    }

    public static boolean isLogged(String username) {
        Long lastActive = loggedUsers.get(username);
        if (lastActive == null) return false;

        long now = System.currentTimeMillis();
        if (now - lastActive > TIMEOUT) {
            loggedUsers.remove(username);
            UdpSessionManager.removeClientUdpInfo(username);
            return false;
        }
        return true;
    }

    public static void updateActivity(String username) {
        loggedUsers.put(username, System.currentTimeMillis());
    }

    public static int logout(String username) {
        loggedUsers.remove(username);
        UdpSessionManager.removeClientUdpInfo(username);
        System.out.println("User " + username + " logged out successfully.");
        return 100;
    }

    public static int handleRegisterMulticastInterest(String username, int multicastPort) {
        return UdpSessionManager.handleMulticastInterest(username, multicastPort);
    }

}