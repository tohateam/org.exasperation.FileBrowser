package org.exasperation.FileManager;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.Runtime;
import java.net.URLConnection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.text.SimpleDateFormat;

import android.app.ProgressDialog;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem; 
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.os.Bundle;

public class FileBrowser extends Activity implements ListView.OnItemClickListener, AbsListView.MultiChoiceModeListener
{
    public static final String TAG = "org.exasperation.FileManager";
    public static final String ROOT_DIR = "/";
    public static final String DATE_FORMAT = "MMM d, yyyy";
    public static final String TYPE_PLAINTEXT = "text/plain";
    
    public enum ClipType { EMPTY, COPY, CUT };

    ListView lv = null;
    String homeDirectory = "/sdcard/";
    Context c = this;
    File currentDirectory = null;
    List<File> selectedEntries = new ArrayList<File>();
    List<File> directoryEntries = new ArrayList<File>();
    List<File> clipboardEntries = new ArrayList<File>();
    ClipType clipboardType = ClipType.EMPTY;
    ActionBar topMenu = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        topMenu = getActionBar();
        lv = (ListView) findViewById(R.id.file_list);

        if (savedInstanceState == null)
        {
            currentDirectory = new File(homeDirectory);
        }
        else
        {
            currentDirectory = new File(savedInstanceState.getString("savedPath"));
            int savedPosition = savedInstanceState.getInt("savedPosition");
            int savedListTop = savedInstanceState.getInt("savedListTop");
            if (savedPosition >= 0)
                lv.setSelectionFromTop(savedPosition, savedListTop);
        }

    }

    @Override
    public void onStart()
    {
        Log.d(TAG, "onStart()");

        super.onStart();
        lv.setOnItemClickListener(this);
        lv.setMultiChoiceModeListener(this);
        browseTo(currentDirectory);
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "onResume()");

        super.onResume();
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "onPause()");

        super.onPause();
    }

    @Override
    public void onStop()
    {
        Log.d(TAG, "onStop()");

        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy()");

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        Log.d(TAG, "onSaveInstanceState()");

        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putString("savedPath", currentDirectory.getAbsolutePath());
        int savedPosition = -1;
        savedPosition = lv.getFirstVisiblePosition();
        savedInstanceState.putInt("savedPosition", savedPosition);
        View firstVisibleView = lv.getChildAt(0);
        savedInstanceState.putInt("savedListTop", (firstVisibleView == null) ? 0 : firstVisibleView.getTop());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        Log.d(TAG, "onRestoreInstanceState()");

        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        Log.d(TAG, "onPrepareOptionsMenu()" + clipboardType);
        MenuInflater inflater = getMenuInflater();
        menu.clear();
        if (clipboardType != ClipType.EMPTY)
            inflater.inflate(R.menu.paste_menu, menu);
        inflater.inflate(R.menu.general_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected()");
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go up
                if (currentDirectory.getParentFile() != null)
                {
                    browseTo(currentDirectory.getParentFile());
                }
                return true;
            case R.id.menu_paste:
                paste(currentDirectory);
                invalidateOptionsMenu();
                return true;
            case R.id.menu_select_all:
                selectAll();
                return true; 
            case R.id.menu_new_file:
                newFile();
                return true;
            case R.id.menu_new_directory:
                newDirectory();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView list, View view, int position, long id)
    {
        Log.d(TAG, "onItemClick()");
        String newPath = null;
        newPath = currentDirectory.getAbsolutePath() + File.separator + directoryEntries.get(position).getName();
        browseTo(new File(newPath));
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        Log.d(TAG, "onCreateActionMode()");
        selectedEntries.clear();
        // Inflate the menu for the CAB
        return true;
    }
    
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Log.d(TAG, "onPrepareActionMode()");
        // Here you can perform updates to the CAB due to
        // an invalidate() request
        menu.clear();

        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.general_selected_menu, menu);
        if (selectedEntries.size() < 2)
            inflater.inflate(R.menu.single_selected_menu, menu);

        return false;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position,
                                          long id, boolean checked) {
        Log.d(TAG, "onItemCheckedStateChanged()");
        // Here you can do something when items are selected/de-selected,
        // such as update the title in the 
        if (checked) {
            selectedEntries.add(directoryEntries.get(position));
        } else {
            selectedEntries.remove(selectedEntries.indexOf(directoryEntries.get(position)));
        }
        mode.invalidate();

    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        Log.d(TAG, "onActionItemClicked()");
        // Respond to clicks on the actions in the CAB

        switch (item.getItemId()) {
            case R.id.menu_select_all:
                selectAll();
                return true;
            case R.id.menu_share:
                share(selectedEntries.get(0));
                mode.finish();
                return true;
            case R.id.menu_rename:
                rename(selectedEntries.get(0));
                mode.finish();
                return true;
            case R.id.menu_delete:
                delete(selectedEntries);
                mode.finish();
                return true;
            case R.id.menu_copy:
                clipboardType = ClipType.COPY;
                clip(selectedEntries);
                invalidateOptionsMenu();
                mode.finish();
                return true;
            case R.id.menu_cut:
                clipboardType = ClipType.CUT;
                clip(selectedEntries);
                invalidateOptionsMenu();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        Log.d(TAG, "onDestroyActionMode()");

        selectedEntries.clear();
        // Here you can make any necessary updates to the activity when
        // the CAB is removed. By default, selected items are deselected/unchecked.
    }

    private void selectAll()
    {
        selectedEntries.clear();
        for (int i = 0; i < directoryEntries.size(); i++)
        {
            lv.setItemChecked(i, true);
        }
    }

    private void share(final File selected)
    {
        final File file = selected.getAbsoluteFile();
        Intent i = new Intent(android.content.Intent.ACTION_SEND);
        String type = URLConnection.guessContentTypeFromName(file.getName());
        if (type != null)
        {
            i.setType(type);
            Log.d(TAG, type);
        }
        else
        {
            i.setType("*/*");
            Log.d(TAG, "*/*");
        }
        i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        startActivity(Intent.createChooser(i, "Share"));
    }


    private void clip(final List<File> files)
    {
        for (File file : files)
        {
            clipboardEntries.add(file.getAbsoluteFile());
        }
    }

    private void paste(final File directory)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.paste_file))
               .setMessage(R.string.confirm_paste)
               .setCancelable(true)
               .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       if (clipboardType == ClipType.CUT)
                       {
                           for (File file : clipboardEntries)
                           {
                               file.renameTo(new File(currentDirectory, file.getName()));
                           }
                           clipboardEntries.clear();
                           clipboardType = ClipType.EMPTY;
                           fill();
                       }
                       else if (clipboardType == ClipType.COPY)
                       {
                           for (File file : clipboardEntries)
                           {
                               try{
                                   if (file.isDirectory())
                                       FileUtils.copyDirectoryToDirectory(file, directory);
                                   else
                                       FileUtils.copyFileToDirectory(file, directory);
                               }
                               catch (IOException e)
                               {}
                           }
                           fill();
                       }
                       else
                           dialog.cancel();
                           return;
                   }
               })
               .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                   }
               });
        builder.create().show();
    }

    private void newDirectory() {
        final EditText nameEditor = new EditText(getApplicationContext());
        nameEditor.setText(R.string.new_directory);
        nameEditor.selectAll();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.new_directory))
               .setView(nameEditor)
               .setCancelable(true)
               .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       final File file = new File(currentDirectory, nameEditor.getText().toString());
                       file.mkdir();
                       fill();
                   }
               })
               .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                   }
               });
        builder.create().show();
    }
    
    private void newFile() {
        final EditText nameEditor = new EditText(getApplicationContext());
        nameEditor.setText(R.string.default_filename);
        nameEditor.selectAll();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.new_file))
               .setView(nameEditor)
               .setCancelable(true)
               .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       final File file = new File(currentDirectory, nameEditor.getText().toString());
                       try {
                           file.createNewFile();
                       }
                       catch(Exception e) {}
                       fill();
                   }
               })
               .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                   }
               });
        builder.create().show();
    }

    private void rename(final File selected) {
        final File file = selected.getAbsoluteFile();
        final EditText nameEditor = new EditText(getApplicationContext());
        nameEditor.setText(file.getName());
        nameEditor.selectAll();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.rename_file))
               .setView(nameEditor)
               .setCancelable(true)
               .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       if (file.renameTo(new File(currentDirectory, nameEditor.getText().toString())))
                           Log.d(TAG, "renameSelection(): rename successful!");
                       fill();
                   }
               })
               .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                   }
               });
        builder.create().show();
    }

    private void delete(final List<File> selected)
    {
        Log.d(TAG, "delete()");
        final List<File> files = new ArrayList<File>();
        for (File file : selected)
            files.add(file);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.delete_file))
               .setMessage(getString(R.string.confirm_delete))
               .setCancelable(true)
               .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       new DeleteTask(c).execute(files.toArray(new File[1]));
                   }
               })
               .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                   }
               });
        builder.create().show();
    }

    private void browseTo(final File aDirectory)
    {
        Log.d(TAG, "browseTo()");
        if (aDirectory.isDirectory())
        {
            if (aDirectory.canRead() && aDirectory.canExecute())
            {
                currentDirectory = aDirectory;
                fill();
            }
            else
            {
                Toast t = Toast.makeText(c, "Cannot access directory", Toast.LENGTH_SHORT);
                t.show();
            }
        }
        else
        {
            String type = URLConnection.guessContentTypeFromName(aDirectory.getName());
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            if (type == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.unknown_alert_title))
                       .setMessage(getString(R.string.open_as_text))
                       .setCancelable(true)
                       .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               intent.setDataAndType(Uri.fromFile(aDirectory), TYPE_PLAINTEXT);
                               
                               startActivity(intent);
                           }
                        })
                       .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
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


	private void fill() {
        Log.d(TAG, "fill()");
        final File[] directories = currentDirectory.listFiles(new DirectoryFilter());
        final File[] files = currentDirectory.listFiles(new NoDirectoryFilter());
        Arrays.sort(directories);
        Arrays.sort(files);
		this.directoryEntries.clear();
	    for (File file : directories){
	        this.directoryEntries.add(file);
	    }
	    for (File file : files){
	        this.directoryEntries.add(file);
	    }

        lv.setAdapter(new FileAdapter(this, R.layout.file_row, this.directoryEntries));
        if (currentDirectory.getAbsolutePath() != ROOT_DIR)
        {
            topMenu.setTitle(currentDirectory.getName() + File.separator);
            topMenu.setHomeButtonEnabled(true);
        }
        else
        {
            topMenu.setTitle(ROOT_DIR);
            topMenu.setHomeButtonEnabled(false);
        }
        topMenu.setIcon(getResources().getDrawable(R.drawable.navigate_up));
        invalidateOptionsMenu();
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
    private class DeleteTask extends AsyncTask<File, Void, Boolean> {
        public static final String TAG = "org.exasperation.FileManager";
        ProgressDialog dialog;
        boolean success = true;
        Context c;
        public DeleteTask(Context context) {
            c = context;
        }

        protected void onPreExecute() {
            
            dialog = new ProgressDialog(c);
            Log.d(TAG,"setup complete");
            dialog.setTitle("Deleting files");
            dialog.setMessage("Please wait...");
            dialog.setIndeterminate(true);
            dialog.show();
            Log.d(TAG, "showing...");
            
        }
        protected Boolean doInBackground(File... files) {
            for (int i = 0; i < files.length; i++)
            {
                if (files[i].isDirectory())
                    try{
                        FileUtils.deleteDirectory(files[i]);
                    }
                    catch(Exception e)
                    {}
                else
                    files[i].delete();
            }

            return true; 
        }
        protected void onPostExecute(Boolean result) {
            dialog.dismiss();
            fill();
        }
    }
    private class DirectoryFilter implements FileFilter {
        public boolean accept (File file) {
            return file.isDirectory();
        }
    }
    private class NoDirectoryFilter implements FileFilter {
        public boolean accept (File file) {
            return !file.isDirectory();
        }
    }
}

