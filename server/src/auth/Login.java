package auth;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Login {
    private static final String FILE_NAME = "users.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Login function
     *
     * @param username    client's username
     * @param password    password string.length >= 2
     * @param loggedUsers A Set of currently logged-in usernames. This Set is expected to be thread-safe (e.g., from ConcurrentHashMap.keySet()).
     * @return 100 -> OK, 101 -> username/password error, 102 -> Already logged, 103 -> Other
     */
    public static int login(String username, String password, Set<String> loggedUsers) {
        try {
            HashMap<String, DataUser> users = FileManager.loadUsers();

            if (users.containsKey(username)) {
                DataUser userData = users.get(username);
                String storedHashedPassword = userData.getHashedPassword();

                if (BCrypt.checkpw(password, storedHashedPassword)) {
                    if (loggedUsers.contains(username)) {
                        return 102;
                    }
                    return 100;
                } else {
                    System.out.println("Password errata per l'utente: " + username);
                    return 101;
                }
            } else {
                System.out.println("Utente non trovato: " + username);
                return 101;
            }
        } catch (IOException e) {
            System.err.println("Errore di I/O durante il login per " + username + ": " + e.getMessage());
            throw new RuntimeException("Errore durante il caricamento degli utenti per il login.", e);
        } catch (Exception e) {
            System.err.println("Errore inatteso durante il login per " + username + ": " + e.getMessage());
            e.printStackTrace();
            return 103;
        }
    }

    /**
     * Logout function
     *
     * @param username    username to remove
     * @param loggedUsers list of all usernames logged. This Set is expected to be thread-safe.
     * @return 100 -> OK, 101 -> Error (user not found in logged list)
     */
    public static int logout(String username, Set<String> loggedUsers) {
        if (loggedUsers.contains(username)) {
            loggedUsers.remove(username);
            System.out.println("Utente " + username + " disconnesso con successo.");
            return 100;
        }
        System.out.println("Errore: Utente " + username + " non trovato tra gli utenti loggati.");
        return 101;
    }
}