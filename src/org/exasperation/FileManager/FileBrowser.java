package org.exasperation.FileManager;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import android.widget.ListView;
import android.view.View;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.content.DialogInterface;
import android.util.Log;

import android.app.ListActivity;
import android.os.Bundle;

public class FileBrowser extends ListActivity
{
    public static final String TAG = "org.exasperation.FileManager";
    public static final String DIR_DIVIDER = "/";
    public static final String ROOT_DIR = "/";
    
    String homeDirectory = "/sdcard/";
    File currentDirectory = new File(homeDirectory);
    List<String> directoryEntries = new ArrayList<String>();

   /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        browseTo(currentDirectory);
    }

    private void browseTo(final File aDirectory)
    {
        if (aDirectory.isDirectory())
        {
            Log.d (TAG, "DIRECTOREEEEEEE");
            currentDirectory = aDirectory;
            File[] fileList = currentDirectory.listFiles();
            Arrays.sort(fileList);
            fill(fileList);
        }
        /*
        else
        {
            OnClickListener okButtonListener = new OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    // Lets start an intent to View the file, that was clicked...
                    //openFile(aDirectory);
                }
            };
            OnClickListener cancelButtonListener = new OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    // Do nothing
                }
            };
            AlertDialog.show(this,"Question", "Do you want to open that file?n" + aDirectory.getName(),
                             "OK", okButtonListener,
                             "Cancel", cancelButtonListener, false, null);
        }*/
    }

    private void browseToRoot()
    {
        browseTo(new File(ROOT_DIR));
    }

	private void fill(File[] files) {
		this.directoryEntries.clear();
        Log.d (TAG, "currentDirectory.getAbsolutePath: "+ currentDirectory.getAbsolutePath());
        if (currentDirectory.getParentFile() != null )
            directoryEntries.add(getString(R.string.up_one_level));
        else
            Log.d (TAG, "NULLLY");
	    for (File file : files){
	        this.directoryEntries.add(file.getName());
	    }

        ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, this.directoryEntries);
        setListAdapter(directoryList);
        
    }

    public void onListItemClick(ListView list,View view, int position, long id)
    {
        Log.d (TAG, "pos:"+position+" id:"+id);
        String newPath = null;
        if (directoryEntries.get(position) == getString(R.string.up_one_level))
            newPath = currentDirectory.getParent();
        else
            newPath = currentDirectory.getAbsolutePath() + DIR_DIVIDER + directoryEntries.get(position);
        Log.d (TAG, newPath + " WHEEE"); 
        browseTo(new File(newPath));
    }
}
