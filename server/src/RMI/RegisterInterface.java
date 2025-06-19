package RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegisterInterface extends Remote {
    int Register(String username, String password) throws RemoteException;
    
}
