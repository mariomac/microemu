/*
 *  MicroEmulator
 *  Copyright (C) 2001-2003 Bartek Teodorczyk <barteo@it.pl>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package com.barteo.emulator.app;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import com.barteo.emulator.DisplayComponent;
import com.barteo.emulator.EmulatorContext;
import com.barteo.emulator.MIDletBridge;
import com.barteo.emulator.app.ui.ResponseInterfaceListener;
import com.barteo.emulator.app.ui.StatusBarListener;
import com.barteo.emulator.app.ui.awt.AwtDeviceComponent;
import com.barteo.emulator.app.ui.awt.AwtDialogWindow;
import com.barteo.emulator.app.ui.awt.AwtSelectDevicePanel;
import com.barteo.emulator.app.ui.awt.ExtensionFileFilter;
import com.barteo.emulator.app.ui.awt.FileChooser;
import com.barteo.emulator.app.ui.awt.OptionPane;
import com.barteo.emulator.app.util.DeviceEntry;
import com.barteo.emulator.app.util.ProgressJarClassLoader;
import com.barteo.emulator.device.DeviceFactory;
import com.barteo.emulator.device.j2se.J2SEDevice;


public class Awt extends Frame
{
	Awt instance = null;
  
	Common common;
  
	boolean initialized = false;
  
	AwtSelectDevicePanel selectDevicePanel = null;
	FileChooser fileChooser = null;
	MenuItem menuOpenJADFile;
	MenuItem menuOpenJADURL;
	MenuItem menuSelectDevice;
	    
	AwtDeviceComponent devicePanel;
	DeviceEntry deviceEntry;

	Label statusBar = new Label("Status");
  
	private EmulatorContext emulatorContext = new EmulatorContext()
	{
		ProgressJarClassLoader loader = new ProgressJarClassLoader();
    
		public ClassLoader getClassLoader()
		{
			return loader;
		}
    
		public DisplayComponent getDisplayComponent()
		{
			return devicePanel.getDisplayComponent();
		}    
	};
  
	KeyListener keyListener = new KeyListener()
	{    
		public void keyTyped(KeyEvent e)
		{
		}
    
		public void keyPressed(KeyEvent e)
		{
			devicePanel.keyPressed(e);
		}
    
		public void keyReleased(KeyEvent e)
		{
			devicePanel.keyReleased(e);
		}    
	};
   
	ActionListener menuOpenJADFileListener = new ActionListener()
	{
		public void actionPerformed(ActionEvent ev)
		{
			if (fileChooser == null) {
				ExtensionFileFilter fileFilter = new ExtensionFileFilter("JAD files");
				fileFilter.addExtension("jad");
				fileChooser = new FileChooser(instance, "Open JAD File...", FileDialog.LOAD);
				fileChooser.setFilenameFilter(fileFilter);
			}
      
			fileChooser.show();
						
			if (fileChooser.getFile() != null) {
				try {
					common.openJadFile(fileChooser.getSelectedFile().toURL());
				} catch (MalformedURLException ex) {
					System.err.println("Bad URL format " + fileChooser.getSelectedFile().getName());
				}
			}
		} 
	};
  
	ActionListener menuOpenJADURLListener = new ActionListener()
	{
		public void actionPerformed(ActionEvent ev)
		{
			String entered = OptionPane.showInputDialog(instance, "Enter JAD URL:");
			if (entered != null) {
				try {
					URL url = new URL(entered);
					common.openJadFile(url);
				} catch (MalformedURLException ex) {
					System.err.println("Bad URL format " + entered);
				}
			}
		}    
	};
  
	ActionListener menuExitListener = new ActionListener()
	{    
		public void actionPerformed(ActionEvent e)
		{
			System.exit(0);
		}    
	};
  
  
	ActionListener menuSelectDeviceListener = new ActionListener()
	{    
		public void actionPerformed(ActionEvent e)
		{
			if (AwtDialogWindow.show("Select device...", selectDevicePanel)) {
				if (selectDevicePanel.getSelectedDeviceEntry().equals(getDevice())) {
					return;
				}
				if (MIDletBridge.getCurrentMIDlet() != common.getLauncher()) {
/*					if (JOptionPane.showConfirmDialog(instance, 
							"Changing device needs MIDlet to be restarted. All MIDlet data will be lost. Are you sure?", 
							"Question?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != 0) {
						return;
					}*/
				}
				setDevice(selectDevicePanel.getSelectedDeviceEntry());

				if (MIDletBridge.getCurrentMIDlet() != common.getLauncher()) {
					try {
						MIDlet result = (MIDlet) MIDletBridge.getCurrentMIDlet().getClass().newInstance();
						common.startMidlet(result);
					} catch (Exception ex) {
						System.err.println(ex);
					}
				} else {
					common.startMidlet(common.getLauncher());
				}
			}
		}    
	};
	
	StatusBarListener statusBarListener = new StatusBarListener()
	{
		public void statusBarChanged(String text) 
		{
			statusBar.setText(text);
		}  
	};
  
	ResponseInterfaceListener responseInterfaceListener = new ResponseInterfaceListener()
	{
		public void stateChanged(boolean state) 
		{
			menuOpenJADFile.setEnabled(state);
			menuOpenJADURL.setEnabled(state);
			menuSelectDevice.setEnabled(state);
		}  
	};
  
	WindowAdapter windowListener = new WindowAdapter()
	{
		public void windowClosing(WindowEvent ev) 
		{
			menuExitListener.actionPerformed(null);
		}
		

		public void windowIconified(WindowEvent ev) 
		{
			MIDletBridge.getMIDletAccess(common.getLauncher().getCurrentMIDlet()).pauseApp();
		}
		
		public void windowDeiconified(WindowEvent ev) 
		{
			try {
				MIDletBridge.getMIDletAccess(common.getLauncher().getCurrentMIDlet()).startApp();
			} catch (MIDletStateChangeException ex) {
				System.err.println(ex);
			}
		}
	};  
 
  
	Awt()
	{
		instance = this;
    
		MenuBar menuBar = new MenuBar();
    
		Menu menuFile = new Menu("File");
    
		menuOpenJADFile = new MenuItem("Open JAD File...");
		menuOpenJADFile.addActionListener(menuOpenJADFileListener);
		menuFile.add(menuOpenJADFile);

		menuOpenJADURL = new MenuItem("Open JAD URL...");
		menuOpenJADURL.addActionListener(menuOpenJADURLListener);
		menuFile.add(menuOpenJADURL);
    
		menuFile.addSeparator();
    
		MenuItem menuItem = new MenuItem("Exit");
		menuItem.addActionListener(menuExitListener);
		menuFile.add(menuItem);
    
		Menu menuOptions = new Menu("Options");
    
		menuSelectDevice = new MenuItem("Select device...");
		menuSelectDevice.addActionListener(menuSelectDeviceListener);
		menuOptions.add(menuSelectDevice);

		menuBar.add(menuFile);
		menuBar.add(menuOptions);
		setMenuBar(menuBar);
    
		setTitle("MicroEmulator");
		addWindowListener(windowListener);
		
    
		Config.loadConfig();
		addKeyListener(keyListener);

		devicePanel = new AwtDeviceComponent();
		selectDevicePanel = new AwtSelectDevicePanel();
		setDevice(selectDevicePanel.getSelectedDeviceEntry());
    
		common = new Common(emulatorContext);
		common.setStatusBarListener(statusBarListener);
		common.setResponseInterfaceListener(responseInterfaceListener);

		add(devicePanel, "Center");
		add(statusBar, "South");    

		initialized = true;
	}
      
  
	public DeviceEntry getDevice()
	{
		return deviceEntry;
	}
  
  
	public void setDevice(DeviceEntry entry)
	{
		ProgressJarClassLoader loader = (ProgressJarClassLoader) emulatorContext.getClassLoader();
		try {
			Class deviceClass = null;
			if (entry.getFileName() != null) {
				loader.addRepository(
						new File(Config.getConfigPath(), entry.getFileName()).toURL());
				deviceClass = loader.findClass(entry.getClassName());
			} else {
				deviceClass = Class.forName(entry.getClassName());
			}
			J2SEDevice device = (J2SEDevice) deviceClass.newInstance();
			DeviceFactory.setDevice(device);
			device.init(emulatorContext);
			devicePanel.init();
			this.deviceEntry = entry;
			Image tmpImg = device.getNormalImage();
			Dimension size = new Dimension(tmpImg.getWidth(null), tmpImg.getHeight(null));
			size.width += 10;
			size.height += statusBar.getPreferredSize().height + 55;
			setSize(size);
			doLayout();
		} catch (MalformedURLException ex) {
			System.err.println(ex);          
		} catch (ClassNotFoundException ex) {
			System.err.println(ex);          
		} catch (InstantiationException ex) {
			System.err.println(ex);          
		} catch (IllegalAccessException ex) {
			System.err.println(ex);          
		}
	}
  
  
	public static void main(String args[])
	{    
		Awt app = new Awt();
		MIDlet m = null;

		if (args.length > 0) {
			Class midletClass;
			try {
				midletClass = Class.forName(args[0]);
				m = app.common.loadMidlet("MIDlet", midletClass);
			} catch (ClassNotFoundException ex) {
				System.out.println("Cannot find " + args[0] + " MIDlet class");
			}
		} else {
			m = app.common.getLauncher();
		}
    
		if (app.initialized) {
			if (m != null) {
				app.common.startMidlet(m);
			}
			app.validate();
			app.setVisible(true);
		} else {
			System.exit(0);
		}
	}

}