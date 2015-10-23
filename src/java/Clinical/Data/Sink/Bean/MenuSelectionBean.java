/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

/**
 *
 * @author taywh
 */

@ManagedBean (name="menuSelectionBean")
@RequestScoped
public class MenuSelectionBean implements Serializable{
    @ManagedProperty("#{param.command}")
    String command;
    
    public MenuSelectionBean() {}
    
    public String gexPipelineIllumina() {
        System.out.println("Command receive from main: " + command);
        return Constants.NGS_PAGE;
    }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    
    
}
