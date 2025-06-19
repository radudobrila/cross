package RMI;

import auth.BCrypt;
import auth.DataUser;
import auth.FileManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class Register extends UnicastRemoteObject implements RegisterInterface {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FILE_NAME = "users.json";

    public Register() throws RemoteException {
        super();
    }

    @Override
    public int Register(String username, String password) {
        try {
            HashMap<String, DataUser> users = FileManager.loadUsers();

            if (password.length() < 2) {
                return 101;
            }
            if (users.containsKey(username)) {
                return 102;
            }

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
            users.put(username, new DataUser(hashedPassword));

            FileManager.saveUsers(users);
            return 100;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update Credential function
     *
     * @param username    client username
     * @param oldPassword old password
     * @param newPassword new password
     * @return 100 -> OK, 101 -> Invalid new Password, 102 -> Invalid Username or old password, 103 -> New Password equals old password
     * @throws IOException
     */
    public static int updatePassword(String username, String oldPassword, String newPassword) throws IOException {
        HashMap<String, DataUser> users = FileManager.loadUsers();

        if ((!users.containsKey(username)) || !BCrypt.checkpw(oldPassword, users.get(username).getHashedPassword())) {
            return 102;
        }

        if (newPassword.equals(oldPassword)) {
            return 103;
        }
        if (newPassword.length() < 2) {
            return 101;
        }

        String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        users.put(username, new DataUser(newHashedPassword));

        FileManager.saveUsers(users);
        return 100;
    }

}
