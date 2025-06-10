import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

public class Main {
    private static JPanel folderPanel;
    private static JLabel folderPathLabel;
    private static Stack<File> folderHistory = new Stack<>();
    private static File copiedFile = null;
    private static File currentFolder = null;
    private static File currentDirectory = new File(System.getProperty("user.home"));

	public static void main(String[] args) {
        JFrame frame = new JFrame("My File Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        folderPathLabel = new JLabel("No folder imported.");
        frame.add(folderPathLabel, BorderLayout.NORTH);

        folderPanel = new JPanel();
        folderPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 15, 15));
        JScrollPane scrollPane = new JScrollPane(folderPanel);

        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 0));
        outerPanel.add(scrollPane, BorderLayout.CENTER);
        
        outerPanel.add(scrollPane, BorderLayout.CENTER);
        frame.add(outerPanel, BorderLayout.CENTER);
        
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu helpMenu = new JMenu("Help");
        JMenu searchMenu = new JMenu("Search");
        
        JMenuItem newFileItem = new JMenuItem("New File");
        JMenuItem newFolderItem = new JMenuItem("New Folder");
        JMenuItem importItem = new JMenuItem("Import Folder");
        JMenuItem backItem = new JMenuItem("Back");
        JMenuItem exitItem = new JMenuItem("Exit");
        
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem deleteItem = new JMenuItem("Delete");
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem pasteItem = new JMenuItem("Paste");
        
        JMenuItem aboutItem = new JMenuItem("About");
        helpMenu.add(aboutItem);
        
        JMenuItem searchFolderItem = new JMenuItem("Search Folder");
        searchMenu.add(searchFolderItem);

        importItem.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = chooser.getSelectedFile();

                displayFolderContents(selectedFolder);

                saveFolderPath(selectedFolder.getAbsolutePath());
            }
        });
        
        backItem.addActionListener(e -> {
            if (folderHistory.size() > 1) {
                folderHistory.pop(); 
                File previousFolder = folderHistory.peek();
                displayFolderContents(previousFolder);
            } else {
                JOptionPane.showMessageDialog(null, "No previous folder.");
            }
        });
        exitItem.addActionListener(e -> System.exit(0));
        
        renameItem.addActionListener(e -> {
            if (currentFolder == null) return;

            String name = JOptionPane.showInputDialog("Enter name of file/folder to rename:");
            if (name == null || name.trim().isEmpty()) return;

            File target = new File(currentFolder, name);
            if (!target.exists()) {
                JOptionPane.showMessageDialog(null, "File not found.");
                return;
            }

            String newName = JOptionPane.showInputDialog("Enter new name:");
            if (name == null || name.trim().isEmpty()) return;

            File renamed = new File(currentFolder, newName);
            if (target.renameTo(renamed)) {
                displayFolderContents(currentFolder);
            } else {
                JOptionPane.showMessageDialog(null, "Rename failed.");
            }
        });

        deleteItem.addActionListener(e -> {
            if (currentFolder == null) return;

            String name = JOptionPane.showInputDialog("Enter name of file/folder to delete:");
            if (name == null || name.trim().isEmpty()) return;

            File target = new File(currentFolder, name);
            if (!target.exists()) {
                JOptionPane.showMessageDialog(null, "File not found.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(null, "Delete " + name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (target.isDirectory()) {
                    deleteDirectory(target);
                } else {
                    target.delete();
                }
                displayFolderContents(currentFolder);
            }
        });
        
        copyItem.addActionListener(e -> {
            if (currentFolder == null) return;

            String name = JOptionPane.showInputDialog("Enter name of file/folder to copy:");
            if (name == null || name.trim().isEmpty()) return;

            File target = new File(currentFolder, name);
            if (!target.exists()) {
                JOptionPane.showMessageDialog(null, "File not found.");
                return;
            }

            copiedFile = target;
            JOptionPane.showMessageDialog(null, "Copied: " + copiedFile.getName());
        });
        
        pasteItem.addActionListener(e -> {
            if (currentFolder == null || copiedFile == null) return;

            File dest = new File(currentFolder, copiedFile.getName());
            if (dest.exists()) {
                JOptionPane.showMessageDialog(null, "File already exists in this folder.");
                return;
            }

            try {
                if (copiedFile.isDirectory()) {
                    copyDirectory(copiedFile, dest);
                } else {
                    java.nio.file.Files.copy(copiedFile.toPath(), dest.toPath());
                }
                displayFolderContents(currentFolder);
                JOptionPane.showMessageDialog(null, "Pasted: " + copiedFile.getName());
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Paste failed.");
            }
        });
        
        searchFolderItem.addActionListener(e -> {
            String folderName = JOptionPane.showInputDialog(null, "Enter folder name to search:");

            if (folderName != null && !folderName.trim().isEmpty()) {
                File result = searchFolder(currentDirectory, folderName.trim()); 

                if (result != null) {
                    JOptionPane.showMessageDialog(null, "Folder found: " + result.getAbsolutePath());

                    folderPanel.removeAll();

                    Icon folderIcon = FileSystemView.getFileSystemView().getSystemIcon(result);

                    JButton folderButton = new JButton(result.getName(), folderIcon);
                    folderButton.setHorizontalTextPosition(SwingConstants.CENTER);
                    folderButton.setVerticalTextPosition(SwingConstants.BOTTOM);

                    folderButton.addActionListener(ev -> displayFolderContents(result));

                    folderPanel.add(folderButton);

                    folderPanel.revalidate();
                    folderPanel.repaint();
                } else {
                    JOptionPane.showMessageDialog(null, "Folder not found.");
                }
            }
        });
        
        newFolderItem.addActionListener(e -> {
            if (currentFolder == null || !currentFolder.isDirectory()) {
                JOptionPane.showMessageDialog(null, "No valid folder selected.");
                return;
            }

            String folderName = JOptionPane.showInputDialog(null, "Enter name for new folder:");

            if (folderName != null && !folderName.trim().isEmpty()) {
                File newFolder = new File(currentFolder, folderName.trim());

                if (newFolder.exists()) {
                    JOptionPane.showMessageDialog(null, "A folder with that name already exists.");
                } else {
                    boolean created = newFolder.mkdir();

                    if (created) {
                        JOptionPane.showMessageDialog(null, "Folder created successfully.");
                        displayFolderContents(currentFolder);
                    } else {
                        JOptionPane.showMessageDialog(null, "Failed to create folder.");
                    }
                }
            }
        });
        
        newFileItem.addActionListener(e -> {
            if (currentFolder == null || !currentFolder.isDirectory()) {
                JOptionPane.showMessageDialog(null, "No valid folder selected.");
                return;
            }

            String fileName = JOptionPane.showInputDialog(null, "Enter name for new file (e.g., file.txt):");

            if (fileName != null && !fileName.trim().isEmpty()) {
                File newFile = new File(currentFolder, fileName.trim());

                if (newFile.exists()) {
                    JOptionPane.showMessageDialog(null, "A file with that name already exists.");
                } else {
                    try {
                        boolean created = newFile.createNewFile();

                        if (created) {
                            JOptionPane.showMessageDialog(null, "File created successfully.");
                            displayFolderContents(currentFolder);
                        } else {
                            JOptionPane.showMessageDialog(null, "Failed to create file.");
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "An error occurred: " + ex.getMessage());
                    }
                }
            }
        });

        aboutItem.addActionListener(e -> {
            String instructions = 
                "📂 File Manager Instructions:\n\n" +
                "• File > Import Folder: Select a folder from your device to browse.\n" +
                "• File > New Folder: Create a new folder inside the current directory.\n" +
                "• File > New File: Create a new file inside the current directory.\n" +
                "• File > Back: Go to the previous folder you opened.\n" +
                "• File > Exit: Close the application.\n\n" +
                "• Edit > Rename: Rename a selected file or folder.\n" +
                "• Edit > Delete: Delete a selected file or folder.\n" +
                "• Edit > Copy/Paste: Duplicate files or folders.\n\n" +
                "• Search > Search Folder: Find a folder by name and jump directly to it.\n\n" +
                "• Click a folder icon to open it and view its contents.\n" +
                "• Click a file to open it with the default app (e.g., VS Code, Photos).\n" +
                "• Imported folders are saved and reloaded when you reopen the app.\n\n" +
                "✅ Tip: Make sure your Java version supports file opening via Desktop.\n\n" +
                "This project I purposely made to access my project folders more excitingly 😎😏";

            JOptionPane.showMessageDialog(null, instructions, "About This App", JOptionPane.INFORMATION_MESSAGE);
        });
        
        fileMenu.add(newFileItem);
        fileMenu.add(newFolderItem);
        fileMenu.add(importItem);
        fileMenu.add(backItem);
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);

        editMenu.add(renameItem);
        editMenu.add(deleteItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(searchMenu);
        menuBar.add(helpMenu);
        
        frame.add(outerPanel, BorderLayout.CENTER);
        frame.setJMenuBar(menuBar);
        
        loadSavedFolders();

        frame.setVisible(true);
	}
	
	private static void displayFolderContents(File folder) {
	    currentFolder = folder;

	    if (folder == null || !folder.isDirectory()) return;

	    folderPathLabel.setText("Current Folder: " + folder.getAbsolutePath());

	    if (folderHistory.isEmpty() || !folderHistory.peek().equals(folder)) {
	        folderHistory.push(folder);
	    }

	    folderPanel.removeAll();

	    File[] files = folder.listFiles();
	    if (files != null) {
	        for (File f : files) {
	            Icon icon = FileSystemView.getFileSystemView().getSystemIcon(f);

	            JButton btn = new JButton(f.getName(), icon);
	            btn.setVerticalTextPosition(SwingConstants.BOTTOM);
	            btn.setHorizontalTextPosition(SwingConstants.CENTER);
	            btn.setPreferredSize(new Dimension(100, 80));

	            if (f.isDirectory()) {
	                btn.addActionListener(e -> displayFolderContents(f));
	            } else {
	            	btn.addActionListener(e -> {
	            	    try {
	            	        Desktop.getDesktop().open(f);
	            	    } catch (IOException ex) {
	            	        ex.printStackTrace();
	            	        JOptionPane.showMessageDialog(null, "Cannot open file: " + f.getName());
	            	    }
	            	});
	            }

	            folderPanel.add(btn);
	        }
	    }

	    folderPanel.revalidate();
	    folderPanel.repaint();
	}

    
    private static void saveFolderPath(String folderPath) {
        try (FileWriter writer = new FileWriter("saved_folders.txt", true)) {
            writer.write(folderPath + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
    
    private static void copyDirectory(File source, File dest) throws IOException {
        if (!dest.exists()) dest.mkdir();
        for (String f : source.list()) {
            File srcFile = new File(source, f);
            File destFile = new File(dest, f);
            if (srcFile.isDirectory()) {
                copyDirectory(srcFile, destFile);
            } else {
                java.nio.file.Files.copy(srcFile.toPath(), destFile.toPath());
            }
        }
    }
    
    public static File searchFolder(File directory, String targetFolderName) {
        if (directory == null || !directory.isDirectory()) return null;

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (file.getName().equalsIgnoreCase(targetFolderName)) {
                        return file;
                    } else {
                        File result = searchFolder(file, targetFolderName);
                        if (result != null) return result;
                    }
                }
            }
        }
        return null;
    }
    
    private static void loadSavedFolders() {
        File file = new File("saved_folders.txt");
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String path;
            while ((path = reader.readLine()) != null) {
                File folder = new File(path);
                if (folder.exists() && folder.isDirectory()) {
                    displayFolderContents(folder);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }   
}
