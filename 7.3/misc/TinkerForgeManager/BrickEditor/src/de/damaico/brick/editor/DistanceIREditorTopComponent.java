/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.damaico.brick.editor;

import com.tinkerforge.BrickletDistanceIR;
import com.tinkerforge.BrickletRotaryPoti;
import com.tinkerforge.Device;
import com.tinkerforge.IPConnection.TimeoutException;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
    dtd = "-//de.damaico.brick.editor//DistanceIREditor//EN",
autostore = false)
@TopComponent.Description(
    preferredID = "DistanceIREditorTopComponent",
//iconBase="SET/PATH/TO/ICON/HERE", 
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "de.damaico.brick.editor.DistanceIREditorTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
    displayName = "#CTL_DistanceIREditorAction",
preferredID = "DistanceIREditorTopComponent")
@Messages(
{
    "CTL_DistanceIREditorAction=DistanceIREditor",
    "CTL_DistanceIREditorTopComponent=DistanceIREditor Window",
    "HINT_DistanceIREditorTopComponent=This is a DistanceIREditor window"
})
public final class DistanceIREditorTopComponent extends TopComponent implements LookupListener
{
    
    Lookup.Result<BrickletDistanceIR> devices;    

    public DistanceIREditorTopComponent()
    {
        initComponents();
        setName(Bundle.CTL_DistanceIREditorTopComponent());
        setToolTipText(Bundle.HINT_DistanceIREditorTopComponent());
        Lookup.getDefault().lookupAll(Device.class);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        linear1 = new eu.hansolo.steelseries.gauges.Linear();

        linear1.setMaxValue(800.0);
        linear1.setThreshold(500.0);
        linear1.setTitle(org.openide.util.NbBundle.getMessage(DistanceIREditorTopComponent.class, "DistanceIREditorTopComponent.linear1.title")); // NOI18N
        linear1.setUnitString(org.openide.util.NbBundle.getMessage(DistanceIREditorTopComponent.class, "DistanceIREditorTopComponent.linear1.unitString")); // NOI18N

        javax.swing.GroupLayout linear1Layout = new javax.swing.GroupLayout(linear1);
        linear1.setLayout(linear1Layout);
        linear1Layout.setHorizontalGroup(
            linear1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 386, Short.MAX_VALUE)
        );
        linear1Layout.setVerticalGroup(
            linear1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 464, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(linear1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(linear1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private eu.hansolo.steelseries.gauges.Linear linear1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened()
    {
        //Register to the global context and declare your interest
        devices = Utilities.actionsGlobalContext().lookupResult(BrickletDistanceIR.class);
        
        devices.addLookupListener(this);
        
                startListening();
    }
    
    @Override
    public void componentClosed()
    {
        devices.removeLookupListener(this);
    }
    
    void writeProperties(java.util.Properties p)
    {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }
    
    void readProperties(java.util.Properties p)
    {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
    
    @Override
    public void resultChanged(LookupEvent ev)
    {
        startListening();
    }

    private void startListening()
    {
        for (Device device : devices.allInstances())
        {
            ((BrickletDistanceIR) device).setDistanceCallbackPeriod(10);
            ((BrickletDistanceIR) device).addListener(new BrickletDistanceIR.DistanceListener()
            {
                @Override
                public void distance(int distance)
                {
                    linear1.setValue(distance);
                }
            });
        }
    }
}
