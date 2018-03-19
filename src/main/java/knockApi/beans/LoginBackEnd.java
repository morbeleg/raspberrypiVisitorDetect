package knockApi.beans;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import java.io.IOException;

@ManagedBean
public class LoginBackEnd {


    private String accountName;
    private String password;
    private boolean currentLoginStatus;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private Boolean login() {
        if (accountName.equals("admin") && password.equals("admin")) {
            System.out.println("Login successful");
            currentLoginStatus = true;
            return true;
        }
        if (accountName.equals("admin") && password.equals("admin!")) {
            System.out.println("Restarting");
            try {
                Runtime.getRuntime().exec("sudo reboot");
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

        }
        return false;
    }
    public void welcomeMessage() {
        FacesContext context = FacesContext.getCurrentInstance();
        String welcomeMessage;
        if (currentLoginStatus) {
            welcomeMessage = "welcome Aboard Caption";
            context.addMessage(null, new FacesMessage("Successful", "Your message: " + welcomeMessage));
        } else {
            welcomeMessage = "Login Failed";
            context.addMessage(null, new FacesMessage("Login Failed", welcomeMessage));
        }
    }

    public String loginRedirect() {
        if (currentLoginStatus = login()) {
            return "MainPage.xhtml";
        }
        return "";
    }

}
