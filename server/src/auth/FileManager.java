package auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class FileManager {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static final String FILE_NAME = "users.json";


    private static final Object FILE_LOCK = new Object();

    /**
     * @return a hashmap with all the users registred
     * @throws IOException If an I/O error occurs during file reading.
     */
    public static HashMap<String, DataUser> loadUsers() throws IOException {

        synchronized (FILE_LOCK) {
            File file = new File(FILE_NAME);
            if (file.exists() && file.length() > 0) {
                try {
                    return objectMapper.readValue(file, new TypeReference<HashMap<String, DataUser>>() {
                    });
                } catch (IOException e) {
                    System.err.println("Errore durante il caricamento degli utenti da " + FILE_NAME + ": " + e.getMessage());
                    throw e;
                }
            } else {
                return new HashMap<>();
            }
        }
    }

    /**
     * Saves users in json file
     *
     * @param users The HashMap of users to save.
     * @throws IOException If an I/O error occurs during file writing.
     */
    public static void saveUsers(HashMap<String, DataUser> users) throws IOException {
        synchronized (FILE_LOCK) {
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_NAME), users);
            } catch (IOException e) {
                System.err.println("Errore durante il salvataggio degli utenti in " + FILE_NAME + ": " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Extract from Json file user's password
     *
     * @param json The JSON string to extract the password from.
     * @return HashedPassword, or null if not found.
     */
    private static String extractPasswordFromJson(String json) {
        String passwordKey = "\"hashedPassword\":\"";
        int startIndex = json.indexOf(passwordKey);
        if (startIndex == -1) {
            return null;
        }
        startIndex += passwordKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return null;
        }
        return json.substring(startIndex, endIndex);
    }
}