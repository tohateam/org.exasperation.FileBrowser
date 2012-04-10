package org.exasperatio
n.FileManager;
import android.view.MenuItem; 

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import android.widget.TextView;
import android.content.Context;
import android.view.ViewGroup;
import android.view.LayoutInflater;
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
    public static final String DATE_FORMAT = "MMM d, yyyy";
    
    String homeDirectory = "/sdcard/";
    File currentDirectory = new File(homeDirectory);
    List<File> directoryEntries = new ArrayList<File>();

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
	    for (File file : files){
	        this.directoryEntries.add(file);
	    }

        setListAdapter(new FileAdapter(this, R.layout.file_row, this.directoryEntries));
        
    }

    public void onListItemClick(ListView list, View view, int position, long id)
    {
        String newPath = null;
        newPath = currentDirectory.getAbsolutePath() + DIR_DIVIDER + directoryEntries.get(position).getName();
        browseTo(new File(newPath));
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go up
                if (currentDirectory.getParentFile() != null)
                {
                    browseTo(currentDirectory.getParentFile());
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private class FileAdapter extends ArrayAdapter<File> {
        private List<File> files;
        public FileAdapter(Context context, int textViewResourceId, List<File> files) {
            super(context, textViewResourceId, files);
            this.files = files;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.file_row, null);
            }
            File o = files.get(position);
            if (o != null) {
                TextView fileName = (TextView) v.findViewById(R.id.file_name);
                TextView fileMeta = (TextView) v.findViewById(R.id.file_meta);
                if (fileName != null) {
                    fileName.setText(o.getName());
                }
                if( fileMeta != null){
                    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
                    fileMeta.setText(format.format(new Date(o.lastModified())));
                }
            }
            return v;
        }
    }
}
