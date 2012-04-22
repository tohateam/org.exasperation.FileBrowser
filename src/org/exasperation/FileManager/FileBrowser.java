package org.exasperation.FileManager;

import java.io.File;
import java.net.URLConnection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.text.SimpleDateFormat;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem; 
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import android.app.ListActivity;
import android.os.Bundle;

public class FileBrowser extends ListActivity
{
    public static final String TAG = "org.exasperation.FileManager";
    public static final String DIR_DIVIDER = "/";
    public static final String ROOT_DIR = "/";
    public static final String DATE_FORMAT = "MMM d, yyyy";
    public static final String TYPE_PLAINTEXT = "text/plain";
    
    String homeDirectory = "/sdcard/";
    File currentDirectory = new File(homeDirectory);
    List<File> directoryEntries = new ArrayList<File>();
    ActionBar topMenu = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        topMenu = getActionBar();

        browseTo(currentDirectory);
        topMenu.setHomeButtonEnabled(true);
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
        else
        {
            String type = URLConnection.guessContentTypeFromName(aDirectory.getName());
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            if (type == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Unknown file type")
                       .setMessage("Open as plaintext file instead?")
                       .setCancelable(true)
                       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               intent.setDataAndType(Uri.fromFile(aDirectory),URLConnection.guessContentTypeFromName("hello.txt"));
                               
                               startActivity(intent);
                           }
                        })
                       .setNegativeButton("No", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               dialog.cancel();
                           }
                       });
                builder.create().show();
            }
            else {
                intent.setDataAndType(Uri.fromFile(aDirectory),type);
                Log.d(TAG, "opening as "+type);
                startActivity(intent);
            }
        }
    }

	private void fill(File[] files) {
		this.directoryEntries.clear();
	    for (File file : files){
	        this.directoryEntries.add(file);
	    }

        setListAdapter(new FileAdapter(this, R.layout.file_row, this.directoryEntries));
        if (currentDirectory.getAbsolutePath() != ROOT_DIR)
            topMenu.setTitle(currentDirectory.getName() + DIR_DIVIDER);
        else
            topMenu.setTitle(ROOT_DIR);
        topMenu.setIcon(getResources().getDrawable(R.drawable.navigate_up));
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
                ImageView fileIcon = (ImageView) v.findViewById(R.id.file_icon);
                
                Drawable icon = null;

                if (o.isDirectory()) {
                    icon = getResources().getDrawable(R.drawable.folder);
                }
                else {
                    String type = URLConnection.guessContentTypeFromName(o.getName());
                    Log.d (TAG, "type guessed as " + type);
                    if (type != null) {
                        final Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(o), type);

                        final ResolveInfo app = getPackageManager().resolveActivity(intent, 0);
   /* 
                        final List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    
                        if (matches.size() > 0)
                        {
                            //Only get first match, since we can't display a whole list of icons
                            //OR CAN WE????
                            //Yea we can't fuck you
                            icon = matches.get(0).loadIcon(getPackageManager());
                            for (ResolveInfo match : matches)
                                Log.d(TAG, ""+match.loadLabel(getPackageManager()));
                        }
*/
                        if (app != null)
                            icon = app.loadIcon(getPackageManager());
                        else 
                            icon = getResources().getDrawable(R.drawable.file);
                    }
                    else
                        icon = getResources().getDrawable(R.drawable.file);
                }

                fileIcon.setImageDrawable(icon);
                 
                
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
