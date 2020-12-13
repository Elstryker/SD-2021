package Model;

import Server.Notifier;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// Informação que o Servidor tem de reter
public class Info {
    private Map<Tuple<Integer,Integer>, Set<String>> mapa; //a chave será a localização e o value será uma lista dos ids dos utilizadores que estão nessa mesma localização
    private Map<String,User> users; //todos os users no sistema
    private ReentrantReadWriteLock l;
    private ReentrantReadWriteLock.ReadLock rl;
    private ReentrantReadWriteLock.WriteLock wl;

    public Info() {
        mapa = new HashMap<>();
        users = new HashMap<>();
        l = new ReentrantReadWriteLock();
        rl = l.readLock();
        wl = l.writeLock();
    }

    public void updateCoords(Tuple<Integer,Integer> pos,String id) {
        try {
            wl.lock();
            Set<String> s;
            User user = users.get(id);
            if (user.getPosicao()!=null && mapa.containsKey(user.getPosicao())) {
                mapa.get(user.getPosicao()).remove(id);
            }
            if (mapa.containsKey(pos)) {
                s = mapa.get(pos);
            } else {
                s = new HashSet<>();
            }
            s.add(id);
            mapa.put(pos, s);
            user.setPosicao(pos);
            for (String userID : mapa.get(pos)) {
                if (!userID.equals(id)) {
                    user.addEncontro(userID);
                }
            }
        }
        finally {
            wl.unlock();
        }
    }

    public boolean addNewUser(String user,String pass) {
        try {
            wl.lock();
            if (users.containsKey(user)) {
                return false;
            } else {
                users.put(user, new User(user,pass,false));
            }
            return true;
        }
        finally {
            wl.unlock();
        }
    }

    public void addDOAtualToUser(String user,DataOutputStream dataOutput) {
        users.get(user).setdOatual(dataOutput);
    }

    public void removeDOAtualDoUser(String user) {
        users.get(user).setdOatual(null);
    }

    public void addInfetado(String user) throws IOException {
        try {
            wl.lock();
            User u = users.get(user);
            u.setInfetado();
            mapa.get(u.getPosicao()).remove(u.username);
            u.setPosicao(null);
            Thread th = new Thread(new Notifier(users,u.getEncontros(),"DEVES TER FICADO INFETADO, NAQUELE DIA FATÍDICO NÂO DEVIAS TER SAÍDO DE CASA"));
            th.start();
        } finally {
            wl.unlock();
        }
    }

    public boolean isPassCorreta(String userID,String pass) {
        try {
            wl.lock();
            return users.containsKey(userID) && users.get(userID).passwordCorreta(pass);
        }
        finally {
            wl.unlock();
        }
    }

    public int getNumOfPeopleOn(Tuple<Integer, Integer> pos) {
        int r=0;
        if (mapa.containsKey(pos)) r=mapa.get(pos).size();
        return r;
    }

    public boolean isInfetado(String userID) {
        return users.containsKey(userID) && users.get(userID).isInfetado();
    }

    public boolean emContactoComInfetado(String userID) {
        boolean r=false;
        for (String id : users.get(userID).getEncontros()) {
            if (isInfetado(id)) {
                r=true;
                break;
            }
        }
        return r;
    }



}